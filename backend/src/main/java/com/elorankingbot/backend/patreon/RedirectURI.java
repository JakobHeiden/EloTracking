package com.elorankingbot.backend.patreon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectURI {

    @GetMapping("/patreon-redirect")
    public String patreonRedirect() {
        return "placeholder";
    }
}
