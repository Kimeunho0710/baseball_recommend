package com.baseball.recommend.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByMember(Member member);

    @Transactional
    @Modifying
    void deleteByMember(Member member);

    @Transactional
    @Modifying
    void deleteByToken(String token);
}
