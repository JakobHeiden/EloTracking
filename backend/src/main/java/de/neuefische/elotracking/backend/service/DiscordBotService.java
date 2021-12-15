package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.commandparser.CommandParser;
import de.neuefische.elotracking.backend.commandparser.EventWrapper;
import de.neuefische.elotracking.backend.commandparser.PreProcessor;
import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.function.Function;

@Slf4j
@Component
public class DiscordBotService {

    private final GatewayDiscordClient client;
    private final EloTrackingService service;
    private final PreProcessor preProcessor;
    private final TimedTaskQueue queue;
    private final Function<EventWrapper, Command> slashCommandFactory;
    private long adminId;// TODO kann weg?
    private PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;
    private final long applicationId = 612578637836845057L;
    private final long entenwieseId = 612347065762054165L;
    private final Snowflake botSnowflake;
    public final ReactionEmoji checkMark = ReactionEmoji.codepoints("U+2705");
    public final ReactionEmoji crossMark = ReactionEmoji.codepoints("U+274E");

    public DiscordBotService(GatewayDiscordClient gatewayDiscordClient, EloTrackingService service, @Lazy CommandParser commandParser,
                             PreProcessor preProcessor, TimedTaskQueue queue, Function<EventWrapper, Command> slashCommandFactory) {
        this.client = gatewayDiscordClient;
        this.service = service;
        this.adminId = Long.valueOf(service.getPropertiesLoader().getAdminId());
        this.preProcessor = preProcessor;
        this.queue = queue;
        this.slashCommandFactory = slashCommandFactory;
        this.adminMentionAsString = String.format("<@%s>", adminId);
        this.botSnowflake = client.getSelfId();


        ApplicationCommandRequest challengeCmdRequest = ApplicationCommandRequest.builder()
                .name("challenge")
                .description("Challenge a player to a match")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("player").description("The player to challenge")
                        .type(ApplicationCommandOption.Type.USER.getValue()).required(true)
                        .build())
                .build();
        ApplicationCommandRequest acceptCommandRequest = ApplicationCommandRequest.builder()
                .name("accept").description("Accept an open challenge by a player")
                .addOption(ApplicationCommandOptionData.builder()
                        .addChoice(ApplicationCommandOptionChoiceData.builder().name("choice").value("player 1").build())
                        .addChoice(ApplicationCommandOptionChoiceData.builder().name("choice").value("player 2").build())
                        .name("player").description("The player whose challenge to accept")
                        .type(ApplicationCommandOption.Type.STRING.getValue()).required(true)
                        .build())
                .build();

        ApplicationCommandRequest challengeUserCommandRequest = ApplicationCommandRequest.builder()
                .type(2)
                .name("challenge")
                .build();

        ApplicationService applicationService = client.getRestClient().getApplicationService();
        applicationService.createGuildApplicationCommand(applicationId, entenwieseId, challengeCmdRequest).subscribe();
        applicationService.createGuildApplicationCommand(applicationId, entenwieseId, acceptCommandRequest).subscribe();
        applicationService.createGuildApplicationCommand(applicationId, entenwieseId, challengeUserCommandRequest).subscribe();

        client.on(ApplicationCommandInteractionEvent.class, event -> {
            log.warn(slashCommandFactory.toString());
            Command command = slashCommandFactory.apply(new EventWrapper(event, service, this, this.queue));
            log.warn(command.toString());
            command.execute();
            Mono returnValue = null;
            for (String reply : command.getBotReplies()) {
                returnValue = event.reply(reply);
            }
            return returnValue;
        }).subscribe();

        client.on(ReactionAddEvent.class)
                .filter(event -> !event.getUserId().equals(botSnowflake))
                .filter(event -> event.getEmoji().equals(checkMark) || event.getEmoji().equals(crossMark))
                .subscribe(preProcessor::preProcessReactionEvent);

        Function<User, Boolean> isTestBotOrNotBot = user -> !user.isBot() ||
                user.getId().asString().equals(service.getPropertiesLoader().getTestBotChallengerId()) ||
                user.getId().asString().equals(service.getPropertiesLoader().getTestBotAcceptorId());
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(msgEvent -> msgEvent.getMessage())
                .filter(msg -> msg.getAuthor().map(isTestBotOrNotBot).orElse(false))
                .filter(commandParser::isCommand)
                .subscribe(commandParser::processCommand);

    }

    @PostConstruct
    public void initAdminDm() {
        User admin = client.getUserById(Snowflake.of(adminId)).block();
        this.adminDm = admin.getPrivateChannel().block();
        log.info("Private channel to admin established");
        sendToAdmin("I am logged in and ready");
    }

    public void sendToAdmin(String text) {
        adminDm.createMessage(text).subscribe();
    }

    public void sendToChannel(long channelId, String text) {
        TextChannel channel = (TextChannel) client.getChannelById(Snowflake.of(channelId)).block();
        channel.createMessage(text).subscribe();
    }

    public Mono<Message> sendToUser(long userId, String text) {
        return (Mono<Message>) client.getUserById(Snowflake.of(userId)).block()
                .getPrivateChannel().block()
                .createMessage(text);
    }

    public String getPlayerName(long playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
