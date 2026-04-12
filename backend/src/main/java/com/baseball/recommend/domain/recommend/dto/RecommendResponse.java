package com.baseball.recommend.domain.recommend.dto;

import com.baseball.recommend.domain.recommend.RecommendResult;
import com.baseball.recommend.domain.team.Team;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecommendResponse {

    private Long recommendId;
    private Long surveyResultId;
    private String teamName;
    private String teamCity;
    private String teamStadium;
    private String teamDescription;
    private String teamCharacteristics;
    private String primaryColor;
    private String logoUrl;
    private String reason;
    private List<TeamRankItem> top3;
    private String fanProfile;
    private String fanProfileDescription;

    public static RecommendResponse from(RecommendResult result, List<TeamRankItem> top3) {
        Team team = result.getTeam();
        return RecommendResponse.builder()
                .recommendId(result.getId())
                .surveyResultId(result.getSurveyResultId())
                .teamName(team.getName())
                .teamCity(team.getCity())
                .teamStadium(team.getStadium())
                .teamDescription(team.getDescription())
                .teamCharacteristics(team.getCharacteristics())
                .primaryColor(team.getPrimaryColor())
                .logoUrl(team.getLogoUrl())
                .reason(result.getReason())
                .top3(top3)
                .fanProfile(result.getFanProfile())
                .fanProfileDescription(result.getFanProfileDescription())
                .build();
    }
}
