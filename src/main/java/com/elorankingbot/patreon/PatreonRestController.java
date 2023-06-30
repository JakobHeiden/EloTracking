package com.elorankingbot.patreon;

import com.elorankingbot.service.DiscordBotService;
import com.elorankingbot.service.Services;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Component
public class PatreonRestController {

    private final PatreonClient patreonClient;
    private final DiscordBotService bot;

    public PatreonRestController(Services services) {
        this.patreonClient = services.patreonClient;
        this.bot = services.bot;
    }

    @GetMapping("/patreon-redirect")
    public String patreonRedirect(@RequestParam String code, @RequestParam String state) {
        return patreonClient.processRedirect(code, state);
    }
}
