package de.neuefische.elotracking.backend.dto;

import de.neuefische.elotracking.backend.model.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerLeaderboardDto implements Comparable<PlayerLeaderboardDto> {
    private String name;
    private double rating;

    @Override
    public int compareTo(PlayerLeaderboardDto otherPlayer) {
        return (int) (otherPlayer.getRating() - this.rating);
    }
}
