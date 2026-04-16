package com.baseball.recommend.domain.recommend;

import com.baseball.recommend.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendRepository extends JpaRepository<RecommendResult, Long> {
    Optional<RecommendResult> findBySurveyResultId(Long surveyResultId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"team"})
    List<RecommendResult> findByMemberOrderByCreatedAtDesc(Member member);
}
