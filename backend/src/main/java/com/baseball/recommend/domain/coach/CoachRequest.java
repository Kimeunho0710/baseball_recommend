package com.baseball.recommend.domain.coach;

import java.util.List;

public record CoachRequest(
        Long recommendId,
        String message,
        List<ChatMessageDto> history
) {
    public record ChatMessageDto(String role, String content) {}
}
