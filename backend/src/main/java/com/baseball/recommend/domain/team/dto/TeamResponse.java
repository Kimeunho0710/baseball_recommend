package com.baseball.recommend.domain.team.dto;

import com.baseball.recommend.domain.team.Team;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamResponse {

    private Long id;
    private String name;
    private String city;
    private String stadium;
    private String description;
    private String characteristics;
    private String primaryColor;
    private String logoUrl;
    private Integer foundedYear;
    private Integer championships;
    private String mascot;
    private String manager;
    private String beginnerGuide;

    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .city(team.getCity())
                .stadium(team.getStadium())
                .description(team.getDescription())
                .characteristics(team.getCharacteristics())
                .primaryColor(team.getPrimaryColor())
                .logoUrl(team.getLogoUrl())
                .foundedYear(team.getFoundedYear())
                .championships(team.getChampionships())
                .mascot(team.getMascot())
                .manager(team.getManager())
                .beginnerGuide(team.getBeginnerGuide())
                .build();
    }
}
