package com.baseball.recommend.domain.member.dto;

import com.baseball.recommend.domain.recommend.RecommendResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyRecommendResponse {
    private Long recommendId;
    private String teamName;
    private String teamColor;
    private String fanProfile;
    private LocalDateTime createdAt;

    public static MyRecommendResponse from(RecommendResult result) {
        return MyRecommendResponse.builder()
                .recommendId(result.getId())
                .teamName(result.getTeam().getName())
                .teamColor(result.getTeam().getPrimaryColor())
                .fanProfile(result.getFanProfile())
                .createdAt(result.getCreatedAt())
                .build();
    }
}
