package com.elorankingbot.backend.service;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.*;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;// TODO vllt refaktorieren und den commands die referenz geben
	private final EloRankingService service;
	private final TimedTaskQueue queue;
	private final ApplicationService applicationService;
	private PrivateChannel ownerPrivateChannel;
	private final long botId;


	public DiscordBotService(GatewayDiscordClient gatewayDiscordClient, EloRankingService service, TimedTaskQueue queue) {
		this.client = gatewayDiscordClient;
		this.service = service;
		this.queue = queue;
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
								match.getWinnerTag(client), service.formatRating(match.getWinnerNewRating()),
								match.isDraw() ? "drew" : "defeated",
								match.getLoserTag(client), service.formatRating(match.getLoserNewRating())))
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

	// Commands
	public Mono<ApplicationCommandData> deployCommandToGuild(ApplicationCommandRequest request, Guild guild, Role... permitRoles) {
		return applicationService.createGuildApplicationCommand(botId, guild.getId().asLong(), request)
				.doOnNext(commandData -> log.trace(String.format("deployed command %s:%s to %s",
						commandData.name(), commandData.id(), guild.getId().asString())))
				.doOnNext(commandData -> setDiscordCommandPermissions(guild, commandData, permitRoles));
	}

	public Mono<ApplicationCommandData> deployCommandToGuild(ApplicationCommandRequest request, long guildId) {
		return applicationService.createGuildApplicationCommand(botId, guildId, request)
				.doOnNext(commandData -> log.trace(String.format("deployed command %s:%s to %s",
						commandData.name(), commandData.id(), guildId)));
	}

	public Flux<ApplicationCommandData> deleteAllGuildCommands(long guildId) {
		return applicationService.getGuildApplicationCommands(botId, guildId)
				.doOnNext(commandData -> log.trace(String.format("deleting command %s:%s on %s",
						commandData.name(), commandData.id(), guildId)))
				.doOnNext(commandData -> applicationService.deleteGuildApplicationCommand(
						botId, guildId, Long.parseLong(commandData.id())).subscribe());
	}

	private void setDiscordCommandPermissions(Guild guild, ApplicationCommandData commandData, Role... roles) {
		ImmutableApplicationCommandPermissionsRequest.Builder requestBuilder = ApplicationCommandPermissionsRequest.builder();
		Arrays.stream(roles).map(role -> ApplicationCommandPermissionsData.builder()
						.id(role.getId().asLong()).type(1).permission(true).build())
				.forEach(applicationCommandPermissionsData -> requestBuilder.addPermission(applicationCommandPermissionsData));
		applicationService.modifyApplicationCommandPermissions(
						botId, guild.getId().asLong(), Long.parseLong(commandData.id()), requestBuilder.build())
				.subscribe();
	}// TODO permissions fuer owner setzen?
}
