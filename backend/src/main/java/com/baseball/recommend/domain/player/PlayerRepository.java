package com.baseball.recommend.domain.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamIdOrderByJerseyNumber(Long teamId);

    boolean existsByTeamId(Long teamId);
}
