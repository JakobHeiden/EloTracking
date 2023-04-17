package com.elorankingbot.backend.patreon;

import com.elorankingbot.backend.commands.Patreon;
import discord4j.core.spec.EmbedCreateSpec;

public class Components {

    public static EmbedCreateSpec begForPatreonEmbed(long patreonCommandId) {
        return EmbedCreateSpec.builder()
                .description(String.format("Please consider supporting the developer.\nUse </%s:%s> for details.",
                        Patreon.class.getSimpleName().toLowerCase(), patreonCommandId))
                .build();
    }
}
