package com.elorankingbot.backend.service;

import com.elorankingbot.backend.commands.admin.*;
import com.elorankingbot.backend.commands.admin.deleteranking.DeleteRanking;
import com.elorankingbot.backend.commands.mod.ForceDraw;
import com.elorankingbot.backend.commands.mod.ForceWin;
import com.elorankingbot.backend.commands.player.Join;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.google.common.collect.Iterables;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DiscordBotService {

	@Getter
	private final GatewayDiscordClient client;
	private final DBService dbService;
	private final ApplicationService applicationService;
	private final ApplicationPropertiesLoader props;
	private PrivateChannel ownerPrivateChannel;
	public final long botId;

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
		ownerPrivateChannel.createMessage(sendToOwnerTimeStamp.format(new Date()) + "\n" + text).subscribe();
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

	// Commands
	public String updateGuildCommandsByRanking(Server server) {
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
		return String.format("/%s, /%s, /%s, /%s, /%s",
				DeleteRanking.class.getSimpleName().toLowerCase(),
				AddQueue.class.getSimpleName().toLowerCase(),
				AddRank.class.getSimpleName().toLowerCase(),
				DeleteRanks.class.getSimpleName().toLowerCase(),
				Reset.class.getSimpleName().toLowerCase());
	}

	public String updateGuildCommandsByQueue(Server server) {
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
		return String.format("/%s, /%s, /%s, /%s, (/%s)",
				Join.class.getSimpleName().toLowerCase(),
				DeleteQueue.class.getSimpleName().toLowerCase(),
				Edit.class.getSimpleName().toLowerCase(),
				ForceWin.class.getSimpleName().toLowerCase(),
				ForceDraw.class.getSimpleName().toLowerCase());
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

	public Mono<List<ApplicationCommandData>> getAllGlobalCommands() {
		return applicationService.getGlobalApplicationCommands(botId).collectList();
	}

	public Mono<List<ApplicationCommandData>> getAllGuildCommands(long guildId) {
		return applicationService.getGuildApplicationCommands(botId, guildId).collectList();
	}

	// Command Permissions
	public void setCommandPermissionForRole(Server server, String commandName, long roleId) {
		// discord api changes invalidated this code, and Discord4J does not currently support the workaround.
		// currently, command permissions are checked on our side in Command
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
}
