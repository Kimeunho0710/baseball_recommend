package com.baseball.recommend.domain.survey;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "survey_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answers; // JSON string: {"q1":"A","q2":"B",...}

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public SurveyResult(String answers) {
        this.answers = answers;
    }
}
