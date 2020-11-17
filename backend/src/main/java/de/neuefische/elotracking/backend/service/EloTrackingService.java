package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EloTrackingService {
    private final DiscordBot discordBot;

    @Autowired
    public EloTrackingService(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }
}
