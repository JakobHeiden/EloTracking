package de.neuefische.elotracking.backend.discord;

import de.neuefische.elotracking.backend.command.*;
import de.neuefische.elotracking.backend.common.ApplicationPropertiesLoader;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class DiscordBot {
    private final GatewayDiscordClient client;
    private final EloTrackingService service;
    private final Function<Message, Command> commandFactory;
    private final PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;
    private final ApplicationPropertiesLoader config;

    @Autowired
    public DiscordBot(GatewayDiscordClient gatewayDiscordClient,
                      EloTrackingService eloTrackingService,
                      ApplicationPropertiesLoader applicationPropertiesLoader,
                      Function<Message, Command> commandFactory) {
        this.client = gatewayDiscordClient;
        this.service = eloTrackingService;
        this.config = applicationPropertiesLoader;
        this.commandFactory = commandFactory;

        String adminId = config.getProperty("ADMIN_DISCORD_ID");
        this.adminMentionAsString = String.format("<@%s>", adminId);
        User admin = client.getUserById(Snowflake.of(adminId)).block();
        this.adminDm = admin.getPrivateChannel().block();
        log.info("Private channel to admin established");
        sendToAdmin("I am logged in and ready");

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(msgEvent -> {
                    log.trace(msgEvent.toString());
                    log.debug("Incoming message: " + msgEvent.getMessage().getContent());
                    return msgEvent.getMessage();
                })
                .filter(msg -> msg.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(this::isCommand)
                .subscribe(this::parseCommand);

        client.getEventDispatcher().on(Event.class)
                .subscribe(logs -> log.trace(logs.toString()));
    }

    public void sendToAdmin(String text) {
        adminDm.createMessage(text).subscribe();
    }

    public boolean isCommand(Message msg) {
        String necessaryPrefix;
        Optional<Game> game = service.findGameByChannelId(msg.getChannelId().asString());
        if (game.isPresent()) {
            necessaryPrefix = game.get().getCommandPrefix();
        } else {
            necessaryPrefix = config.getProperty("DEFAULT_COMMAND_PREFIX");
        }
        if (msg.getContent().startsWith(necessaryPrefix)) return true;
        else return false;
    }

    private void parseCommand(Message msg) {
        log.debug("Parsing command: " + msg.getContent());
        Mono<MessageChannel> channelMono = msg.getChannel();
        Command command = commandFactory.apply(msg);
        command.execute();
        MessageChannel channel = channelMono.block();
        //command.getBotReplies().forEach(channel::createMessage); TODO
        for (String reply : command.getBotReplies()) {
            channel.createMessage(reply).subscribe();
        }
    }

    public String getPlayerName(String playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
