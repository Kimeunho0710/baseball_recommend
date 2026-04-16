package com.baseball.recommend.domain.member;

import com.baseball.recommend.domain.member.dto.AuthResponse;
import com.baseball.recommend.domain.member.dto.LoginRequest;
import com.baseball.recommend.domain.member.dto.MyRecommendResponse;
import com.baseball.recommend.domain.member.dto.SignupRequest;
import com.baseball.recommend.domain.recommend.RecommendRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final RecommendRepository recommendRepository;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(memberService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(AuthResponse.of(null, member));
    }

    @GetMapping("/me/recommendations")
    public ResponseEntity<List<MyRecommendResponse>> myRecommendations(@AuthenticationPrincipal Member member) {
        List<MyRecommendResponse> list = recommendRepository
                .findByMemberOrderByCreatedAtDesc(member)
                .stream()
                .map(MyRecommendResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }
}
