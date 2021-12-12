package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.commandparser.CommandParser;
import de.neuefische.elotracking.backend.commandparser.EventWrapper;
import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.commands.SlashCommand;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
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
    private final TimedTaskQueue queue;
    private final Function<EventWrapper, SlashCommand> slashCommandFactory;
    private String adminId;
    private PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;

    public DiscordBotService(GatewayDiscordClient gatewayDiscordClient, EloTrackingService service, @Lazy CommandParser commandParser,
                             TimedTaskQueue queue, Function<EventWrapper, SlashCommand> slashCommandFactory) {
        this.client = gatewayDiscordClient;
        this.service = service;
        this.adminId = service.getPropertiesLoader().getAdminId();
        this.queue = queue;
        this.slashCommandFactory = slashCommandFactory;
        this.adminMentionAsString = String.format("<@%s>", adminId);

        Function<User, Boolean> isTestBotOrNotBot = user -> !user.isBot() ||
                        user.getId().asString().equals(service.getPropertiesLoader().getTestBotChallengerId()) ||
                        user.getId().asString().equals(service.getPropertiesLoader().getTestBotAcceptorId());
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(msgEvent -> msgEvent.getMessage())
                .filter(msg -> msg.getAuthor().map(isTestBotOrNotBot).orElse(false))
                .filter(commandParser::isCommand)
                .subscribe(commandParser::processCommand);

        ApplicationCommandRequest greetCmdRequest = ApplicationCommandRequest.builder()
                .name("challenge")
                .description("Challenge a player to a match")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("player").description("The player to challenge")
                        .type(ApplicationCommandOption.Type.USER.getValue()).required(true)
                        .build())
                .build();

        client.getRestClient().getApplicationService()
                .createGuildApplicationCommand(612578637836845057L, 612347065762054165L, greetCmdRequest)
                .subscribe();

        client.on(ChatInputInteractionEvent.class, event -> {
            log.warn(slashCommandFactory.toString());
            SlashCommand command = slashCommandFactory.apply(new EventWrapper(event, service, this, this.queue));
            log.warn(command.toString());
            command.execute();
            Mono returnValue = null;
            for (String reply : command.getBotReplies()) {
                returnValue = event.reply(reply);
            }
            return returnValue;
        }).subscribe();


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

    public void sendToChannel(String channelId, String text) {
        TextChannel channel = (TextChannel) client.getChannelById(Snowflake.of(channelId)).block();
        channel.createMessage(text).subscribe();
    }

    public String getPlayerName(String playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
