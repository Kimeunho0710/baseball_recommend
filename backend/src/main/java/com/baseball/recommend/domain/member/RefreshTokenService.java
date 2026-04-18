package com.baseball.recommend.domain.member;

import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    /**
     * 로그인/회원가입 시 Refresh Token 발급 (기존 토큰이 있으면 교체)
     */
    public String issue(Member member) {
        String newToken = generateOpaque();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000);

        RefreshToken rt = refreshTokenRepository.findByMember(member)
                .orElseGet(() -> RefreshToken.builder().member(member).build());
        rt.update(newToken, expiresAt);
        refreshTokenRepository.save(rt);
        return newToken;
    }

    /**
     * Access Token 재발급 시 Refresh Token Rotation
     * - 기존 토큰을 검증하고 새 토큰으로 교체
     * - 만료되었거나 존재하지 않으면 예외 발생
     */
    public RotateResult rotate(String oldToken) {
        RefreshToken rt = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = rt.getMember();
        String newToken = generateOpaque();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000);
        rt.update(newToken, expiresAt);

        return new RotateResult(member, newToken);
    }

    /**
     * 로그아웃 시 Refresh Token 폐기 (쿠키의 토큰 값으로 삭제)
     */
    public void revokeByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    /**
     * 회원 탈퇴 등 전체 세션 폐기
     */
    public void revokeByMember(Member member) {
        refreshTokenRepository.deleteByMember(member);
    }

    private String generateOpaque() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    public record RotateResult(Member member, String newToken) {}
}
