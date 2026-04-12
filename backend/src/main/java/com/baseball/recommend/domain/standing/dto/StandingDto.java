package com.baseball.recommend.domain.standing.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StandingDto {

    private int rank;
    private String teamName;
    private int games;
    private int wins;
    private int losses;
    private int draws;
    private double winRate;
    private String gamesBehind;
    private String streak;
}
