package de.neuefische.elotracking.backend.controller;

import de.neuefische.elotracking.backend.dto.PlayerLeaderboardDto;
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

    @GetMapping("{channelId}")
    public List<PlayerLeaderboardDto> getRankings(@PathVariable String channelId) {
        return service.getRankings(channelId);
    }
}
