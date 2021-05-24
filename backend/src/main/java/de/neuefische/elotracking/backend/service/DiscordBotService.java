package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.parser.CommandParser;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DiscordBotService {
    private final GatewayDiscordClient client;
    private final EloTrackingService service;
    private final PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;

    @Autowired
    public DiscordBotService(GatewayDiscordClient gatewayDiscordClient,
                             EloTrackingService eloTrackingService,
                             CommandParser commandParser) {
        this.client = gatewayDiscordClient;
        this.service = eloTrackingService;

        String adminId = service.getConfig().getProperty("ADMIN_DISCORD_ID");
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
                .filter(commandParser::isCommand)
                .subscribe(commandParser::processCommand);

        client.getEventDispatcher().on(Event.class)
                .subscribe(logs -> log.trace(logs.toString()));
    }

    public void sendToAdmin(String text) {
        adminDm.createMessage(text).subscribe();
    }

    public String getPlayerName(String playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
