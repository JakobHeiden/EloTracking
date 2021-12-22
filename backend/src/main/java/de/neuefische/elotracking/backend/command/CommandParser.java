package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.ApplicationCommandInteractionCommand;
import de.neuefische.elotracking.backend.commands.ButtonInteractionCommand;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
public class CommandParser {

    private final GatewayDiscordClient client;
    private final Function<ApplicationCommandInteractionEventWrapper, ApplicationCommandInteractionCommand> slashCommandFactory;
    private final Function<ButtonInteractionEventWrapper, ButtonInteractionCommand> buttonInteractionCommandFactory;
    private final EloTrackingService service;
    private final DiscordBotService bot;
    private final TimedTaskQueue queue;
    private final long entenwieseId = 612347065762054165L;
    private final Snowflake botSnowflake;

    public CommandParser(GatewayDiscordClient client, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
                         Function<ApplicationCommandInteractionEventWrapper, ApplicationCommandInteractionCommand> slashCommandFactory,
                         Function<ButtonInteractionEventWrapper, ButtonInteractionCommand> buttonInteractionCommandFactory) {
        this.client = client;
        this.slashCommandFactory = slashCommandFactory;
        this.buttonInteractionCommandFactory = buttonInteractionCommandFactory;
        this.service = service;
        this.bot = bot;
        this.queue = queue;
        this.botSnowflake = client.getSelfId();

        ApplicationCommandRequest challengeCommandRequest = ApplicationCommandRequest.builder()
                .name("challenge")
                .description("Challenge a player to a match")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("player").description("The player to challenge")
                        .type(ApplicationCommandOption.Type.USER.getValue()).required(true)
                        .build())
                .build();

        ApplicationCommandRequest challengeUserCommandRequest = ApplicationCommandRequest.builder()
                .type(2)
                .name("challenge")
                .build();

        ApplicationCommandRequest setupCommandRequest = ApplicationCommandRequest.builder()
                .name("setup")
                .description("Get started with the bot")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name").description("The name of the game you want to track elo rating for")
                        .type(3).required(true).build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("logchannel").description("Should I create a channel where all resolved matches are displayed?")
                        .type(5).required(true).build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("modchannel").description("Should I create a channel where all disputes are displayed?")
                        .type(5).required(true).build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("allowdraw").description("Allow draw results and not just win or lose?")
                        .type(5).required(true).build())
                .build();

        ApplicationCommandRequest createResultChannelCommandRequest = ApplicationCommandRequest.builder()
                .name("createresultchannel")
                .description("Create a channel to show logs of all matches played on the guild")
                .build();

        ApplicationCommandRequest createDisputeChannelCommandRequest = ApplicationCommandRequest.builder()
                .name("createdisputechannel")
                .description("Create a channel to show disputes that arise from conflicting match reports")
                .build();

        if (service.getPropertiesLoader().isDeployGuildCommands()) {
            log.info("Deploying guild commands...");
            ApplicationService applicationService = client.getRestClient().getApplicationService();

            List<ApplicationCommandData> guildApplicationCommands = applicationService.getGuildApplicationCommands(botSnowflake.asLong(), entenwieseId)
                    .collectList().block();
            for (ApplicationCommandData guildApplicationCommand : guildApplicationCommands) {
                applicationService.deleteGuildApplicationCommand(
                        botSnowflake.asLong(), entenwieseId,
                        Long.parseLong(guildApplicationCommand.id())).block();
            }

            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, setupCommandRequest).subscribe();
            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, challengeCommandRequest).subscribe();
            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, challengeUserCommandRequest).subscribe();
            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, createResultChannelCommandRequest).subscribe();
            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, createDisputeChannelCommandRequest).subscribe();
        }

        client.on(ApplicationCommandInteractionEvent.class)
                .map(event -> new ApplicationCommandInteractionEventWrapper(event, service, bot, queue, client))
                .map(slashCommandFactory::apply)
                .subscribe(ApplicationCommandInteractionCommand::execute);

        client.on(ButtonInteractionEvent.class)
                .map(event -> new ButtonInteractionEventWrapper(event, service, bot, queue, client))
                .map(buttonInteractionCommandFactory::apply)
                .subscribe(ButtonInteractionCommand::execute);
    }

    public static ApplicationCommandInteractionCommand createSlashCommand(ApplicationCommandInteractionEventWrapper wrapper) {
        String commandClassName = wrapper.event().getCommandName();
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (ApplicationCommandInteractionCommand) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
                    .getConstructor(ApplicationCommandInteractionEvent.class, EloTrackingService.class,
                            DiscordBotService.class, TimedTaskQueue.class, GatewayDiscordClient.class)
                    .newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
        } catch (Exception e) {
            wrapper.bot().sendToAdmin(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static ButtonInteractionCommand createButtonInteractionCommand(ButtonInteractionEventWrapper wrapper) {
        String commandClassName = wrapper.event().getCustomId().split(":")[0];
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (ButtonInteractionCommand) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
                    .getConstructor(ButtonInteractionEvent.class, EloTrackingService.class, DiscordBotService.class,
                            TimedTaskQueue.class, GatewayDiscordClient.class)
                    .newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
        } catch (Exception e) {
            wrapper.bot().sendToAdmin(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
