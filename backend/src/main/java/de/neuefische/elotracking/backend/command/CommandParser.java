package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.*;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

        ApplicationCommandRequest challengeCmdRequest = ApplicationCommandRequest.builder()
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

        ApplicationService applicationService = client.getRestClient().getApplicationService();
        System.out.println("global");
        applicationService.getGlobalApplicationCommands(botSnowflake.asLong())
                        .subscribe(applicationCommandData -> System.out.println(applicationCommandData.id()));
        System.out.println("guild");
        applicationService.getGuildApplicationCommands(botSnowflake.asLong(), entenwieseId)
                .subscribe(applicationCommandData -> System.out.println(applicationCommandData.id()));
        //applicationService.deleteGuildApplicationCommand(applicationId, entenwieseId, 919196138941349898L).subscribe();
        //applicationService.deleteGuildApplicationCommand(applicationId, entenwieseId, 919396992906579999L).subscribe();
        //applicationService.deleteGuildApplicationCommand(applicationId, entenwieseId, 920374163997327391L).subscribe();
        //applicationService.deleteGuildApplicationCommand(applicationId, entenwieseId, 920376289536385134L).subscribe();
        //applicationService.deleteGuildApplicationCommand(applicationId, entenwieseId, 920376617514201128L).subscribe();
        //applicationService.deleteGuildApplicationCommand(applicationId, entenwieseId, 920381930388717639L).subscribe();
        //applicationService.deleteGlobalApplicationCommand(applicationId, 920324803393646632L).subscribe();
        //applicationService.deleteGlobalApplicationCommand(applicationId, 920324803859193897L).subscribe();
        //applicationService.deleteGlobalApplicationCommand(applicationId, 921890863565664347L).subscribe();
        //applicationService.createGlobalApplicationCommand(applicationId, challengeCmdRequest).subscribe();
        //applicationService.createGlobalApplicationCommand(applicationId, challengeUserCommandRequest).subscribe();

        client.on(ApplicationCommandInteractionEvent.class)
                .map(event -> new ApplicationCommandInteractionEventWrapper(event, service, bot, queue))
                .map(slashCommandFactory::apply)
                .subscribe(ApplicationCommandInteractionCommand::execute);

        client.on(ButtonInteractionEvent.class)
                .map(event -> new ButtonInteractionEventWrapper(event, service, bot, queue))
                .map(buttonInteractionCommandFactory::apply)
                .subscribe(ButtonInteractionCommand::execute);

    }

    public static ApplicationCommandInteractionCommand createSlashCommand(ApplicationCommandInteractionEventWrapper wrapper) {
        String commandClassName = wrapper.event().getCommandName();
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (ApplicationCommandInteractionCommand) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
                    .getConstructor(ApplicationCommandInteractionEvent.class, EloTrackingService.class, DiscordBotService.class, TimedTaskQueue.class)
                    .newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue());
        } catch (Exception e) {
            wrapper.bot().sendToAdmin(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static ButtonInteractionCommand createButtonInteractionCommand(ButtonInteractionEventWrapper wrapper) {
        ButtonInteractionEvent event = wrapper.event();
        EloTrackingService service = wrapper.service();
        DiscordBotService bot = wrapper.bot();
        TimedTaskQueue queue = wrapper.queue();

        String commandClassName = event.getCustomId().split(":")[0];
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (ButtonInteractionCommand) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
                    .getConstructor(ButtonInteractionEvent.class, EloTrackingService.class, DiscordBotService.class, TimedTaskQueue.class)
                    .newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue());
        } catch (Exception e) {
            wrapper.bot().sendToAdmin(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
