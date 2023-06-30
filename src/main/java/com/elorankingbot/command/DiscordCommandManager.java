package com.elorankingbot.command;

import com.elorankingbot.commands.mod.ForceDraw;
import com.elorankingbot.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.logging.ExceptionHandler;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.DBService;
import com.elorankingbot.service.DiscordBotService;
import com.elorankingbot.service.Services;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@CommonsLog
public class DiscordCommandManager {

    private final DBService dbService;
    private final DiscordBotService bot;
    private final CommandClassScanner commandClassScanner;
    private final ExceptionHandler exceptionHandler;
    private final ApplicationService applicationService;
    private final List<ApplicationCommandData> currentGlobalCommands;
    private final Set<String> neededGlobalCommandsNames, neededOwnerCommands, allNeededGuildCommandNames;
    private final long botId;
    private final Consumer<Object> NO_OP = object -> {
    };

    public DiscordCommandManager(Services services) {
        dbService = services.dbService;
        bot = services.bot;
        commandClassScanner = services.commandClassScanner;
        exceptionHandler = services.exceptionHandler;
        applicationService = services.client.rest().getApplicationService();
        neededGlobalCommandsNames = commandClassScanner.getGlobalCommandClassNames();
        neededOwnerCommands = commandClassScanner.getOwnerCommandClassNames();
        allNeededGuildCommandNames = new HashSet<>(neededOwnerCommands);
        allNeededGuildCommandNames.addAll(commandClassScanner.getRankingCommandClassNames());
        allNeededGuildCommandNames.addAll(commandClassScanner.getQueueCommandClassNames());
        botId = services.client.getSelfId().asLong();
        currentGlobalCommands = applicationService.getGlobalApplicationCommands(botId).collectList().block();

        deleteSuperfluousGlobalCommands();
        deployNeededGlobalCommands();
        deleteSuperfluousGuildCommands(services.props);
        deployNeededOwnerCommands(services.props);
    }

    private void deleteSuperfluousGlobalCommands() {
        currentGlobalCommands.stream()
                .filter(command -> !neededGlobalCommandsNames.contains(command.name().toLowerCase().replace(" ", "")))
                .forEach(command -> deleteGlobalCommand(command.name(), command.id().asLong()));
    }

    private void deployNeededGlobalCommands() {
        Set<String> currentGlobalCommandsNames = currentGlobalCommands.stream()
                .map(applicationCommandData -> applicationCommandData.name().toLowerCase().replace(" ", ""))
                .collect(Collectors.toSet());
        neededGlobalCommandsNames.stream()
                .filter(necessaryCommandName -> !currentGlobalCommandsNames.contains(necessaryCommandName))
                .forEach(this::deployGlobalCommand);
    }

    private void deleteSuperfluousGuildCommands(ApplicationPropertiesLoader props) {
        props.getTestServerIds().forEach(testServerId ->
                applicationService.getGuildApplicationCommands(botId, testServerId).subscribe(applicationCommandData -> {
                    if (!allNeededGuildCommandNames.contains(applicationCommandData.name()))
                        applicationService.deleteGuildApplicationCommand(botId, testServerId, applicationCommandData.id().asLong()).subscribe();
                }));
    }

    private void deployNeededOwnerCommands(ApplicationPropertiesLoader props) {
        props.getTestServerIds().forEach(testServerId ->
                applicationService.getGuildApplicationCommands(botId, testServerId).map(ApplicationCommandData::name)
                        .collectList().subscribe(deployedCommandNames ->
                                neededOwnerCommands.forEach(necessaryCommandName -> {
                                    if (!deployedCommandNames.contains(necessaryCommandName)) {
                                        deployGuildCommand(dbService.getOrCreateServer(testServerId), necessaryCommandName,
                                                exceptionHandler.updateCommandFailedCallbackFactory());
                                    }})));
    }

    public String updateGameCommands(Server server, BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory) {
        Set<String> updatedCommandNames = new HashSet<>();
        if (server.getGames().isEmpty()) {
            commandClassScanner.getRankingCommandClassNames().forEach(
                    commandClassName -> {
                        deleteGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName.toLowerCase());
                    });
        } else {
            commandClassScanner.getRankingCommandClassNames().forEach(commandClassName -> {
                        deployGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName.toLowerCase());
                    }
            );
        }
        return "/" + String.join(", /", updatedCommandNames);
    }

    public String updateQueueCommands(Server server, BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory) {
        Set<String> updatedCommandNames = new HashSet<>();
        if (server.getGames().isEmpty()) {
            commandClassScanner.getQueueCommandClassNames().forEach(
                    commandClassName -> {
                        deleteGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName.toLowerCase());
                    });
        } else {
            commandClassScanner.getQueueCommandClassNames().forEach(
                    commandClassName -> {
                        deployGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName.toLowerCase());
                    }
            );
        }
        // special case ForceDraw depends on allowDraw
        if (server.getQueues().stream().filter(queue -> queue.getGame().isAllowDraw()).toList().isEmpty()) {
            deleteGuildCommand(server, ForceDraw.class.getSimpleName().toLowerCase(), updateCommandFailedCallbackFactory);
        } else {
            deployGuildCommand(server, ForceDraw.class.getSimpleName().toLowerCase(), updateCommandFailedCallbackFactory);
        }
        updatedCommandNames.add(ForceDraw.class.getSimpleName().toLowerCase());
        return "/" + String.join(", /", updatedCommandNames);
    }

    private void deployGlobalCommand(String commandName) {
        String commandClassFullName = commandClassScanner.getFullClassName(commandName);
        ApplicationCommandRequest request = null;
        try {
            request = (ApplicationCommandRequest) Class.forName(commandClassFullName)
                    .getMethod("getRequest").invoke(null);
        } catch (Exception e) {
            exceptionHandler.handleException(e, this.getClass().getSimpleName() + "::deployGlobalCommand : " + commandName);
        }
        applicationService.createGlobalApplicationCommand(botId, request)
                .subscribe(commandData -> log.warn(String.format("deployed global command %s:%s",
                                commandData.id(), commandData.name())),
                        throwable -> exceptionHandler.handleException(throwable, this.getClass().getSimpleName() + "::deployGlobalCommand"));
    }

    private void deleteGlobalCommand(String commandName, long commandId) {
        log.warn(String.format("deleting global command %s:%s", commandId, commandName));
        applicationService.deleteGlobalApplicationCommand(botId, commandId)
                .subscribe(NO_OP, throwable -> exceptionHandler.handleException(throwable, "DiscordCommandService::deleteGlobalCommand"));
    }

    private void deployGuildCommand(Server server, String commandName, BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory) {
        applicationService.createGuildApplicationCommand(botId, server.getGuildId(), getRequestByCommandName(commandName, server))
                .subscribe(commandData -> log.info(String.format("deployed command %s:%s to %s",
                                commandData.id(), commandData.name(), bot.getServerIdAndName(server))),
                        updateCommandFailedCallbackFactory.apply(commandName, true));
    }

    private ApplicationCommandRequest getRequestByCommandName(String commandName, Server server) {
        String fullCommandClassName = commandClassScanner.getFullClassName(commandName);
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

    private void deleteGuildCommand(Server server, String commandName, BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory) {
        log.info(String.format("deleting command %s on %s", commandName, bot.getServerIdAndName(server)));
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
}
