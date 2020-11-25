package de.neuefische.elotracking.backend.discord;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

@Component
public class DiscordBot {
    private final GatewayDiscordClient client;
    private final EloTrackingService service;
    private final PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;
    @Getter
    private final String prefix;

    @Autowired
    public DiscordBot(GatewayDiscordClient gatewayDiscordClient, EloTrackingService eloTrackingService) {
        this.client = gatewayDiscordClient;
        this.service = eloTrackingService;
        this.prefix = service.getConfig().getProperty("COMMAND_PREFIX");

        String adminId = service.getConfig().getProperty("ADMIN_DISCORD_ID");
        this.adminMentionAsString = String.format("<@%s>", adminId);
        User admin = client.getUserById(Snowflake.of(adminId)).block();
        this.adminDm = admin.getPrivateChannel().block();
        Logger.info("Private channel to admin established");
        sendToAdmin("I am logged in and ready");

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(message -> message.getContent().startsWith(prefix))
                .subscribe(this::parseCommand);

        client.getEventDispatcher().on(Event.class)
                .subscribe(Logger::trace);
    }

    public void sendToAdmin(String text) {
        adminDm.createMessage(text).subscribe();
    }

    private void parseCommand(Message msg) {
        String[] parts = msg.getContent().substring(prefix.length()).split(" ");
        MessageChannel channel = msg.getChannel().block();
        switch(parts[0]) {
            case "register":
                register(msg, parts, channel);
                break;
                default:
                    channel.createMessage("Unknown command " + parts[0]).subscribe();
        }
    }

    private void register(Message msg, String[] parts, MessageChannel channel) {
        if (parts.length < 2) {
            channel.createMessage(String.format("Usage: %sregister <name of your game>", prefix)).subscribe();
            return;
        }

        String name = msg.getContent().substring(10);
        String replyFromService = service.register(channel.getId().asString(), name);
        channel.createMessage(replyFromService).subscribe();
    }
}
