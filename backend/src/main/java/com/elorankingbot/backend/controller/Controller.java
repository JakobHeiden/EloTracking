package com.elorankingbot.backend.controller;

import com.elorankingbot.backend.dto.PlayerInRankingsDto;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DBService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class Controller {

    private final DBService service;

    public Controller(DBService DBService) {
        this.service = DBService;
    }

    @GetMapping("rankings/{channelId}")
    public List<PlayerInRankingsDto> getRankings(@PathVariable long channelId) {
        return service.getRankingsAsDto(channelId);
    }

    @GetMapping("gamedata/{channelId}")
    public Game getGame(@PathVariable long channelId) {
        return service.findGameByGuildId(channelId).get();
    }
}
