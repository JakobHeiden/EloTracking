package com.elorankingbot.backend.service;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.tools.EmbedBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
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

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;
	private final DBService dbservice;
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
		this.dbservice = services.dbService;
		this.botId = client.getSelfId().asLong();
		this.props = services.props;
		applicationService = client.getRestClient().getApplicationService();
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

	public Mono<User> getUser(Player player) {
		return getUser(player.getUserId());
	}

	public Mono<User> getUser(long userId) {
		return client.getUserById(Snowflake.of(userId));
	}

	public String getPlayerTag(long playerId) {
		return client.getUserById(Snowflake.of(playerId)).block().getTag();
	}

	public Mono<Message> getMessageById(long messageId, long channelId) {
		return client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId));
	}

	public Mono<Message> getPlayerMessage(Player player, Match match) {
		return getMessageById(match.getMessageId(player.getId()), match.getPrivateChannelId(player.getId()));
	}

	public Mono<Guild> getGuildById(long guildId) {
		return client.getGuildById(Snowflake.of(guildId));
	}

	// Channels
	public TextChannel createResultChannel(Server server) {
		Guild guild = getGuildById(server.getGuildId()).block();
		TextChannel resultChannel = guild.createTextChannel("Elo Rating match results")
				.withPermissionOverwrites(PermissionOverwrite.forRole(
						Snowflake.of(guild.getId().asLong()),
						PermissionSet.none(),
						PermissionSet.of(Permission.SEND_MESSAGES)))
				.block();
		server.setResultChannelId(resultChannel.getId().asLong());
		return resultChannel;
	}

	public Message createLeaderboardChannelAndMessage(Server server) {
		Guild guild = getGuildById(server.getGuildId()).block();
		TextChannel leaderboardChannel = guild.createTextChannel("Elo Rankings")
				.withPermissionOverwrites(PermissionOverwrite.forRole(
						Snowflake.of(guild.getId().asLong()),
						PermissionSet.none(),
						PermissionSet.of(Permission.SEND_MESSAGES)))
				.block();
		Message leaderboardMessage = leaderboardChannel.createMessage("creating leaderboard...").block();
		server.setLeaderboardMessageId(leaderboardMessage.getId().asLong());
		server.setLeaderboardChannelId(leaderboardChannel.getId().asLong());
		return leaderboardMessage;
	}

	public void postToResultChannel(MatchResult matchResult) {
		Server server = matchResult.getGame().getServer();
		TextChannel resultChannel;
		try {
			resultChannel = (TextChannel) client.getChannelById(Snowflake.of(server.getResultChannelId())).block();
		} catch (ClientException e) {
			resultChannel = createResultChannel(server);
			dbservice.saveServer(server);
		}
		resultChannel.createMessage(EmbedBuilder.createMatchResultEmbed(matchResult)).subscribe();
	}

	public void refreshLeaderboard(Server server) {
		Message leaderboardMessage;
		try {
			leaderboardMessage = getMessageById(server.getLeaderboardMessageId(), server.getLeaderboardChannelId()).block();
		} catch (ClientException e) {
			leaderboardMessage = createLeaderboardChannelAndMessage(server);
			dbservice.saveServer(server);
		}

		List<EmbedCreateSpec> embeds = new ArrayList<>();
		for (Game game : server.getGames()) {
			embeds.add(EmbedBuilder.createRankingsEmbed(dbservice.getLeaderboard(game)));
		}
		leaderboardMessage.edit().withContent(Possible.of(Optional.empty())).withEmbedsOrNull(embeds).subscribe();
	}

	public Mono<Message> getChallengerMessage(ChallengeModel challenge) {// TODO schauen wo das noch uber client gemacht wird
		return client.getMessageById(Snowflake.of(challenge.getChallengerChannelId()),
				Snowflake.of(challenge.getChallengerMessageId()));
	}

	public Mono<Message> getAcceptorMessage(ChallengeModel challenge) {
		return client.getMessageById(Snowflake.of(challenge.getAcceptorChannelId()),
				Snowflake.of(challenge.getAcceptorMessageId()));
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
		getCommandIdByName(server.getGuildId(), commandName).subscribe(commandId ->	applicationService
				.modifyApplicationCommandPermissions(botId, server.getGuildId(), commandId, request).subscribe());
	}

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
