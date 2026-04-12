package com.baseball.recommend.domain.recommend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendRepository extends JpaRepository<RecommendResult, Long> {
    Optional<RecommendResult> findBySurveyResultId(Long surveyResultId);
}
