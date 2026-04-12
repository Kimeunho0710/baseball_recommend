package com.baseball.recommend.domain.survey.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class SurveyRequest {

    @NotEmpty(message = "설문 답변은 필수입니다.")
    private Map<String, String> answers; // {"q1":"A","q2":"B",...}
}
