package com.baseball.recommend.domain.survey.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuestionDto {

    private String id;
    private String question;
    private String hint;
    private List<OptionDto> options;

    @Getter
    @Builder
    public static class OptionDto {
        private String value;
        private String text;
    }
}
