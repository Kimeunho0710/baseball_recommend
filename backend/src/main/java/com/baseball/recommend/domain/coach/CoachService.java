package com.baseball.recommend.domain.coach;

import com.baseball.recommend.domain.recommend.RecommendResult;
import com.baseball.recommend.domain.recommend.RecommendRepository;
import com.baseball.recommend.domain.team.Team;
import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import com.baseball.recommend.infra.claude.ClaudeApiClient;
import com.baseball.recommend.infra.claude.GeminiClient;
import com.baseball.recommend.infra.claude.GroqClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoachService {

    private final RecommendRepository recommendRepository;
    private final GroqClient groqClient;
    private final ClaudeApiClient claudeApiClient;
    private final GeminiClient geminiClient;

    @Transactional(readOnly = true)
    public String chat(Long recommendId, String message, List<CoachRequest.ChatMessageDto> history) {
        RecommendResult result = recommendRepository.findById(recommendId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMEND_NOT_FOUND));

        Team team = result.getTeam();
        String systemPrompt = buildSystemPrompt(team, result);

        List<Map<String, String>> historyMaps = history.stream()
                .map(h -> Map.of("role", h.role(), "content", h.content()))
                .toList();

        // Groq(무료) 우선 → Claude → Gemini 순으로 fallback
        String reply = groqClient.chatReply(systemPrompt, historyMaps, message);
        if (reply != null) return reply;

        reply = claudeApiClient.chatReply(systemPrompt, historyMaps, message);
        if (reply != null) return reply;

        return geminiClient.chatReply(systemPrompt, historyMaps, message);
    }

    private String buildSystemPrompt(Team team, RecommendResult result) {
        return """
당신은 KBO 야구 입문자를 위한 친근하고 따뜻한 AI 코치입니다.

## 이 사용자 정보
- 추천 팀: %s
- 팬 성향 프로필: %s
- 팀 추천 이유: %s

## %s 팀 정보
- 연고지: %s, 홈구장: %s
- 팀 소개: %s
- 팀 특징: %s
- 입문 가이드: %s

## 답변 원칙
- 야구 입문자 눈높이에 맞춰 쉽고 친근하게 설명하세요
- 어려운 야구 용어는 괄호 안에 쉬운 말로 풀어주세요 (예: 직관(경기장 직접 관람))
- 3-4문장으로 간결하게 답변하세요
- 추천된 %s을 중심으로 답변하되, 리그 전반 질문도 도움을 드리세요
- 형식적이지 않고 친구처럼 편안한 말투로 대화하세요
""".formatted(
                team.getName(), result.getFanProfile(), result.getReason(),
                team.getName(), team.getCity(), team.getStadium(),
                team.getDescription(), team.getCharacteristics(), team.getBeginnerGuide(),
                team.getName()
        );
    }
}
