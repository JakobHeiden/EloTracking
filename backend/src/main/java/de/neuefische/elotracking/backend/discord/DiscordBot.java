package de.neuefische.elotracking.backend.discord;

import de.neuefische.elotracking.backend.model.Dummy;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class DiscordBot {
    private final GatewayDiscordClient gatewayDiscordClient;

    @Autowired
    public DiscordBot(GatewayDiscordClient gatewayDiscordClient, @Lazy EloTrackingService eloTrackingService) {
        this.gatewayDiscordClient = gatewayDiscordClient;

        //ping the mongodb. TODO remove later on
        gatewayDiscordClient.getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(message -> message.getContent().startsWith("!"))
                .flatMap(message -> {
                    Dummy serviceResponse = eloTrackingService.ping(message.getContent());
                    return message.getChannel()
                            .flatMap(channel -> channel.createMessage(serviceResponse.toString()));
                })
                .subscribe();
    }
}
