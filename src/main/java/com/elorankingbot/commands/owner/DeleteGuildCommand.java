package com.elorankingbot.commands.owner;

import com.elorankingbot.command.annotations.OwnerCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@OwnerCommand
public class DeleteGuildCommand extends SlashCommand {

    private final ApplicationService applicationService;

    public DeleteGuildCommand(ChatInputInteractionEvent event, Services services) {
        super(event, services);
        applicationService = services.client.rest().getApplicationService();
    }

    public static ApplicationCommandRequest getRequest(Server server) {
        return ApplicationCommandRequest.builder()
                .name(DeleteGuildCommand.class.getSimpleName().toLowerCase())
                .description("owner only")
                /*
                .addOption(ApplicationCommandOptionData.builder()
                        .name("game-or-queue")
                        .description("game or queue")
                        .type(STRING.getValue())
                        .addChoice(ApplicationCommandOptionChoiceData.builder().name("game").value("game").build())
                        .addChoice(ApplicationCommandOptionChoiceData.builder().name("queue").value("queue").build())
                        .required(true)
                        .build())
                 */
                .addOption(ApplicationCommandOptionData.builder()
                        .name("command-name")
                        .description("command name")
                        .type(STRING.getValue())
                        .required(true)
                        .build())
                .defaultPermission(true)
                .build();
    }

    protected void execute() throws Exception {
        event.reply("ok").subscribe();
        String commandName = event.getOption("command-name").get().getValue().get().asString();
        List<Server> allServers = dbService.findAllServers().stream().toList();
        allServers.forEach(server1 -> {
            applicationService.getGuildApplicationCommands(bot.getBotId(), server1.getGuildId())
                    .filter(applicationCommandData -> applicationCommandData.name().equals(commandName))
                    .subscribe(applicationCommandData -> {
                        applicationService.deleteGuildApplicationCommand(bot.getBotId(), server1.getGuildId(),
                                applicationCommandData.id().asLong()).block();
                        event.createFollowup(String.format("deleted %s on %s", commandName, server1.getGuildId())).subscribe();
                    });
        });
    }
}
