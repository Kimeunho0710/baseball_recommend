package com.baseball.recommend.domain.survey;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<SurveyResult, Long> {
}
