package com.baseball.recommend.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {

    private String gameDate;   // "2026-04-12"
    private String gameTime;   // "18:30" (예정 경기) or null
    private String awayTeam;
    private String homeTeam;
    private Integer awayScore; // null = 미완료
    private Integer homeScore;
    private String stadium;
    private boolean completed; // 종료 여부
}
