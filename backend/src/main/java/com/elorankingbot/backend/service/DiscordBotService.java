package com.elorankingbot.backend.service;

import com.elorankingbot.backend.commands.admin.*;
import com.elorankingbot.backend.commands.admin.deleteranking.DeleteRanking;
import com.elorankingbot.backend.commands.mod.ForceDraw;
import com.elorankingbot.backend.commands.mod.ForceWin;
import com.elorankingbot.backend.commands.mod.RevertMatch;
import com.elorankingbot.backend.commands.player.Join;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.google.common.collect.Iterables;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
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
import java.util.*;
import java.util.stream.Collectors;

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

	// Logging
	public void sendToOwner(String text) {
		if (text == null) text = "null";
		if (text.equals("")) text = "empty String";
		ownerPrivateChannel.createMessage(text).subscribe();
	}

	public void logCommand(Object command) {
		// this is being used in EventParser::handleException
		latestCommandLog = command.getClass().getSimpleName() + "::execute";
		// TODO logging ueberarbeiten
		// log.debug(latestCommandLog);
	}

	// Server
	public String getServerName(Server server) {
		try {
			return client.getGuildById(Snowflake.of(server.getGuildId())).block().getName();
		} catch (ClientException e) {
			return "unknown";
		}
	}

	public String getServerName(Player player) {
		return client.getGuildById(Snowflake.of(player.getGuildId())).block().getName();
	}

	// Guild
	public Mono<Guild> getGuildById(long guildId) {
		return client.getGuildById(Snowflake.of(guildId));
	}

	public List<Long> getAllGuildIds() {
		return client.getGuilds()
				.map(guild -> guild.getId().asLong())
				.collectList().block();
	}

	// Player, Ranks
	public Mono<User> getUser(long userId) {
		return client.getUserById(Snowflake.of(userId));
	}

	public void updatePlayerRank(Game game, Player player) {
		List<Integer> applicableRequiredRatings = new ArrayList<>(game.getRequiredRatingToRankId().keySet().stream()
				.filter(requiredRating -> player.findGameStats(game).isPresent()
						&& player.findGameStats(game).get().getRating() > requiredRating)
				.toList());
		if (applicableRequiredRatings.size() == 0) return;

		Collections.sort(applicableRequiredRatings);
		int relevantRequiredRating = Iterables.getLast(applicableRequiredRatings);
		Snowflake rankSnowflake = Snowflake.of(game.getRequiredRatingToRankId().get(relevantRequiredRating));

		Member member;
		try {
			member = client.getMemberById(Snowflake.of(player.getGuildId()), Snowflake.of(player.getUserId())).block();
		} catch (ClientException e) {
			return;// TODO player loeschen etc
		}
		Set<Snowflake> currentRankSnowflakes = member.getRoleIds().stream()
				.filter(snowflake -> game.getRequiredRatingToRankId().containsValue(snowflake.asLong()))
				.collect(Collectors.toSet());
		if (!currentRankSnowflakes.contains(rankSnowflake)) {
			member.addRole(rankSnowflake).subscribe();
		}
		currentRankSnowflakes.stream().filter(roleSnowflake -> !roleSnowflake.equals(rankSnowflake))
				.forEach(roleIdSnowflake -> member.removeRole(roleIdSnowflake).subscribe());
	}

	public void removeAllRanks(Game game) {
		Collection<Long> allRankIds = game.getRequiredRatingToRankId().values();
		for (Player player : dbService.findAllPlayersForServer(game.getServer())) {
			client.getMemberById(Snowflake.of(player.getGuildId()), Snowflake.of(player.getUserId()))
					.onErrorContinue((throwable, o) -> {
					})// TODO wahrscheinlich den player loeschen
					.subscribe(member -> member.getRoleIds().stream()
							.filter(snowflake -> allRankIds.contains(snowflake.asLong()))
							.forEach(snowflake -> member.removeRole(snowflake).subscribe()));
		}
	}

	// Messages
	public Mono<Message> getMessage(long messageId, long channelId) {
		return client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId));
	}

	public Mono<Message> getMatchMessage(Match match) {
		return getMessage(match.getMessageId(), match.getChannelId());
	}

	// Channels
	public Mono<PrivateChannel> getPrivateChannelByUserId(long userId) {
		return client.getUserById(Snowflake.of(userId)).flatMap(User::getPrivateChannel);
	}

	public Mono<Channel> getChannelById(long channelId) {
		return client.getChannelById(Snowflake.of(channelId));
	}

	public void deleteChannel(long channelId) {
		client.getChannelById(Snowflake.of(channelId))
				.onErrorContinue(((throwable, o) -> log.info(String.format("Channel %s is already deleted.", channelId))))
				.subscribe(channel -> channel.delete().subscribe());
	}

	public Message postToResultChannel(MatchResult matchResult) {
		Game game = matchResult.getGame();
		TextChannel resultChannel = getOrCreateResultChannel(game);
		System.out.println(resultChannel.getName());
		return resultChannel.createMessage(EmbedBuilder.createMatchResultEmbed(matchResult)).block();
	}

	public TextChannel getOrCreateResultChannel(Game game) {
		try {
			return (TextChannel) client.getChannelById(Snowflake.of(game.getResultChannelId())).block();
		} catch (ClientException e) {// TODO funktioniert das ueberhaupt? testen!
			Guild guild = getGuildById(game.getGuildId()).block();
			TextChannel resultChannel = guild.createTextChannel(String.format("%s match results", game.getName()))
					.withPermissionOverwrites(onlyBotCanSend(game.getServer()))
					.block();
			game.setResultChannelId(resultChannel.getId().asLong());
			dbService.saveServer(game.getServer());
			return resultChannel;
		}
	}

	private List<PermissionOverwrite> onlyBotCanSend(Server server) {
		return List.of(PermissionOverwrite.forRole(
						Snowflake.of(server.getEveryoneId()),
						PermissionSet.none(),
						PermissionSet.of(Permission.SEND_MESSAGES)),
				PermissionOverwrite.forMember(
						client.getSelfId(),
						PermissionSet.of(Permission.SEND_MESSAGES),
						PermissionSet.none()));
	}

	public TextChannelCreateMono createDisputeChannel(Match match) {
		Server server = match.getServer();
		List<PermissionOverwrite> permissionOverwrites = new ArrayList<>(match.getNumPlayers());
		match.getPlayers().forEach(player -> permissionOverwrites.add(allowPlayerView(player)));
		Category disputeCategory = getOrCreateDisputeCategory(server);
		permissionOverwrites.addAll(disputeCategory.getPermissionOverwrites());
		return client.getGuildById(Snowflake.of(match.getGame().getGuildId())).block()
				.createTextChannel(createMatchChannelName(match.getTeams()))
				.withParentId(disputeCategory.getId())
				.withPermissionOverwrites(permissionOverwrites);
	}

	public TextChannelCreateMono createMatchChannel(Match match) {
		Server server = match.getServer();
		List<PermissionOverwrite> permissionOverwrites = new ArrayList<>();
		permissionOverwrites.add(denyEveryoneView(server));
		match.getPlayers().forEach(player -> permissionOverwrites.add(allowPlayerView(player)));
		permissionOverwrites.add(allowModView(server));
		permissionOverwrites.add(allowAdminView(server));
		permissionOverwrites.add(allowBotView());

		String channelName = createMatchChannelName(match.getTeams());
		Category matchCategory = getOrCreateMatchCategory(server);
		return client.getGuildById(Snowflake.of(match.getGame().getGuildId())).block()
				.createTextChannel(channelName)
				.withParentId(matchCategory.getId())
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

	// Leaderboard
	public void updateLeaderboard(Game game, Optional<MatchResult> maybeMatchResult) {
		boolean leaderboardNeedsRefresh;
		if (maybeMatchResult.isPresent()) {
			leaderboardNeedsRefresh = dbService.updateRankingsEntries(maybeMatchResult.get());
		} else {
			leaderboardNeedsRefresh = true;
		}
		if (leaderboardNeedsRefresh) {
			Message leaderboardMessage;
			try {
				leaderboardMessage = getMessage(game.getLeaderboardMessageId(), game.getLeaderboardChannelId()).block();
			} catch (ClientException e) {
				leaderboardMessage = createLeaderboardChannelAndMessage(game);
				dbService.saveServer(game.getServer());// TODO muss das?
			}
			leaderboardMessage.edit()
					.withContent(Possible.of(Optional.empty()))
					.withEmbeds(EmbedBuilder.createRankingsEmbed(dbService.getLeaderboard(game))).subscribe();
		}
	}

	private Message createLeaderboardChannelAndMessage(Game game) {
		Guild guild = getGuildById(game.getGuildId()).block();
		TextChannel leaderboardChannel = guild.createTextChannel(String.format("%s Leaderboard", game.getName()))
				.withPermissionOverwrites(onlyBotCanSend(game.getServer()))
				.block();
		Message leaderboardMessage = leaderboardChannel.createMessage("creating leaderboard...").block();
		game.setLeaderboardMessageId(leaderboardMessage.getId().asLong());
		game.setLeaderboardChannelId(leaderboardChannel.getId().asLong());
		return leaderboardMessage;
	}

	// Categories
	public Category getOrCreateMatchCategory(Server server) {
		try {
			return (Category) getChannelById(server.getMatchCategoryId()).block();
		} catch (ClientException e) {
			if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")
					&& !e.getErrorResponse().get().toString().contains("CHANNEL_PARENT_INVALID")) {
				throw e;
			}
			Guild guild = getGuildById(server.getGuildId()).block();
			Category matchCategory = guild.createCategory("elo matches")
					.withPermissionOverwrites(denyEveryoneView(server), allowAdminView(server), allowModView(server), allowBotView())
					.block();
			server.setMatchCategoryId(matchCategory.getId().asLong());
			dbService.saveServer(server);
			return matchCategory;
		}
	}

	public Category getOrCreateDisputeCategory(Server server) {// TODO evtl Optional<Guild> mit als parameter, um den request zu sparen?
		try {
			return (Category) getChannelById(server.getDisputeCategoryId()).block();
		} catch (ClientException e) {
			if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")
					&& !e.getErrorResponse().get().toString().contains("CHANNEL_PARENT_INVALID")) {
				throw e;
			}
			Guild guild = getGuildById(server.getGuildId()).block();
			Category disputeCategory = guild.createCategory("elo disputes")
					.withPermissionOverwrites(denyEveryoneView(server), allowAdminView(server), allowModView(server), allowBotView())
					.block();
			server.setDisputeCategoryId(disputeCategory.getId().asLong());
			dbService.saveServer(server);
			return disputeCategory;
		}
	}

	public Category getOrCreateArchiveCategory(Server server) {
		List<Long> categoryIds = server.getArchiveCategoryIds();
		Category archiveCategory;
		int index = 0;
		while (true) {
			if (index >= categoryIds.size()) {
				Guild guild = getGuildById(server.getGuildId()).block();
				archiveCategory = guild.createCategory(String.format("elo archive%s", index == 0 ? "" : index + 1))
						.withPermissionOverwrites(denyEveryoneView(server), allowAdminView(server), allowModView(server), allowBotView())
						.block();
				categoryIds.add(archiveCategory.getId().asLong());
				dbService.saveServer(server);
				break;
			}
			try {
				archiveCategory = (Category) getChannelById(categoryIds.get(index)).block();
			} catch (ClientException e) {
				if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")
						&& !e.getErrorResponse().get().toString().contains("CHANNEL_PARENT_INVALID")) {
					throw e;
				}
				Guild guild = getGuildById(server.getGuildId()).block();
				archiveCategory = guild.createCategory(String.format("elo archive%s", index == 0 ? "" : " " + (index + 1)))
						.withPermissionOverwrites(denyEveryoneView(server), allowAdminView(server), allowModView(server), allowBotView())
						.block();
				categoryIds.set(index, archiveCategory.getId().asLong());
				dbService.saveServer(server);
				break;
			}
			if (archiveCategory.getChannels().count().block() < 47) {
				break;
			}
			index++;
		}
		return archiveCategory;
	}

	public void moveToArchive(Server server, Channel channel) {
		Category archiveCategory = getOrCreateArchiveCategory(server);
		setParentCategory(channel, archiveCategory.getId().asLong()).subscribe();
		timedTaskQueue.addTimedTask(CHANNEL_DELETE, 1 * 60, channel.getId().asLong(), 0L, null);
		((TextChannel) channel).createMessage("**I have moved this channel to the archive. " +
				"I will delete this channel in one hour.**").subscribe();
	}

	public TextChannelEditMono setParentCategory(Channel channel, long categoryId) {
		return ((TextChannel) channel).edit().withParentId(Possible.of(Optional.of(Snowflake.of(categoryId))));
	}

	// Commands
	public void updateGuildCommandsByRanking(Server server) {
		if (server.getGames().isEmpty()) {
			deleteCommand(server, DeleteRanking.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, AddQueue.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, AddRank.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, DeleteRanks.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, Reset.class.getSimpleName().toLowerCase()).subscribe();
		} else {
			deployCommand(server, DeleteRanking.getRequest(server)).subscribe();
			deployCommand(server, AddQueue.getRequest(server)).subscribe();
			deployCommand(server, AddRank.getRequest(server)).subscribe();
			deployCommand(server, DeleteRanks.getRequest(server)).subscribe();
			deployCommand(server, Reset.getRequest(server)).subscribe();
		}
	}

	public void updateGuildCommandsByQueue(Server server) {
		if (server.getQueues().isEmpty()) {
			deleteCommand(server, Join.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, DeleteQueue.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, Edit.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, ForceWin.class.getSimpleName().toLowerCase()).subscribe();
		} else {
			deployCommand(server, Join.getRequest(server)).subscribe();
			deployCommand(server, DeleteQueue.getRequest(server)).subscribe();
			deployCommand(server, Edit.getRequest(server)).subscribe();
			deployCommand(server, ForceWin.getRequest(server)).subscribe();
		}
		if (server.getQueues().stream().filter(queue -> queue.getGame().isAllowDraw()).toList().isEmpty()) {
			deleteCommand(server, ForceDraw.class.getSimpleName().toLowerCase()).subscribe();
		} else {
			deployCommand(server, ForceDraw.getRequest(server)).subscribe();
		}
	}

	// TODO direkt schauen ob in dbService.adminCommands und entsprechend permissions setzen?
	// schauen wie es ist mit CommandParser, on(GuildCreateEvent.class)
	public Mono<ApplicationCommandData> deployCommand(Server server, ApplicationCommandRequest request) {
		return applicationService.createGuildApplicationCommand(botId, server.getGuildId(), request)
				.doOnNext(commandData -> log.debug(String.format("deployed command %s:%s to %s",
						commandData.name(), commandData.id(), getServerName(server))))
				.doOnError(ClientException.class, e ->
						log.error("Error deploying command:\n" + e.getRequest().toString()
								.replace(", ", ",\n")));
	}

	private Mono<Long> getCommandIdByName(long guildid, String commandName) {
		return applicationService.getGuildApplicationCommands(botId, guildid)
				.filter(applicationCommandData -> applicationCommandData.name().equals(commandName.toLowerCase()))
				.next()
				.map(commandData -> Long.parseLong(commandData.id()));
	}

	public Mono<Void> deleteCommand(Server server, String commandName) {
		log.debug("deleting command " + commandName);
		return getCommandIdByName(server.getGuildId(), commandName)
				.flatMap(commandId -> applicationService
						.deleteGuildApplicationCommand(botId, server.getGuildId(), commandId));
	}

	// Command Permissions
	public void setCommandPermissionForRole(Server server, String commandName, long roleId) {
		// discord api changes invalidated this code, and Discord4J does not currently support the workaround.
		// currently, command permissions are checked on our side in SlashCommand
		/*
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
		 */
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

	// view permissions
	public static PermissionOverwrite denyEveryoneView(Server server) {
		return PermissionOverwrite.forRole(Snowflake.of(server.getGuildId()),
				PermissionSet.none(),
				PermissionSet.of(Permission.VIEW_CHANNEL));
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

	public static PermissionOverwrite allowPlayerView(Player player) {
		return PermissionOverwrite.forMember(Snowflake.of(player.getUserId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}

	public PermissionOverwrite allowBotView() {
		return PermissionOverwrite.forMember(client.getSelfId(),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}
}
