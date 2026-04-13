package com.baseball.recommend.domain.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByGameDate(String gameDate);

    List<Game> findByCompletedTrueAndGameDateBetweenOrderByGameDateDesc(String from, String to);

    List<Game> findByCompletedTrueAndAwayTeamOrCompletedTrueAndHomeTeamOrderByGameDateDesc(
            String awayTeam, String homeTeam);

    Optional<Game> findByGameDateAndAwayTeamAndHomeTeam(String gameDate, String awayTeam, String homeTeam);

    List<Game> findByGameDateIn(java.util.Collection<String> gameDates);
}
