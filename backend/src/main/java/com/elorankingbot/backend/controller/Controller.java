package com.elorankingbot.backend.controller;

import com.elorankingbot.backend.dto.PlayerInRankingsDto;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.EloRankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class Controller {

    private final EloRankingService service;

    public Controller(EloRankingService eloRankingService) {
        this.service = eloRankingService;
    }

    @GetMapping("rankings/{channelId}")
    public List<PlayerInRankingsDto> getRankings(@PathVariable long channelId) {
        return service.getRankings(channelId);
    }

    @GetMapping("gamedata/{channelId}")
    public Game getGame(@PathVariable long channelId) {
        return service.findGameByGuildId(channelId).get();
    }
}
