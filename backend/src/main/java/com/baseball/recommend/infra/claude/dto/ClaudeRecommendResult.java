package com.baseball.recommend.infra.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeRecommendResult {

    @JsonProperty("teamName")
    private String teamName;

    @JsonProperty("reason")
    private String reason;
}
