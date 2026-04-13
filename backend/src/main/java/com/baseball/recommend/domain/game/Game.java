package com.baseball.recommend.domain.game;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "game",
    uniqueConstraints = @UniqueConstraint(columnNames = {"game_date", "away_team", "home_team"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_date", nullable = false)
    private String gameDate;   // "2026-04-12"

    @Column(name = "game_time")
    private String gameTime;   // "18:30" or null

    @Column(name = "away_team", nullable = false)
    private String awayTeam;

    @Column(name = "home_team", nullable = false)
    private String homeTeam;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column
    private String stadium;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Game(String gameDate, String gameTime, String awayTeam, String homeTeam,
                Integer awayScore, Integer homeScore, String stadium, boolean completed) {
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.awayTeam = awayTeam;
        this.homeTeam = homeTeam;
        this.awayScore = awayScore;
        this.homeScore = homeScore;
        this.stadium = stadium;
        this.completed = completed;
    }

    public void update(String gameTime, Integer awayScore, Integer homeScore,
                       String stadium, boolean completed) {
        this.gameTime = gameTime;
        this.awayScore = awayScore;
        this.homeScore = homeScore;
        this.stadium = stadium;
        this.completed = completed;
    }
}
