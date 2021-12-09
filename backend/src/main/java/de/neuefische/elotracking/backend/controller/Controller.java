package de.neuefische.elotracking.backend.controller;

import de.neuefische.elotracking.backend.dto.PlayerInRankingsDto;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class Controller {

    private final EloTrackingService service;

    public Controller(EloTrackingService eloTrackingService) {
        this.service = eloTrackingService;
    }

    @GetMapping("rankings/{channelId}")
    public List<PlayerInRankingsDto> getRankings(@PathVariable String channelId) {
        return service.getRankings(channelId);
    }

    @GetMapping("gamedata/{channelId}")
    public Game getGame(@PathVariable String channelId) {
        return service.findGameByChannelId(channelId).get();
    }
}
