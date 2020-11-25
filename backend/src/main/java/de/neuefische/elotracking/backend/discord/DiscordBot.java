package de.neuefische.elotracking.backend.discord;

import de.neuefische.elotracking.backend.model.Dummy;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

@Component
public class DiscordBot {
    private final GatewayDiscordClient client;
    private PrivateChannel adminDm;

    @Autowired
    public DiscordBot(GatewayDiscordClient gatewayDiscordClient, EloTrackingService service) {
        this.client = gatewayDiscordClient;

        //ping the mongodb. TODO remove later on
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(message -> message.getContent().startsWith("!"))
                .flatMap(message -> {
                    Dummy serviceResponse = service.ping(message.getContent());
                    return message.getChannel()
                            .flatMap(channel -> channel.createMessage(serviceResponse.toString()));
                })
                .subscribe();

        client.getEventDispatcher().on(Event.class)
                .subscribe(Logger::trace);

        User admin = client.getUserById(Snowflake.of(service.getConfig()
                .getProperty("ADMIN_DISCORD_ID"))).block();
        adminDm = admin.getPrivateChannel().block();
        Logger.info("Private channel to admin established");
        sendToAdmin("I am logged in and ready");
    }

    public void sendToAdmin(String text) {
        adminDm.createMessage(text).subscribe();
    }
}
