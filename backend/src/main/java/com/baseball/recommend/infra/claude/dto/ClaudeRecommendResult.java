package com.baseball.recommend.infra.claude.dto;

import com.baseball.recommend.domain.recommend.dto.TeamRankItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeRecommendResult {

    private String teamName;
    private String reason;
    private List<TeamRankItem> top3;
    private String fanProfile;
    private String fanProfileDescription;
}
