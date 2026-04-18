package com.baseball.recommend.domain.member;

import com.baseball.recommend.domain.member.dto.AuthResponse;
import com.baseball.recommend.domain.member.dto.LoginRequest;
import com.baseball.recommend.domain.member.dto.MyRecommendResponse;
import com.baseball.recommend.domain.member.dto.SignupRequest;
import com.baseball.recommend.domain.recommend.RecommendRepository;
import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import com.baseball.recommend.global.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final RecommendRepository recommendRepository;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        Member member = memberService.signup(request);
        String accessToken = jwtUtil.generateAccessToken(member.getEmail());
        String refreshToken = refreshTokenService.issue(member);
        setRefreshCookie(servletResponse, refreshToken, isSecure(servletRequest));
        return ResponseEntity.ok(AuthResponse.of(accessToken, member));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        Member member = memberService.login(request);
        String accessToken = jwtUtil.generateAccessToken(member.getEmail());
        String refreshToken = refreshTokenService.issue(member);
        setRefreshCookie(servletResponse, refreshToken, isSecure(servletRequest));
        return ResponseEntity.ok(AuthResponse.of(accessToken, member));
    }

    /**
     * Access Token 재발급
     * - HttpOnly 쿠키의 Refresh Token을 검증하고 새 Access Token 발급
     * - Refresh Token Rotation: 매 요청마다 새 Refresh Token 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String oldRefreshToken = extractRefreshCookie(servletRequest);
        if (oldRefreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenService.RotateResult result = refreshTokenService.rotate(oldRefreshToken);
        Member member = result.member();

        String newAccessToken = jwtUtil.generateAccessToken(member.getEmail());
        setRefreshCookie(servletResponse, result.newToken(), isSecure(servletRequest));

        return ResponseEntity.ok(AuthResponse.of(newAccessToken, member));
    }

    /**
     * 로그아웃
     * - DB에서 Refresh Token 삭제 + 쿠키 만료 처리
     * - Access Token이 만료되어도 로그아웃 가능 (쿠키 기반 식별)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String refreshToken = extractRefreshCookie(servletRequest);
        if (refreshToken != null) {
            try {
                refreshTokenService.revokeByToken(refreshToken);
            } catch (Exception ignored) {
                // 이미 없는 토큰이면 무시 (쿠키만 정리)
            }
        }
        clearRefreshCookie(servletResponse, isSecure(servletRequest));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(AuthResponse.of(null, member));
    }

    @GetMapping("/me/recommendations")
    public ResponseEntity<List<MyRecommendResponse>> myRecommendations(
            @AuthenticationPrincipal Member member) {
        List<MyRecommendResponse> list = recommendRepository
                .findByMemberOrderByCreatedAtDesc(member)
                .stream()
                .map(MyRecommendResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    // ── 쿠키 유틸 ────────────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String token, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 운영(Railway): X-Forwarded-Proto: https → secure=true, SameSite=None
     * 로컬(Vite 프록시): HTTP → secure=false, SameSite=Lax
     */
    private boolean isSecure(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto) || request.isSecure();
    }
}
