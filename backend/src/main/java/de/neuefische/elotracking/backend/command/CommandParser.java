package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.*;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@Component
public class CommandParser {

    private final GatewayDiscordClient client;
    private final Function<ApplicationCommandInteractionEventWrapper, Command> slashCommandFactory;
    private final Function<ReactionAddEventWrapper, EmojiCommand> emojiCommandFactory;
    private final EloTrackingService service;
    private final DiscordBotService bot;
    private final TimedTaskQueue queue;
    private final long applicationId = 612578637836845057L;
    private final long entenwieseId = 612347065762054165L;
    private final Snowflake botSnowflake;

    public CommandParser(GatewayDiscordClient client, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
                         Function<ApplicationCommandInteractionEventWrapper, Command> slashCommandFactory,
                         Function<ReactionAddEventWrapper, EmojiCommand> emojiCommandFactory) {
        this.client = client;
        this.slashCommandFactory = slashCommandFactory;
        this.emojiCommandFactory = emojiCommandFactory;
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
        applicationService.createGuildApplicationCommand(applicationId, entenwieseId, challengeCmdRequest).subscribe();
        applicationService.createGuildApplicationCommand(applicationId, entenwieseId, challengeUserCommandRequest).subscribe();

        client.on(ApplicationCommandInteractionEvent.class, event -> {
            Command command = slashCommandFactory.apply(new ApplicationCommandInteractionEventWrapper(event, service, bot, queue));
            command.execute();
            Mono returnValue = null;
            for (String reply : command.getBotReplies()) {
                returnValue = event.reply(reply);
            }
            return returnValue;
        }).subscribe();

        client.on(ReactionAddEvent.class)
                .filter(event -> !event.getUserId().equals(botSnowflake))
                .filter(event -> event.getMessage().block()// TODO langsam. vllt message ids cachen?
                        .getAuthor().get().getId().equals(botSnowflake))
                .map(event -> new ReactionAddEventWrapper(event, service, bot, queue))
                .map(emojiCommandFactory::apply)
                .filter(command -> command != null)
                .subscribe(EmojiCommand::execute);
    }

    public static Command createSlashCommand(ApplicationCommandInteractionEventWrapper eventWrapper) {
        String commandClassName = eventWrapper.event().getCommandName();
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (Command) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
                    .getConstructor(Event.class, EloTrackingService.class, DiscordBotService.class, TimedTaskQueue.class)
                    .newInstance(eventWrapper.event(), eventWrapper.service(), eventWrapper.bot(), eventWrapper.queue());
        } catch (Exception e) {
            eventWrapper.bot().sendToAdmin(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static EmojiCommand createEmojiCommand(ReactionAddEventWrapper wrapper) {
        ReactionAddEvent event = wrapper.event();
        EloTrackingService service = wrapper.service();
        DiscordBotService bot = wrapper.bot();
        TimedTaskQueue queue = wrapper.queue();
        String boldLine = event.getMessage().block()// TODO vllt langsam. vllt doch ueber event.getMessageId()?
                .getContent().split("\\*\\*")[1];    // entweder ich gehe ueber den content (langsam) oder ich schaue in die db (auch langsam)
        ReactionEmoji emoji = wrapper.event().getEmoji();

        if (wrapper.service().challengeExistsByChallengerMessageId(event.getMessageId().asLong())
                || wrapper.service().challengeExistsByAcceptorMessageId(event.getMessageId().asLong())) {
            if (boldLine.contains("Accept?")) {
                if (emoji.equals(Emojis.checkMark)) return new Accept(event, service, bot, queue);
                if (emoji.equals(Emojis.crossMark)) return null;//TODO reject
            }
            if (boldLine.contains("won :arrow_up: or lost :arrow_down:")) {
                if (emoji.equals(Emojis.arrowUp)) return new Win(event, service, bot, queue);
                if (emoji.equals(Emojis.arrowDown)) return new Lose(event, service, bot, queue);
                if (emoji.equals(Emojis.leftRightArrow)) return null;
                if (emoji.equals(Emojis.crossMark)) return null;
            }
        }

        return null;
    }
}
