package com.baseball.recommend.domain.recommend;

import com.baseball.recommend.domain.team.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommend_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long surveyResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String top3Json;

    @Column(length = 50)
    private String fanProfile;

    @Column(columnDefinition = "TEXT")
    private String fanProfileDescription;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public RecommendResult(Long surveyResultId, Team team, String reason,
                           String top3Json, String fanProfile, String fanProfileDescription) {
        this.surveyResultId = surveyResultId;
        this.team = team;
        this.reason = reason;
        this.top3Json = top3Json;
        this.fanProfile = fanProfile;
        this.fanProfileDescription = fanProfileDescription;
    }
}
