package com.baseball.recommend.domain.player;

import com.baseball.recommend.domain.team.Team;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String position;

    @Column
    private Integer jerseyNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    public Player(Team team, String name, String position, Integer jerseyNumber, String description) {
        this.team = team;
        this.name = name;
        this.position = position;
        this.jerseyNumber = jerseyNumber;
        this.description = description;
    }
}
