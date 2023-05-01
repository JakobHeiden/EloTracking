package com.elorankingbot.backend.command;

import com.elorankingbot.backend.commands.mod.ForceDraw;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DiscordCommandService;
import com.elorankingbot.backend.service.Services;
import discord4j.discordjson.json.ApplicationCommandData;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class DiscordCommandManager {

    private final DiscordCommandService discordCommandService;
    private final CommandClassScanner commandClassScanner;
    private List<ApplicationCommandData> currentGlobalCommands;
    private Set<String> necessaryGlobalCommandsNames;

    public DiscordCommandManager(Services services) {
        discordCommandService = services.discordCommandService;
        commandClassScanner = services.commandClassScanner;
        currentGlobalCommands = discordCommandService.getGlobalCommands().block();
        necessaryGlobalCommandsNames = commandClassScanner.getGlobalCommandClassNames();
        System.out.println("current");
        currentGlobalCommands.forEach(applicationCommandData -> System.out.println(applicationCommandData.name()));
        System.out.println("necessary");
        necessaryGlobalCommandsNames.forEach(System.out::println);
        deleteSuperfluousGlobalCommands();
        deployNeededGlobalCommands();
    }

    private void deleteSuperfluousGlobalCommands() {
        currentGlobalCommands.stream()
                .filter(command -> !necessaryGlobalCommandsNames.contains(command.name()))
                .forEach(command -> discordCommandService.deleteGlobalCommand(command.name(), command.id().asLong()));
    }

    private void deployNeededGlobalCommands() {
        Set<String> currentGlobalCommandsNames = currentGlobalCommands.stream()
                .map(ApplicationCommandData::name)
                .collect(Collectors.toSet());
        necessaryGlobalCommandsNames.stream()
                .filter(necessaryCommandName -> !currentGlobalCommandsNames.contains(necessaryCommandName))
                .forEach(discordCommandService::deployGlobalCommand);
    }

    public String updateRankingCommands(Server server, BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory) {
        Set<String> updatedCommandNames = new HashSet<>();
        if (server.getGames().isEmpty()) {
            commandClassScanner.getRankingCommandClassNames().forEach(
                    commandClassName -> {
                        discordCommandService.deleteGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName);
                    });
        } else {
            commandClassScanner.getRankingCommandClassNames().forEach(commandClassName -> {
                        discordCommandService.deployGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName);
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
                        discordCommandService.deleteGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName);
                    });
        } else {
            commandClassScanner.getQueueCommandClassNames().forEach(
                    commandClassName -> {
                        discordCommandService.deployGuildCommand(server, commandClassName, updateCommandFailedCallbackFactory);
                        updatedCommandNames.add(commandClassName);
                    }
            );
        }
        // special case ForceDraw depends on allowDraw
        if (server.getQueues().stream().filter(queue -> queue.getGame().isAllowDraw()).toList().isEmpty()) {
            discordCommandService.deleteGuildCommand(server, ForceDraw.class.getSimpleName().toLowerCase(), updateCommandFailedCallbackFactory);
        } else {
            discordCommandService.deployGuildCommand(server, ForceDraw.class.getSimpleName().toLowerCase(), updateCommandFailedCallbackFactory);
        }
        updatedCommandNames.add(ForceDraw.class.getSimpleName().toLowerCase());
        return "/" + String.join(", /", updatedCommandNames);
    }
}
