package com.elorankingbot.backend.service;

import com.elorankingbot.backend.commands.admin.Setup;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Player;
import com.google.common.base.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateFields;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

import static com.elorankingbot.backend.service.EloRankingService.formatRating;

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;
	private final EloRankingService service;
	private final ApplicationService applicationService;
	private PrivateChannel ownerPrivateChannel;
	private final long botId;
	@Getter
	private String latestCommandLog;

	private static int embedRankSpaces = 6;
	private static int embedRatingSpaces = 8;
	private static int embedWinsSpaces = 5;// TODO abhaengig von den daten
	private static String embedName = "`   Rank  Rating   Wins Losses  Name`";
	private static String embedNameWithDraws = "`   Rank  Rating    Wins Losses Draws Name`";

	public DiscordBotService(GatewayDiscordClient gatewayDiscordClient, EloRankingService service) {
		this.client = gatewayDiscordClient;
		this.service = service;
		this.botId = client.getSelfId().asLong();
		applicationService = client.getRestClient().getApplicationService();
	}

	@PostConstruct
	public void initAdminDm() {
		long ownerId = Long.valueOf(service.getPropertiesLoader().getOwnerId());
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

	public void postToResultChannel(Game game, Match match) {
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
	public Mono<ApplicationCommandData> deployCommand(long guildId, ApplicationCommandRequest request) {
		return applicationService.createGuildApplicationCommand(botId, guildId, request)
				.doOnNext(commandData -> log.debug(String.format("deployed command %s:%s to %s",
						commandData.name(), commandData.id(), guildId)));
	}

	public Mono<Void> deleteCommand(long guildId, String name) {
		log.debug("deleting command " + name);
		return getCommandIdByName(guildId, name)
				.flatMap(commandId -> applicationService.deleteGuildApplicationCommand(botId, guildId, commandId));
	}

	public Flux<ApplicationCommandData> deleteAllGuildCommands(long guildId) {
		return applicationService.getGuildApplicationCommands(botId, guildId)
				.doOnNext(commandData -> log.trace(String.format("deleting command %s:%s on %s",
						commandData.name(), commandData.id(), guildId)))
				.doOnNext(commandData -> applicationService.deleteGuildApplicationCommand(
						botId, guildId, Long.parseLong(commandData.id())).subscribe());
	}

	public void setDiscordCommandPermissions(long guildId, String commandName, Role... roles) {
		var requestBuilder = ApplicationCommandPermissionsRequest.builder();
		Arrays.stream(roles).forEach(role -> {
			log.debug(String.format("setting permissions for command %s to role %s", commandName, role.getName()));
			requestBuilder.addPermission(ApplicationCommandPermissionsData.builder()
					.id(role.getId().asLong()).type(1).permission(true).build()).build();
		});
		getCommandIdByName(guildId, commandName.toLowerCase()).subscribe(commandId ->
				applicationService.modifyApplicationCommandPermissions(botId, guildId, commandId, requestBuilder.build())
						.subscribe(permissionsData -> log.debug("...to command " + permissionsData.id())));
	}

	private Mono<Long> getCommandIdByName(long guildid, String commandName) {
		return applicationService.getGuildApplicationCommands(botId, guildid)
				.filter(applicationCommandData -> applicationCommandData.name().equals(commandName))
				.next()
				.map(commandData -> Long.parseLong(commandData.id()));
	}
}
