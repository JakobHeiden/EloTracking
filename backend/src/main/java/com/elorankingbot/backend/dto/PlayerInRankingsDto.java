package com.elorankingbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerInRankingsDto implements Comparable<PlayerInRankingsDto> {
    private String name;
    private double rating;

    @Override
    public int compareTo(PlayerInRankingsDto otherPlayer) {
        return (int) (1000 * (otherPlayer.getRating() - rating));
    }
}
