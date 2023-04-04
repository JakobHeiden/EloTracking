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

    private final PatreonClient patreonClient;

    public RedirectURI(PatreonClient patreonClient) {
        this.patreonClient = patreonClient;
    }

    @GetMapping("/patreon-redirect")
    public String patreonRedirect(@RequestParam String code) {
        System.out.println(code);
        patreonClient.doStuff(code);
        return "placeholder";
    }



}
