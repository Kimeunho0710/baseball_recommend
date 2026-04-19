package com.baseball.recommend.domain.recommend;

import com.baseball.recommend.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecommendRepository extends JpaRepository<RecommendResult, Long> {
    Optional<RecommendResult> findBySurveyResultId(Long surveyResultId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"team"})
    List<RecommendResult> findByMemberOrderByCreatedAtDesc(Member member);

    @Query("SELECT r.team.name, r.team.primaryColor, COUNT(r) FROM RecommendResult r GROUP BY r.team.name, r.team.primaryColor ORDER BY COUNT(r) DESC")
    List<Object[]> countByTeam();
}
