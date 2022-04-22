package com.elorankingbot.backend.service;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditMono;
import discord4j.core.spec.TextChannelCreateMono;
import discord4j.core.spec.TextChannelEditMono;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.elorankingbot.backend.timedtask.TimedTask.TimedTaskType.CHANNEL_DELETE;

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;
	private final DBService dbService;
	private final TimedTaskQueue timedTaskQueue;
	private final ApplicationService applicationService;
	private final ApplicationPropertiesLoader props;
	private PrivateChannel ownerPrivateChannel;
	private final long botId;
	@Getter
	private String latestCommandLog;

	private static int embedRankSpaces = 6;
	private static int embedRatingSpaces = 8;
	private static int embedWinsSpaces = 5;// TODO abhaengig von den daten
	private static String embedName = "`   Rank  Rating   Wins Losses  Name`";
	private static String embedNameWithDraws = "`   Rank  Rating    Wins Losses Draws Name`";

	public DiscordBotService(Services services) {
		this.client = services.client;
		this.dbService = services.dbService;
		this.timedTaskQueue = services.timedTaskQueue;
		this.botId = client.getSelfId().asLong();
		this.props = services.props;
		this.applicationService = client.getRestClient().getApplicationService();
	}

	@PostConstruct
	public void initAdminDm() {
		long ownerId = Long.valueOf(props.getOwnerId());
		User owner = client.getUserById(Snowflake.of(ownerId)).block();
		this.ownerPrivateChannel = owner.getPrivateChannel().block();
		sendToOwner("I am logged in and ready");
	}

	public void sendToOwner(String text) {
		if (text == null) text = "null";
		if (text.equals("")) text = "empty String";
		ownerPrivateChannel.createMessage(text).subscribe();
	}

	public void logCommand(Object command) {
		latestCommandLog = command.getClass().getSimpleName() + "::execute";
		log.debug(latestCommandLog);
	}

	public Mono<PrivateChannel> getPrivateChannelByUserId(long userId) {
		return client.getUserById(Snowflake.of(userId)).flatMap(User::getPrivateChannel);
	}

	public Mono<Channel> getChannelById(long channelId) {
		return client.getChannelById(Snowflake.of(channelId));
	}

	public Mono<Message> sendToUser(long userId, String text) {
		return client.getUserById(Snowflake.of(userId)).block()
				.getPrivateChannel().block()
				.createMessage(text);
	}

	public Mono<Message> sendToUser(long userId, MessageCreateSpec spec) {
		return client.getUserById(Snowflake.of(userId)).block()
				.getPrivateChannel().block()
				.createMessage(spec);
	}

	public Mono<User> getUser(long userId) {
		return client.getUserById(Snowflake.of(userId));
	}

	public Mono<Message> getMessage(long messageId, long channelId) {
		return client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId));
	}

	public Mono<Message> getMatchMessage(Match match) {
		return getMessage(match.getMessageId(), match.getChannelId());
	}

	public Mono<Message> getPlayerMessage(Player player, Match match) {
		return getMessage(match.getMessageId(player.getId()), match.getPrivateChannelId(player.getId()));
	}

	public Mono<Guild> getGuildById(long guildId) {
		return client.getGuildById(Snowflake.of(guildId));
	}

	// Channels
	public void postToResultChannel(MatchResult matchResult) {
		Game game = matchResult.getGame();
		TextChannel resultChannel = getOrCreateResultChannel(game);
		resultChannel.createMessage(EmbedBuilder.createMatchResultEmbed(matchResult)).subscribe();
	}

	public TextChannel getOrCreateResultChannel(Game game) {
		try {
			return (TextChannel) client.getChannelById(Snowflake.of(game.getResultChannelId())).block();
		} catch (ClientException e) {
			Guild guild = getGuildById(game.getGuildId()).block();
			TextChannel resultChannel = guild.createTextChannel(String.format("%s match results", game.getName()))
					.withPermissionOverwrites(PermissionOverwrite.forRole(
							Snowflake.of(guild.getId().asLong()),
							PermissionSet.none(),
							PermissionSet.of(Permission.SEND_MESSAGES)))
					.block();
			game.setResultChannelId(resultChannel.getId().asLong());
			dbService.saveServer(game.getServer());
			return resultChannel;
		}
	}

	public MessageEditMono refreshLeaderboard(Game game) {
		Message leaderboardMessage;
		try {
			leaderboardMessage = getMessage(game.getLeaderboardMessageId(), game.getLeaderboardChannelId()).block();
		} catch (ClientException e) {
			leaderboardMessage = createLeaderboardChannelAndMessage(game);
			dbService.saveServer(game.getServer());// TODO muss das?
		}
		return leaderboardMessage.edit()
				.withContent(Possible.of(Optional.empty()))
				.withEmbeds(EmbedBuilder.createRankingsEmbed(dbService.getLeaderboard(game)));
	}

	private Message createLeaderboardChannelAndMessage(Game game) {
		Guild guild = getGuildById(game.getGuildId()).block();
		TextChannel leaderboardChannel = guild.createTextChannel(String.format("%s Leaderboard", game.getName()))
				.withPermissionOverwrites(PermissionOverwrite.forRole(
						Snowflake.of(guild.getId().asLong()),
						PermissionSet.none(),
						PermissionSet.of(Permission.SEND_MESSAGES)))
				.block();
		Message leaderboardMessage = leaderboardChannel.createMessage("creating leaderboard...").block();
		game.setLeaderboardMessageId(leaderboardMessage.getId().asLong());
		game.setLeaderboardChannelId(leaderboardChannel.getId().asLong());
		return leaderboardMessage;
	}

	public Category getOrCreateMatchCategory(Server server) {
		try {
			return (Category) getChannelById(server.getMatchCategoryId()).block();
		} catch (ClientException e) {
			if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")) {
				throw e;
			}
			Guild guild = getGuildById(server.getGuildId()).block();
			Category matchCategory = guild.createCategory("elo matches")
					.withPermissionOverwrites(denyEveryoneView(server)).block();
			server.setMatchCategoryId(matchCategory.getId().asLong());
			dbService.saveServer(server);
			return matchCategory;
		}
	}

	public Category getOrCreateDisputeCategory(Server server) {// TODO evtl Optional<Guild> mit als parameter, um den request zu sparen?
		try {
			return (Category) getChannelById(server.getDisputeCategoryId()).block();
		} catch (ClientException e) {
			if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")) {
				throw e;
			}
			Guild guild = getGuildById(server.getGuildId()).block();
			Category disputeCategory = guild.createCategory("elo disputes").withPermissionOverwrites(
					denyEveryoneView(server),
					allowAdminView(server),
					allowModView(server)).block();
			server.setDisputeCategoryId(disputeCategory.getId().asLong());
			dbService.saveServer(server);
			return disputeCategory;
		}
	}

	public Category getOrCreateArchiveCategory(Server server) {
		try {
			return (Category) getChannelById(server.getArchiveCategoryId()).block();
		} catch (ClientException e) {
			if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")) {
				throw e;
			}
			Guild guild = getGuildById(server.getGuildId()).block();
			Category archiveCategory = guild.createCategory("elo archive").withPermissionOverwrites(
					denyEveryoneView(server),
					allowAdminView(server),
					allowModView(server)).block();
			server.setArchiveCategoryId(archiveCategory.getId().asLong());
			dbService.saveServer(server);
			return archiveCategory;
		}
	}

	public void moveToArchive(Server server, Channel channel) {
		setParentCategory(channel, server.getArchiveCategoryId())
				.onErrorResume(error -> {// TODO error filtern
					Category newArchiveCategory = getOrCreateArchiveCategory(server);
					server.setArchiveCategoryId(newArchiveCategory.getId().asLong());
					return setParentCategory(channel, newArchiveCategory.getId().asLong());
				}).subscribe();
		timedTaskQueue.addTimedTask(CHANNEL_DELETE, 24 * 60, channel.getId().asLong(), 0L, null);
		((TextChannel) channel).createMessage("**I have moved this channel to the archive. " +
				"I will delete this channel in 24h.**").subscribe();
	}

	public TextChannelCreateMono createDisputeChannel(Match match) {
		Server server = match.getServer();
		List<PermissionOverwrite> permissionOverwrites = new ArrayList<>(match.getNumPlayers());
		match.getPlayers().forEach(player -> permissionOverwrites.add(allowPlayerView(player)));
		Category disputeCategory = getOrCreateDisputeCategory(server);
		permissionOverwrites.addAll(disputeCategory.getPermissionOverwrites());
		return client.getGuildById(Snowflake.of(match.getGame().getGuildId())).block()
				.createTextChannel(createMatchChannelName(match.getTeams()))
				.withParentId(Snowflake.of(server.getDisputeCategoryId()))
				.withPermissionOverwrites(permissionOverwrites);
	}

	public TextChannelCreateMono createMatchChannel(Match match) {
		Server server = match.getServer();
		List<PermissionOverwrite> permissionOverwrites = new ArrayList<>();
		permissionOverwrites.add(denyEveryoneView(server));
		match.getPlayers().forEach(player -> permissionOverwrites.add(allowPlayerView(player)));
		String channelName = createMatchChannelName(match.getTeams());
		getOrCreateMatchCategory(server);
		return client.getGuildById(Snowflake.of(match.getGame().getGuildId())).block()
				.createTextChannel(channelName)
				.withParentId(Snowflake.of(server.getMatchCategoryId()))
				.withPermissionOverwrites(permissionOverwrites);
	}

	private String createMatchChannelName(List<List<Player>> teams) {
		String tentativeString = String.join("-vs-", teams.stream()
				.map(team -> String.join("-", team.stream().map(Player::getTag).toList())).toList());
		if (tentativeString.length() <= 100) {
			return tentativeString;
		} else {
			return tentativeString.substring(0, 100);
		}
	}

	public TextChannelEditMono setParentCategory(Channel channel, long categoryId) {
		return ((TextChannel) channel).edit().withParentId(Possible.of(Optional.of(Snowflake.of(categoryId))));
	}

	// Commands
	// TODO direkt schauen ob in dbService.adminCommands und entsprechend permissions setzen?
	// schauen wie es ist mit CommandParser, on(GuildCreateEvent.class)
	public Mono<ApplicationCommandData> deployCommand(Server server, ApplicationCommandRequest request) {
		return applicationService.createGuildApplicationCommand(botId, server.getGuildId(), request)
				.doOnNext(commandData -> log.debug(String.format("deployed command %s:%s to %s",
						commandData.name(), commandData.id(), server.getGuildId())))
				.doOnError(ClientException.class, e ->
						log.error("Error deploying command:\n" + e.getRequest().toString()
								.replace(", ", ",\n")));
	}

	public Mono<Void> deleteCommand(Server server, String commandName) {
		log.debug("deleting command " + commandName);
		return getCommandIdByName(server.getGuildId(), commandName)
				.flatMap(commandId -> applicationService
						.deleteGuildApplicationCommand(botId, server.getGuildId(), commandId));
	}

	public Flux<Object> deleteAllGuildCommands(long guildId) {
		return applicationService.getGuildApplicationCommands(botId, guildId)
				.doOnNext(commandData -> log.trace(String.format("deleting command %s:%s on %s",
						commandData.name(), commandData.id(), guildId)))
				.flatMap(commandData -> applicationService.deleteGuildApplicationCommand(
						botId, guildId, Long.parseLong(commandData.id())));
	}

	// Permissions
	public void setCommandPermissionForRole(Server server, String commandName, long roleId) {
		client.getRoleById(Snowflake.of(server.getGuildId()), Snowflake.of(roleId))
				.subscribe(role -> {
					long commandId = getCommandIdByName(server.getGuildId(), commandName).defaultIfEmpty(0L).block();
					if (commandId == 0L) return;

					log.debug(String.format("setting permissions for command %s to role %s", commandName, role.getName()));
					var request = ApplicationCommandPermissionsRequest.builder()
							.addPermission(ApplicationCommandPermissionsData.builder()
									.id(role.getId().asLong()).type(1).permission(true).build()).build();
					applicationService
							.modifyApplicationCommandPermissions(botId, server.getGuildId(), commandId, request).block();
				});
	}

	public void setPermissionsForModCommand(Server server, String commandName) {
		// both admin and mod permissions need to be set
		if (server.getAdminRoleId() != 0L) {
			setCommandPermissionForRole(server, commandName, server.getAdminRoleId());
		}
		if (server.getModRoleId() != 0L) {
			setCommandPermissionForRole(server, commandName, server.getModRoleId());
		}
	}

	public void setPermissionsForAdminCommand(Server server, String commandName) {
		// only admin permissions need to be set
		if (server.getAdminRoleId() != 0L) {
			setCommandPermissionForRole(server, commandName, server.getAdminRoleId());
		}
	}

	public void setOwnerPermissionToCommand(Server server, String commandName) {
		var request = ApplicationCommandPermissionsRequest.builder()
				.addPermission(ApplicationCommandPermissionsData.builder()
						.id(props.getOwnerId()).type(2).permission(true).build()).build();
		getCommandIdByName(server.getGuildId(), commandName).subscribe(commandId -> applicationService
				.modifyApplicationCommandPermissions(botId, server.getGuildId(), commandId, request).subscribe());
	}

	public static PermissionOverwrite allowAdminView(Server server) {
		return PermissionOverwrite.forRole(Snowflake.of(server.getAdminRoleId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}

	public static PermissionOverwrite allowModView(Server server) {
		return PermissionOverwrite.forRole(Snowflake.of(server.getModRoleId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}

	public static PermissionOverwrite denyEveryoneView(Server server) {
		return PermissionOverwrite.forRole(Snowflake.of(server.getGuildId()),
				PermissionSet.none(),
				PermissionSet.of(Permission.VIEW_CHANNEL));
	}

	public static PermissionOverwrite allowPlayerView(Player player) {
		return PermissionOverwrite.forMember(Snowflake.of(player.getUserId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}

	// misc
	private Mono<Long> getCommandIdByName(long guildid, String commandName) {
		return applicationService.getGuildApplicationCommands(botId, guildid)
				.filter(applicationCommandData -> applicationCommandData.name().equals(commandName.toLowerCase()))
				.next()
				.map(commandData -> Long.parseLong(commandData.id()));
	}

	public static boolean isLegalDiscordName(String string) {
		Pattern p = Pattern.compile("[\\w-]{1,32}");
		Matcher m = p.matcher(string);
		if (m.find()) return true;
		else return false;
	}
}
