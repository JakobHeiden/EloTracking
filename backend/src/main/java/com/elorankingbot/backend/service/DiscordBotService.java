package com.elorankingbot.backend.service;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateMono;
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

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;
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

	public MessageCreateMono sendToChannel(long channelId, String text) {
		MessageChannel channel = (MessageChannel) client.getChannelById(Snowflake.of(channelId)).block();
		return channel.createMessage(text);
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
			log.debug(String.format("setting permissions for command %s to role %s",commandName, role.getName()));
			requestBuilder.addPermission(ApplicationCommandPermissionsData.builder()
					.id(role.getId().asLong()).type(1).permission(true).build()).build();
		});
		getCommandIdByName(guildId, commandName).subscribe(commandId ->
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
