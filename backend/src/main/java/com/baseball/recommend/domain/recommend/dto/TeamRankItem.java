package com.baseball.recommend.domain.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeamRankItem {
    private int rank;
    private String teamName;
    private String primaryColor;
    private int score;
    private int percentage;
    private String shortReason;
}
