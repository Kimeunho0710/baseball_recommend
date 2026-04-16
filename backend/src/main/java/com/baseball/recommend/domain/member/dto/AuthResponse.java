package com.baseball.recommend.domain.member.dto;

import com.baseball.recommend.domain.member.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String token;
    private Long memberId;
    private String email;
    private String nickname;

    public static AuthResponse of(String token, Member member) {
        return AuthResponse.builder()
                .token(token)
                .memberId(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .build();
    }
}
