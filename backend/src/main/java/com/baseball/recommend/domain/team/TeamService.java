package com.baseball.recommend.domain.team;

import com.baseball.recommend.domain.team.dto.TeamResponse;
import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(TeamResponse::from)
                .toList();
    }

    public TeamResponse getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        return TeamResponse.from(team);
    }

    public Team findByName(String name) {
        return teamRepository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }
}
