package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.command.CommandParser;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.MessageCreateSpec;
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
		return client.getUserById(Snowflake.of(playerId)).block().getTag();
	}

	public Mono<Message> getMessageById(long channelId, long messageId) {
		return client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId));
	}
}
