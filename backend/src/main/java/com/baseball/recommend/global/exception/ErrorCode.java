package com.baseball.recommend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Team
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "팀을 찾을 수 없습니다."),

    // Survey
    SURVEY_NOT_FOUND(HttpStatus.NOT_FOUND, "설문 결과를 찾을 수 없습니다."),
    INVALID_SURVEY_ANSWER(HttpStatus.BAD_REQUEST, "유효하지 않은 설문 답변입니다."),

    // Claude
    CLAUDE_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 추천 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
