package com.elorankingbot.backend.service;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.logging.ExceptionHandler;
import com.elorankingbot.backend.model.Server;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Service
@CommonsLog
public class DiscordCommandService {

    private final DiscordBotService bot;
    private final GatewayDiscordClient client;
    private final CommandClassScanner commandClassScanner;
    private final ExceptionHandler exceptionHandler;
    private final ApplicationService applicationService;
    private final ApplicationPropertiesLoader props;
    private final long botId;

    private final Consumer<Object> NO_OP = object -> {
    };

    public DiscordCommandService(Services services) {
        this.bot = services.bot;
        this.client = services.client;
        this.commandClassScanner = services.commandClassScanner;
        this.exceptionHandler = services.exceptionHandler;
        this.applicationService = client.rest().getApplicationService();
        this.props = services.props;
        this.botId = client.getSelfId().asLong();
    }

    public void deployGlobalCommand(String commandName) {
        String commandClassFullName = commandClassScanner.getFullClassName(commandName);
        ApplicationCommandRequest request = null;
        try {
            request = (ApplicationCommandRequest) Class.forName(commandClassFullName)
                    .getMethod("getRequest").invoke(null);
        } catch (Exception e) {
            exceptionHandler.handleException(e, this.getClass().getSimpleName() + "::deployGlobalCommand : " + commandName);
        }
        applicationService.createGlobalApplicationCommand(botId, request)
                .subscribe(commandData -> log.info(String.format("deployed global command %s:%s",
                                commandData.name(), commandData.id())),
                        throwable -> exceptionHandler.handleException(throwable, this.getClass().getSimpleName() + "::deployGlobalCommand"));
    }

    public void deleteGlobalCommand(String commandName, long commandId) {
        log.info(String.format("deleting global command %s:%s", commandId, commandName));
        applicationService.deleteGlobalApplicationCommand(botId, commandId)
                .subscribe(NO_OP, throwable -> exceptionHandler.handleException(throwable, "DiscordCommandService::deleteGlobalCommand"));
    }

    public void deployGuildCommand(Server server, String commandName, BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory) {
        applicationService.createGuildApplicationCommand(botId, server.getGuildId(), getRequestByCommandName(commandName, server))
                .subscribe(commandData -> log.info(String.format("deployed command %s:%s to %s",
                                commandData.name(), commandData.id(), bot.getServerName(server))),
                        updateCommandFailedCallbackFactory.apply(commandName, true));
    }

    private ApplicationCommandRequest getRequestByCommandName(String commandName, Server server) {
        String fullCommandClassName = commandClassScanner.getFullClassName(commandName);
        System.out.println(fullCommandClassName);
        ApplicationCommandRequest request = null;
        try {
            request = (ApplicationCommandRequest) Class.forName(fullCommandClassName)
                    .getMethod("getRequest", Server.class)
                    .invoke(null, server);
        } catch (Exception e) {
            exceptionHandler.handleException(e, this.getClass().getSimpleName() + "::getRequestByCommandName");
        }
        return request;
    }

    public void deleteGuildCommand(Server server, String commandName, BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory) {
        log.info(String.format("deleting command %s on %s", commandName, bot.getServerName(server)));
        getGuildCommandIdByName(server.getGuildId(), commandName)
                .flatMap(commandId -> applicationService.deleteGuildApplicationCommand(botId, server.getGuildId(), commandId))
                .subscribe(NO_OP, updateCommandFailedCallbackFactory.apply(commandName, false));
    }

    private Mono<Long> getGuildCommandIdByName(long guildid, String commandName) {
        return applicationService.getGuildApplicationCommands(botId, guildid)
                .filter(applicationCommandData -> applicationCommandData.name().equals(commandName.toLowerCase()))
                .next()
                .map(commandData -> commandData.id().asLong());
    }

    public Mono<List<ApplicationCommandData>> getGlobalCommands() {
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
        getGuildCommandIdByName(server.getGuildId(), commandName).subscribe(commandId -> applicationService
                .modifyApplicationCommandPermissions(botId, server.getGuildId(), commandId, request).subscribe());
    }
}
