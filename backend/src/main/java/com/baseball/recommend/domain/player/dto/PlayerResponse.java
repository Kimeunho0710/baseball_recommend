package com.baseball.recommend.domain.player.dto;

import com.baseball.recommend.domain.player.Player;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerResponse {

    private Long id;
    private String name;
    private String position;
    private Integer jerseyNumber;
    private String description;

    public static PlayerResponse from(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .name(player.getName())
                .position(player.getPosition())
                .jerseyNumber(player.getJerseyNumber())
                .description(player.getDescription())
                .build();
    }
}
