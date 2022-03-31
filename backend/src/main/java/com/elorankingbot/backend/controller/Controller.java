package com.elorankingbot.backend.controller;

import com.elorankingbot.backend.service.DBService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class Controller {

    private final DBService service;

    public Controller(DBService DBService) {
        this.service = DBService;
    }

    /*
    @GetMapping("rankings/{channelId}")
    public List<PlayerInRankingsDto> getRankings(@PathVariable long channelId) {
        return service.getRankingsAsDto(channelId);
    }

    @GetMapping("gamedata/{channelId}")
    public Game getGame(@PathVariable long channelId) {
        return service.findGameByGuildId(channelId).get();
    }

     */
}
