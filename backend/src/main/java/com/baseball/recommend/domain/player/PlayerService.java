package com.baseball.recommend.domain.player;

import com.baseball.recommend.domain.player.dto.PlayerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;

    public List<PlayerResponse> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamIdOrderByJerseyNumber(teamId)
                .stream()
                .map(PlayerResponse::from)
                .toList();
    }
}
