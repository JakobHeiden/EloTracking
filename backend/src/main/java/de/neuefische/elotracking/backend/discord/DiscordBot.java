package de.neuefische.elotracking.backend.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DiscordBot {
    private final GatewayDiscordClient gatewayDiscordClient;

    @Autowired
    public DiscordBot(GatewayDiscordClient gatewayDiscordClient) {

        this.gatewayDiscordClient = gatewayDiscordClient;
        System.out.println("discrodbot§");
    }

    //hier Funktionen



}
