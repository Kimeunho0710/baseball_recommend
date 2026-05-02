package com.baseball.recommend.infra.claude;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GeminiClient(ObjectMapper objectMapper, @Value("${gemini.api.key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    /**
     * @return Map with keys: reason, shortReasons (Map), fanProfileDescription — or null on failure
     */
    public Map<String, Object> generateText(Map<String, String> answers, String topTeam,
                                             String fanProfile, List<String> top3Names) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key 미설정, rule-based fallback 사용");
            return null;
        }
        try {
            String prompt = buildPrompt(answers, topTeam, fanProfile, top3Names);

            List<Map<String, Object>> parts = List.of(Map.of("text", prompt));
            List<Map<String, Object>> contents = List.of(Map.of("parts", parts));

            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 800);

            Map<String, Object> body = new HashMap<>();
            body.put("contents", contents);
            body.put("generationConfig", genConfig);

            String raw = restClient.post()
                    .uri(GEMINI_URL + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().strip();

            if (text.startsWith("```")) {
                text = text.replaceFirst("```json\\s*|```\\s*", "")
                           .replaceAll("```\\s*$", "")
                           .strip();
            }

            Map<String, Object> result = objectMapper.readValue(text, new TypeReference<>() {});
            log.info("Gemini 텍스트 생성 성공: {}", topTeam);
            return result;

        } catch (Exception e) {
            log.error("Gemini API 실패, fallback 사용: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(Map<String, String> answers, String topTeam,
                                String fanProfile, List<String> top3Names) {
        Map<String, String[]> qMeta = new HashMap<>();
        qMeta.put("q1", new String[]{"야구장 관람 스타일",
                "열정적으로 응원가를 부르며 응원", "승패에 집중하며 분석",
                "분위기 즐기며 여유롭게 관람", "데이터와 작전을 분석하며 관람"});
        qMeta.put("q2", new String[]{"9회말 2아웃 상황 반응",
                "가슴 두근거리며 무조건 응원", "냉정하게 확률 계산",
                "목청껏 응원가 외치기", "손 떨리며 상황 분석"});
        qMeta.put("q3", new String[]{"직관 동행자 선택",
                "팬 커뮤니티 사람들과 단체 관람", "야구 처음인 친구 데려오기",
                "혼자 집중해서 보기", "야구 잘 아는 사람에게 배우며 보기"});
        qMeta.put("q4", new String[]{"팀 10연패 시 반응",
                "그래도 내 팀이니까 끝까지 응원", "이번 시즌은 글렀구나 수용",
                "SNS에 답답함 폭발", "개별 경기 자체를 즐김"});
        qMeta.put("q5", new String[]{"주말 계획 스타일",
                "일정표 미리 짜두고 계획대로", "대략적인 계획만 세우고 조율",
                "기분에 따라 즉흥적으로", "아무 계획 없이 자유롭게"});
        qMeta.put("q6", new String[]{"새로운 사람 만남 방식",
                "먼저 말 걸고 주도적으로 친해지기", "적당히 어울리며 자연스럽게",
                "조용히 관찰하다 친해지면 말 걸기", "처음엔 조용하다 친해지면 에너지 폭발"});
        qMeta.put("q7", new String[]{"좋아하는 야구 플레이",
                "타자들의 화끈한 홈런 퍼레이드", "투수들의 긴장감 넘치는 투구 싸움",
                "빠른 발과 화려한 수비 플레이", "무조건 이기는 것이 최고"});
        qMeta.put("q8", new String[]{"끌리는 팀 스토리",
                "오랜 역사와 전통의 명문 팀", "약체에서 강팀으로 성장하는 도전기",
                "스타 플레이어 중심의 화려한 야구", "꾸준하고 안정적으로 성과를 내는 팀"});

        Map<String, Integer> answerIdx = Map.of("A", 1, "B", 2, "C", 3, "D", 4);

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 KBO 야구 팀 추천 전문가입니다. 야구 입문자의 설문 결과를 분석하여 팀 추천 이유를 작성해주세요.\n\n");
        sb.append("## 설문 답변\n");

        for (int i = 1; i <= 8; i++) {
            String q = "q" + i;
            String[] meta = qMeta.get(q);
            String ans = answers.getOrDefault(q, "");
            int idx = answerIdx.getOrDefault(ans, 0);
            String ansText = (idx > 0) ? meta[idx] : ans;
            sb.append(String.format("- %s: %s\n", meta[0], ansText));
        }

        sb.append("\n## 분석 결과\n");
        sb.append("- 1위 추천 팀: ").append(topTeam).append("\n");
        sb.append("- 상위 3개 팀: ").append(String.join(", ", top3Names)).append("\n");
        sb.append("- 팬 성향 프로필: ").append(fanProfile).append("\n");

        sb.append("\n## 요청\n");
        sb.append("아래 JSON 형식으로만 응답해주세요. 다른 텍스트는 포함하지 마세요.\n");
        sb.append("{\n");
        sb.append("  \"reason\": \"1위 팀 ").append(topTeam)
          .append("을(를) 추천하는 이유 (설문 답변을 구체적으로 언급하며 친근하고 따뜻한 말투로 2-3문장)\",\n");
        sb.append("  \"shortReasons\": {\n");
        for (int i = 0; i < top3Names.size(); i++) {
            String comma = (i < top3Names.size() - 1) ? "," : "";
            sb.append("    \"").append(top3Names.get(i))
              .append("\": \"이 팀과 잘 맞는 핵심 이유 한 줄\"").append(comma).append("\n");
        }
        sb.append("  },\n");
        sb.append("  \"fanProfileDescription\": \"").append(fanProfile)
          .append("에 해당하는 팬의 특성 설명 (이 사용자의 답변을 반영하여 1-2문장)\"\n");
        sb.append("}");

        return sb.toString();
    }
}
