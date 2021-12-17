package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
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
    private final Function<EventWrapper, Command> commandFactory;
    private final EloTrackingService service;
    private final DiscordBotService bot;
    private final TimedTaskQueue queue;
    private final long applicationId = 612578637836845057L;
    private final long entenwieseId = 612347065762054165L;
    private final Snowflake botSnowflake;

    public CommandParser(GatewayDiscordClient client, EloTrackingService service, DiscordBotService bot,
                         TimedTaskQueue queue, Function<EventWrapper, Command> commandFactory) {
        this.client = client;
        this.commandFactory = commandFactory;
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
            Command command = commandFactory.apply(new EventWrapper(event, service, bot, this.queue));
            command.execute();
            Mono returnValue = null;
            for (String reply : command.getBotReplies()) {
                returnValue = event.reply(reply);
            }
            return returnValue;
        }).subscribe();

        client.on(ReactionAddEvent.class)
                .filter(event -> !event.getUserId().equals(botSnowflake))
                .filter(event -> event.getEmoji().equals(Emojis.checkMark) || event.getEmoji().equals(Emojis.crossMark))
                .filter(event -> service.challengeExistsByChallengerMessageId(event.getMessageId().asLong()) // TODO optimieren?
                        || service.challengeExistsByAcceptorMessageId(event.getMessageId().asLong()))
                .map(event -> new EventWrapper(event, service, bot, queue))
                .map(this::setCommandStringForReactionAddEvent)
                .map(commandFactory::apply)
                .subscribe(Command::execute);
    }

    public EventWrapper setCommandStringForReactionAddEvent(EventWrapper wrapper) {
        String lastLine = ((ReactionAddEvent) wrapper.getEvent()).getMessage().block()
                .getContent().split("/n")[0];// TODO vllt langsam. vllt doch ueber event.getMessageId()?
        ReactionEmoji emoji = ((ReactionAddEvent) wrapper.getEvent()).getEmoji();

        if (lastLine.contains("Accept?") && emoji.equals(Emojis.checkMark)) wrapper.setCommandString("accept");

        if (lastLine.contains("won :arrow_up: or lost :arrow_down:") && emoji.equals(Emojis.arrowUp)) wrapper.setCommandString("win");
        if (lastLine.contains("won :arrow_up: or lost :arrow_down:") && emoji.equals(Emojis.arrowDown)) wrapper.setCommandString("lose");
        if (lastLine.contains("won :arrow_up: or lost :arrow_down:") && emoji.equals(Emojis.leftRightArrow)) wrapper.setCommandString("draw");
        if (lastLine.contains("won :arrow_up: or lost :arrow_down:") && emoji.equals(Emojis.crossMark)) wrapper.setCommandString("cancel");
        return wrapper;
    }
}
