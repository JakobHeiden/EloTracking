package com.elorankingbot.backend.service;

import com.elorankingbot.backend.commands.admin.*;
import com.elorankingbot.backend.commands.admin.deleteranking.DeleteRanking;
import com.elorankingbot.backend.commands.mod.ForceDraw;
import com.elorankingbot.backend.commands.mod.ForceWin;
import com.elorankingbot.backend.commands.mod.SetRating;
import com.elorankingbot.backend.commands.player.Join;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.Server;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class DiscordCommandService {

	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final ApplicationService applicationService;
	private final ApplicationPropertiesLoader props;
	private final long botId;

	public DiscordCommandService(Services services) {
		this.bot = services.bot;
		this.client = services.client;
		this.applicationService = client.rest().getApplicationService();
		this.props = services.props;
		this.botId = client.getSelfId().asLong();
	}

	public String updateGuildCommandsByRanking(Server server) {
		if (server.getGames().isEmpty()) {
			deleteCommand(server, DeleteRanking.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, AddQueue.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, AddRank.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, DeleteRanks.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, Reset.class.getSimpleName().toLowerCase()).subscribe();
			deleteCommand(server, SetRating.class.getSimpleName().toLowerCase()).subscribe();
		} else {
			deployCommand(server, DeleteRanking.getRequest(server)).subscribe();
			deployCommand(server, AddQueue.getRequest(server)).subscribe();
			deployCommand(server, AddRank.getRequest(server)).subscribe();
			deployCommand(server, DeleteRanks.getRequest(server)).subscribe();
			deployCommand(server, Reset.getRequest(server)).subscribe();
			deployCommand(server, SetRating.getRequest(server)).subscribe();
		}
		return String.format("/%s, /%s, /%s, /%s, /%s, /%s",
				DeleteRanking.class.getSimpleName().toLowerCase(),
				AddQueue.class.getSimpleName().toLowerCase(),
				AddRank.class.getSimpleName().toLowerCase(),
				DeleteRanks.class.getSimpleName().toLowerCase(),
				Reset.class.getSimpleName().toLowerCase(),
				SetRating.class.getSimpleName().toLowerCase());
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
						commandData.name(), commandData.id(), bot.getServerName(server))))
				.doOnError(ClientException.class, e ->
						log.error("Error deploying command:\n" + e.getRequest().toString()
								.replace(", ", ",\n")));
	}

	private Mono<Long> getCommandIdByName(long guildid, String commandName) {
		return applicationService.getGuildApplicationCommands(botId, guildid)
				.filter(applicationCommandData -> applicationCommandData.name().equals(commandName.toLowerCase()))
				.next()
				.map(commandData -> commandData.id().asLong());
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
