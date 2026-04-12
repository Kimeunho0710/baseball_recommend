package com.baseball.recommend.domain.standing;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "standing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int season;

    @Column(name = "team_rank", nullable = false)
    private int rank;

    @Column(nullable = false)
    private String teamName;

    @Column(nullable = false)
    private int games;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int draws;

    @Column(nullable = false)
    private double winRate;

    @Column
    private String gamesBehind;

    @Column
    private String streak;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Standing(int season, int rank, String teamName, int games,
                    int wins, int losses, int draws, double winRate,
                    String gamesBehind, String streak) {
        this.season = season;
        this.rank = rank;
        this.teamName = teamName;
        this.games = games;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.winRate = winRate;
        this.gamesBehind = gamesBehind;
        this.streak = streak;
    }

    public void update(int rank, int games, int wins, int losses, int draws,
                       double winRate, String gamesBehind, String streak) {
        this.rank = rank;
        this.games = games;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.winRate = winRate;
        this.gamesBehind = gamesBehind;
        this.streak = streak;
    }
}
