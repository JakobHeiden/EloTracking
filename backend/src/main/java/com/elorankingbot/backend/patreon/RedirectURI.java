package com.elorankingbot.backend.patreon;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CommonsLog
@Component
public class RedirectURI {

    private final DiscordBotService bot;

    public RedirectURI(Services services) {
        this.bot = services.bot;
    }

    @GetMapping("/patreon-redirect")
    public String patreonRedirect(@RequestParam String code) {
        bot.sendToOwner("REDIRECT " + code);
        log.warn("REDIRECT " + code);
        bot.sendToOwner(prototype.doStuff(code, bot));
        return "placeholder";
    }
}
