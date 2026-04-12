package com.baseball.recommend.domain.recommend;

import com.baseball.recommend.domain.recommend.dto.RecommendResponse;
import com.baseball.recommend.domain.recommend.dto.TeamRankItem;
import com.baseball.recommend.domain.survey.SurveyResult;
import com.baseball.recommend.domain.team.Team;
import com.baseball.recommend.domain.team.TeamService;
import com.baseball.recommend.infra.claude.ClaudeClient;
import com.baseball.recommend.infra.claude.dto.ClaudeRecommendResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecommendService {

    private final RecommendRepository recommendRepository;
    private final TeamService teamService;
    private final ClaudeClient claudeClient;
    private final ObjectMapper objectMapper;

    public RecommendResponse recommend(SurveyResult surveyResult) {
        ClaudeRecommendResult claudeResult = claudeClient.recommend(surveyResult.getAnswers());

        Team team = teamService.findByName(claudeResult.getTeamName());

        String top3Json = serializeTop3(claudeResult.getTop3());

        RecommendResult result = RecommendResult.builder()
                .surveyResultId(surveyResult.getId())
                .team(team)
                .reason(claudeResult.getReason())
                .top3Json(top3Json)
                .fanProfile(claudeResult.getFanProfile())
                .fanProfileDescription(claudeResult.getFanProfileDescription())
                .build();

        RecommendResult saved = recommendRepository.save(result);
        log.info("추천 결과 저장: surveyId={}, team={}, profile={}",
                surveyResult.getId(), team.getName(), claudeResult.getFanProfile());

        return RecommendResponse.from(saved, claudeResult.getTop3());
    }

    private String serializeTop3(List<TeamRankItem> top3) {
        if (top3 == null) return null;
        try {
            return objectMapper.writeValueAsString(top3);
        } catch (Exception e) {
            log.warn("top3 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
