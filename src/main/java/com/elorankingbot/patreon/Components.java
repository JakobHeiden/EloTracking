package com.elorankingbot.patreon;

import com.elorankingbot.commands.Patreon;
import com.elorankingbot.logging.ExceptionHandler;
import discord4j.core.spec.EmbedCreateSpec;

public class Components {

    private static final String sourceUrl = "https://github.com/JakobHeiden/EloTracking";

    public static EmbedCreateSpec begForPatreonEmbed(long patreonCommandId) {
        return EmbedCreateSpec.builder()
                .description(String.format("Please consider supporting the developer.\nUse </%s:%s> for details." +
                                "\nThe bot is now open source. Find the code here: %s" +
                                "\nIf you would like to contribute, please go to the support server: %s",
                        Patreon.class.getSimpleName().toLowerCase(), patreonCommandId, sourceUrl, ExceptionHandler.supportServerInvite))
                .build();
    }
}
