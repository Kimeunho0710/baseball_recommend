package com.baseball.recommend.domain.recommend;

import com.baseball.recommend.domain.recommend.dto.RecommendResponse;
import com.baseball.recommend.domain.survey.SurveyResult;
import com.baseball.recommend.domain.team.Team;
import com.baseball.recommend.domain.team.TeamService;
import com.baseball.recommend.infra.claude.ClaudeClient;
import com.baseball.recommend.infra.claude.dto.ClaudeRecommendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendService {

    private final RecommendRepository recommendRepository;
    private final TeamService teamService;
    private final ClaudeClient claudeClient;

    public RecommendResponse recommend(SurveyResult surveyResult) {
        ClaudeRecommendResult claudeResult = claudeClient.recommend(surveyResult.getAnswers());

        Team team = teamService.findByName(claudeResult.getTeamName());

        RecommendResult result = RecommendResult.builder()
                .surveyResultId(surveyResult.getId())
                .team(team)
                .reason(claudeResult.getReason())
                .build();

        RecommendResult saved = recommendRepository.save(result);
        log.info("추천 결과 저장: surveyId={}, team={}", surveyResult.getId(), team.getName());

        return RecommendResponse.from(saved);
    }
}
