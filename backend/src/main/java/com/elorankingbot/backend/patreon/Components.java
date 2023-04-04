package com.elorankingbot.backend.patreon;

import com.elorankingbot.backend.commands.player.Patreon;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.service.Services;
import discord4j.core.spec.EmbedCreateSpec;
import org.springframework.stereotype.Component;

public class Components {

    public static EmbedCreateSpec begForPatreonEmbed(long patreonCommandId) {
        return EmbedCreateSpec.builder()
                .description(String.format("Please consider supporting the developer.\nUse </%s:%s> for details.",
                        Patreon.class.getSimpleName().toLowerCase(), patreonCommandId))
                .build();
    }
}
