package com.baseball.recommend.domain.standing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandingRepository extends JpaRepository<Standing, Long> {

    List<Standing> findBySeasonOrderByRankAsc(int season);

    Optional<Standing> findBySeasonAndTeamName(int season, String teamName);
}
