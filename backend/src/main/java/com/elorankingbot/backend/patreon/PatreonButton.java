package com.elorankingbot.backend.patreon;

import com.elorankingbot.backend.components.Emojis;
import discord4j.core.object.component.Button;

import java.util.UUID;

public class PatreonButton {

    public static Button link(String state) {
        String url = "https://www.patreon.com/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=9dkSNb9DssBnmdlFGv9oiVnEUHTAt5qohN3eT6EvZ4-PFSRMkcOx2dYMNzriLjr4" +
                "&redirect_uri=http://45.77.53.94:8080/patreon-redirect" +
                "&state=" + state;
        //return Button.link(url, Emojis.win, "Win");
        return Button.link(url, "Win");
    }
}
