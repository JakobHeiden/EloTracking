package com.elorankingbot.backend.service;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.*;
import com.google.common.base.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;
	private final EloRankingService service;
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

	public DiscordBotService(@Lazy Services services) {
		this.client = services.client();
		this.service = services.service();
		this.botId = client.getSelfId().asLong();
		this.props = services.props();
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

	public String getPlayerTag(long playerId) {
		return client.getUserById(Snowflake.of(playerId)).block().getTag();
	}

	public Mono<Message> getMessageById(long messageId, long channelId) {
		return client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId));
	}

	public Mono<Guild> getGuildById(long guildId) {
		return client.getGuildById(Snowflake.of(guildId));
	}

	public void postToResultChannel(Game game, MatchResult matchResult) {
		/*
		TextChannel resultChannel;
		try {
			resultChannel = (TextChannel) client.getChannelById(Snowflake.of(game.getResultChannelId())).block();
		} catch (ClientException e) {
			resultChannel = Setup.createResultChannel(getGuildById(game.getGuildId()).block(), game);
			game.setResultChannelId(resultChannel.getId().asLong());
			service.saveGame(game);
		}
		resultChannel.createMessage(String.format("%s (%s, +%s) %s %s (%s, -%s)",
						match.getWinnerTag(),
						formatRating(match.getWinnerNewRating()),
						formatRating(match.getWinnerNewRating() - match.getWinnerOldRating()),
						match.isDraw() ? "drew" : "defeated",
						match.getLoserTag(),
						formatRating(match.getLoserNewRating()),
						formatRating(match.getLoserOldRating() - match.getLoserNewRating())))
				.subscribe();

		 */
	}

	// TODO setting ob wins/losses angezeigt wird
	public void updateLeaderboard(Game game) {
		/*
		Message leaderboardMessage;
		try {
			leaderboardMessage = getMessageById(game.getLeaderboardMessageId(), game.getLeaderboardChannelId()).block();
		} catch (ClientException e) {
			Setup.createLeaderboardChannelAndMessage(getGuildById(game.getGuildId()).block(), game);
			service.saveGame(game);
			leaderboardMessage = getMessageById(game.getLeaderboardMessageId(), game.getLeaderboardChannelId()).block();
		}

		List<Player> playerList = service.getRankings(game.getGuildId());
		int numTotalPlayers = playerList.size();
		if (numTotalPlayers > game.getLeaderboardLength())
			playerList = playerList.subList(0, game.getLeaderboardLength());

		leaderboardMessage.edit().withContent("\n").withEmbeds(
						generateLeaderboardEmbed(playerList, numTotalPlayers, game, 1, -1))
				.subscribe();

		 */
	}

	public EmbedCreateSpec generateLeaderboardEmbed(List<Player> playerList, int numTotalPlayers, Game game,
													int rankOffset, int rankToHighlight) {
		return null;
		/*
		String leaderboardString = "";
		for (int i = 0; i < playerList.size(); i++) {
			Player player = playerList.get(i);
			String numDrawsString = game.isAllowDraw() ? entryOf(player.getDraws(), embedWinsSpaces) : "";
			String leaderboardEntry = entryOf(i + rankOffset, embedRankSpaces)
					+ entryOf(player.getRating(), embedRatingSpaces)
					+ entryOf(player.getWins(), embedWinsSpaces)
					+ entryOf(player.getLosses(), embedWinsSpaces)
					+ numDrawsString
					+ "  " + player.getTag() + "\n";
			if (i == rankToHighlight) leaderboardEntry = "+" + leaderboardEntry.substring(1);
			leaderboardString += leaderboardEntry;
		}
		if (leaderboardString.equals("")) leaderboardString = "no games played so far";
		leaderboardString = "```diff\n" + leaderboardString + "```";

		return EmbedCreateSpec.builder()
				.title(game.getName() + " Rankings")
				.addField(EmbedCreateFields.Field.of(
						game.isAllowDraw() ? embedNameWithDraws : embedName,
						leaderboardString,
						true))
				.footer(String.format("%s players total", numTotalPlayers), null)
				.build();

		 */
	}

	private String entryOf(String data, int totalSpaces) {
		data = Strings.padEnd(data, (totalSpaces + data.length()) / 2, ' ');
		return Strings.padStart(data, totalSpaces, ' ');
	}

	private String entryOf(int data, int totalSpaces) {
		return entryOf(String.valueOf(data), totalSpaces);
	}

	private String entryOf(double data, int totalSpaces) {
		return entryOf(EloRankingService.formatRating(data), totalSpaces);
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

	public void setAdminAndModPermissionsToModCommand(Server server, String commandName) {
		if (server.getAdminRoleId() != 0L) {
			setCommandPermissionForRole(server, commandName, server.getAdminRoleId());
		}
		if (server.getModRoleId() != 0L) {
			setCommandPermissionForRole(server, commandName, server.getModRoleId());
		}
	}

	public void setAdminPermissionToAdminCommand(Server server, String commandName) {
		if (server.getAdminRoleId() != 0L) {
			setCommandPermissionForRole(server, commandName, server.getAdminRoleId());
		}
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
