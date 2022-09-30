package com.elorankingbot.backend.service;

import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.Server;
import com.google.common.collect.Iterables;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiscordBotService {

	private final GatewayDiscordClient client;
	private final DBService dbService;
	private final ApplicationService applicationService;
	private final ApplicationPropertiesLoader props;
	private PrivateChannel ownerPrivateChannel;
	@Getter
	private final long botId;

	private SimpleDateFormat sendToOwnerTimeStamp = new SimpleDateFormat("HH:mm:ss");

	public DiscordBotService(Services services) {
		this.client = services.client;
		this.dbService = services.dbService;
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
		ownerPrivateChannel.createMessage(sendToOwnerTimeStamp.format(new Date()) + ": " + text).subscribe();
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

	public void sendDM(User user, ChatInputInteractionEvent event, String content) {
		user.getPrivateChannel()
				.flatMap(privateChannel -> privateChannel.createMessage(content))
				.subscribe(messageIgnored -> {},
						throwable -> event.createFollowup(dmFailedFollowupMessage(user.getTag())).subscribe());
	}

	public void sendDM(User user, ChatInputInteractionEvent event, EmbedCreateSpec content) {
		user.getPrivateChannel()
				.flatMap(privateChannel -> privateChannel.createMessage(content))
				.subscribe(messageIgnored -> {},
						throwable -> event.createFollowup(dmFailedFollowupMessage(user.getTag())).subscribe());
	}

	private String dmFailedFollowupMessage(String tag) {
		return String.format("I was not able to inform %s. Most likely their DMs are closed.", tag);
	}

	public void sendDM(User user, String content) {
		user.getPrivateChannel()
				.flatMap(privateChannel -> privateChannel.createMessage(content))
				.subscribe(messageIgnored -> {}, throwableIgnored -> {});
	}

	public Mono<Channel> getChannelById(long channelId) {
		//return client.withRetrievalStrategy(EntityRetrievalStrategy.REST).getChannelById(Snowflake.of(channelId));
		return client.getChannelById(Snowflake.of(channelId));
	}

	public void deleteChannel(long channelId) {
		client.getChannelById(Snowflake.of(channelId))
				.onErrorContinue(((throwable, o) -> log.info(String.format("Channel %s is already deleted.", channelId))))
				.subscribe(channel -> channel.delete().subscribe());
	}
}
