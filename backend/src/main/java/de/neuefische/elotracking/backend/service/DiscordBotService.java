package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.command.CommandParser;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;// TODO vllt refaktorieren und den commands die referenz geben
	private final EloTrackingService service;
	private final TimedTaskQueue queue;
	private PrivateChannel ownerPrivateChannel;


	public DiscordBotService(GatewayDiscordClient gatewayDiscordClient, EloTrackingService service, @Lazy CommandParser commandParser,
							 TimedTaskQueue queue) {
		this.client = gatewayDiscordClient;
		this.service = service;
		this.queue = queue;
	}

	@PostConstruct
	public void initAdminDm() {
		long ownerId = Long.valueOf(service.getPropertiesLoader().getOwnerId());
		User owner = client.getUserById(Snowflake.of(ownerId)).block();
		this.ownerPrivateChannel = owner.getPrivateChannel().block();
		log.info("Private channel to owner established");
		sendToOwner("I am logged in and ready");
	}

	public void sendToOwner(String text) {
		if (text == null) text = "null";
		if (text.equals("")) text = "empty String";
		ownerPrivateChannel.createMessage(text).subscribe();
	}

	public void sendToChannel(long channelId, String text) {
		TextChannel channel = (TextChannel) client.getChannelById(Snowflake.of(channelId)).block();
		channel.createMessage(text).subscribe();
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

	public String getPlayerName(long playerId) {
		return client.getUserById(Snowflake.of(playerId)).block().getUsername();
	}

	public Mono<Message> getMessageById(long channelId, long messageId) {
		return client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId));
	}

	public void postToResultChannel(Game game, Match match) {
		if (game.getResultChannelId() != 0L) {
			try {
				TextChannel resultChannel = (TextChannel) client.getChannelById(Snowflake.of(game.getResultChannelId())).block();
				resultChannel.createMessage(String.format("%s (%s) %s %s (%s)",
								match.getWinnerTag(client), Math.round(match.getWinnerNewRating()),
								match.isDraw() ? "drew" : "defeated",
								match.getLoserTag(client), Math.round(match.getLoserNewRating())))
						.subscribe();
			} catch (ClientException e) {
				game.setResultChannelId(0L);
				service.saveGame(game);
			}
		}
	}

	public Mono<Message> getChallengerMessage(ChallengeModel challenge) {// TODO schauen wo das noch uber client gemacht wird
		return client.getMessageById(Snowflake.of(challenge.getChallengerChannelId()),
				Snowflake.of(challenge.getChallengerMessageId()));
	}

	public Mono<Message> getAcceptorMessage(ChallengeModel challenge) {
		return client.getMessageById(Snowflake.of(challenge.getAcceptorChannelId()),
				Snowflake.of(challenge.getAcceptorMessageId()));
	}
}
