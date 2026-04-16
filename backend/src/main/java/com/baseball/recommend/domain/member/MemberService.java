package com.baseball.recommend.domain.member;

import com.baseball.recommend.domain.member.dto.AuthResponse;
import com.baseball.recommend.domain.member.dto.LoginRequest;
import com.baseball.recommend.domain.member.dto.SignupRequest;
import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import com.baseball.recommend.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();
        memberRepository.save(member);
        String token = jwtUtil.generateToken(member.getEmail());
        return AuthResponse.of(token, member);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        String token = jwtUtil.generateToken(member.getEmail());
        return AuthResponse.of(token, member);
    }
}
