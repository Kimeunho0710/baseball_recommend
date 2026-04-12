package com.baseball.recommend.domain.team;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "team")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String stadium;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String characteristics;

    @Column(nullable = false)
    private String primaryColor;

    @Column
    private String logoUrl;

    @Column
    private Integer foundedYear;

    @Column
    private Integer championships;

    @Column
    private String mascot;

    @Column
    private String manager;

    @Column(columnDefinition = "TEXT")
    private String beginnerGuide;

    @Builder
    public Team(String name, String city, String stadium, String description,
                String characteristics, String primaryColor, String logoUrl,
                Integer foundedYear, Integer championships, String mascot, String manager,
                String beginnerGuide) {
        this.name = name;
        this.city = city;
        this.stadium = stadium;
        this.description = description;
        this.characteristics = characteristics;
        this.primaryColor = primaryColor;
        this.logoUrl = logoUrl;
        this.foundedYear = foundedYear;
        this.championships = championships;
        this.mascot = mascot;
        this.manager = manager;
        this.beginnerGuide = beginnerGuide;
    }

    public void update(String city, String stadium, String description, String characteristics,
                       String primaryColor, Integer foundedYear, Integer championships,
                       String mascot, String manager, String beginnerGuide) {
        this.city = city;
        this.stadium = stadium;
        this.description = description;
        this.characteristics = characteristics;
        this.primaryColor = primaryColor;
        this.foundedYear = foundedYear;
        this.championships = championships;
        this.mascot = mascot;
        this.manager = manager;
        this.beginnerGuide = beginnerGuide;
    }
}
