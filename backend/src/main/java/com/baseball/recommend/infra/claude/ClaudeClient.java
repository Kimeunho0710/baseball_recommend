package com.baseball.recommend.infra.claude;

import com.baseball.recommend.domain.recommend.dto.TeamRankItem;
import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import com.baseball.recommend.infra.claude.dto.ClaudeRecommendResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final ObjectMapper objectMapper;

    public ClaudeRecommendResult recommend(String answersJson) {
        try {
            Map<String, String> answers = objectMapper.readValue(answersJson, new TypeReference<>() {});
            Map<String, Integer> scores = calculateScores(answers);

            String topTeamName = scores.entrySet().stream()
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .orElseThrow()
                    .getKey();

            String reason = buildReason(topTeamName, answers);
            List<TeamRankItem> top3 = buildTop3(scores, answers);
            String fanProfile = determineFanProfile(answers);
            String fanProfileDescription = buildProfileDescription(fanProfile);

            log.info("추천 결과: {} | 프로필: {} | 점수: {}", topTeamName, fanProfile, scores.get(topTeamName));

            return new ClaudeRecommendResult(topTeamName, reason, top3, fanProfile, fanProfileDescription);
        } catch (Exception e) {
            log.error("추천 로직 오류: {}", e.getMessage());
            throw new BusinessException(ErrorCode.CLAUDE_API_ERROR);
        }
    }

    // ─────────────────────────────────────────────
    // 점수 계산
    // ─────────────────────────────────────────────

    private Map<String, Integer> calculateScores(Map<String, String> answers) {
        Map<String, Integer> scores = new HashMap<>();
        for (String team : TEAM_NAMES) scores.put(team, 0);

        // q1: 야구장 관람 스타일 (A=열정응원, B=승부집착, C=분위기관람, D=전략분석)
        switch (answers.getOrDefault("q1", "")) {
            case "A" -> { add(scores, "LG 트윈스", 3);     add(scores, "한화 이글스", 3);   add(scores, "롯데 자이언츠", 1); }
            case "B" -> { add(scores, "KIA 타이거즈", 3);  add(scores, "삼성 라이온즈", 3); add(scores, "두산 베어스", 1); }
            case "C" -> { add(scores, "롯데 자이언츠", 3); add(scores, "KT 위즈", 3);       add(scores, "LG 트윈스", 1); }
            case "D" -> { add(scores, "SSG 랜더스", 3);    add(scores, "두산 베어스", 3);   add(scores, "NC 다이노스", 2); }
        }

        // q2: 9회말 2아웃 반응 (A=감성몰입, B=냉정전략, C=열정응원, D=소심집착)
        switch (answers.getOrDefault("q2", "")) {
            case "A" -> { add(scores, "한화 이글스", 3);   add(scores, "롯데 자이언츠", 3); add(scores, "KIA 타이거즈", 1); }
            case "B" -> { add(scores, "두산 베어스", 3);   add(scores, "SSG 랜더스", 2);    add(scores, "NC 다이노스", 2); }
            case "C" -> { add(scores, "LG 트윈스", 3);     add(scores, "KIA 타이거즈", 2);  add(scores, "SSG 랜더스", 1); }
            case "D" -> { add(scores, "KT 위즈", 3);       add(scores, "NC 다이노스", 2);   add(scores, "키움 히어로즈", 2); }
        }

        // q3: 직관 동행 (A=커뮤니티, B=입문자친화, C=독립, D=학습)
        switch (answers.getOrDefault("q3", "")) {
            case "A" -> { add(scores, "LG 트윈스", 3);     add(scores, "롯데 자이언츠", 3); add(scores, "한화 이글스", 1); }
            case "B" -> { add(scores, "KT 위즈", 3);       add(scores, "키움 히어로즈", 3); add(scores, "롯데 자이언츠", 1); }
            case "C" -> { add(scores, "삼성 라이온즈", 3); add(scores, "NC 다이노스", 3);   add(scores, "두산 베어스", 1); }
            case "D" -> { add(scores, "두산 베어스", 3);   add(scores, "SSG 랜더스", 3);    add(scores, "NC 다이노스", 1); }
        }

        // q4: 10연패 반응 (A=충성최상, B=현실수용, C=열혈비판, D=느슨한팬)
        switch (answers.getOrDefault("q4", "")) {
            case "A" -> { add(scores, "한화 이글스", 3);   add(scores, "롯데 자이언츠", 3); add(scores, "LG 트윈스", 1); }
            case "B" -> { add(scores, "삼성 라이온즈", 3); add(scores, "KIA 타이거즈", 2);  add(scores, "두산 베어스", 1); }
            case "C" -> { add(scores, "키움 히어로즈", 3); add(scores, "LG 트윈스", 3);     add(scores, "KIA 타이거즈", 1); }
            case "D" -> { add(scores, "KT 위즈", 3);       add(scores, "NC 다이노스", 2);   add(scores, "SSG 랜더스", 1); }
        }

        // q5: 주말 계획 (A=전통체계, B=균형, C=즉흥, D=자유)
        switch (answers.getOrDefault("q5", "")) {
            case "A" -> { add(scores, "삼성 라이온즈", 3); add(scores, "두산 베어스", 3);   add(scores, "NC 다이노스", 1); }
            case "B" -> { add(scores, "KIA 타이거즈", 3);  add(scores, "SSG 랜더스", 3);    add(scores, "KT 위즈", 1); }
            case "C" -> { add(scores, "롯데 자이언츠", 3); add(scores, "한화 이글스", 3);   add(scores, "LG 트윈스", 1); }
            case "D" -> { add(scores, "키움 히어로즈", 3); add(scores, "KT 위즈", 3);       add(scores, "SSG 랜더스", 1); }
        }

        // q6: 새로운 사람 만남 (A=외향주도, B=균형사교, C=내향집중, D=관찰후폭발)
        switch (answers.getOrDefault("q6", "")) {
            case "A" -> { add(scores, "LG 트윈스", 3);     add(scores, "한화 이글스", 3);   add(scores, "SSG 랜더스", 1); }
            case "B" -> { add(scores, "KIA 타이거즈", 3);  add(scores, "롯데 자이언츠", 3); add(scores, "KT 위즈", 1); }
            case "C" -> { add(scores, "삼성 라이온즈", 3); add(scores, "NC 다이노스", 3);   add(scores, "두산 베어스", 1); }
            case "D" -> { add(scores, "두산 베어스", 3);   add(scores, "키움 히어로즈", 3); add(scores, "LG 트윈스", 1); }
        }

        // q7: 게임/스포츠 스타일 (A=공격화력, B=투수중심, C=화려플레이, D=승리지상)
        switch (answers.getOrDefault("q7", "")) {
            case "A" -> { add(scores, "LG 트윈스", 3);     add(scores, "SSG 랜더스", 3);    add(scores, "한화 이글스", 2); }
            case "B" -> { add(scores, "삼성 라이온즈", 3); add(scores, "NC 다이노스", 3);   add(scores, "KT 위즈", 2); }
            case "C" -> { add(scores, "키움 히어로즈", 3); add(scores, "롯데 자이언츠", 3); add(scores, "KIA 타이거즈", 1); }
            case "D" -> { add(scores, "KIA 타이거즈", 3);  add(scores, "두산 베어스", 3);   add(scores, "SSG 랜더스", 1); }
        }

        // q8: 끌리는 이야기 (A=명문전통, B=언더독, C=스타중심, D=꾸준함)
        switch (answers.getOrDefault("q8", "")) {
            case "A" -> { add(scores, "KIA 타이거즈", 3);  add(scores, "삼성 라이온즈", 3); add(scores, "두산 베어스", 2); }
            case "B" -> { add(scores, "한화 이글스", 3);   add(scores, "키움 히어로즈", 3); add(scores, "KT 위즈", 2); }
            case "C" -> { add(scores, "롯데 자이언츠", 3); add(scores, "LG 트윈스", 3);     add(scores, "SSG 랜더스", 1); }
            case "D" -> { add(scores, "NC 다이노스", 3);   add(scores, "SSG 랜더스", 3);    add(scores, "KT 위즈", 1); }
        }

        return scores;
    }

    // ─────────────────────────────────────────────
    // Top 3 팀 빌드
    // ─────────────────────────────────────────────

    private List<TeamRankItem> buildTop3(Map<String, Integer> scores, Map<String, String> answers) {
        int maxScore = scores.values().stream().max(Integer::compareTo).orElse(1);

        List<Map.Entry<String, Integer>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        List<TeamRankItem> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            String teamName = sorted.get(i).getKey();
            int score = sorted.get(i).getValue();
            int percentage = (int) Math.round((double) score / maxScore * 100);
            String color = TEAM_COLORS.getOrDefault(teamName, "#e94560");
            String shortReason = buildShortReason(teamName, answers);
            result.add(new TeamRankItem(i + 1, teamName, color, score, percentage, shortReason));
        }
        return result;
    }

    // ─────────────────────────────────────────────
    // 팀별 짧은 매칭 이유
    // ─────────────────────────────────────────────

    private String buildShortReason(String teamName, Map<String, String> answers) {
        String q1 = answers.getOrDefault("q1", "");
        String q2 = answers.getOrDefault("q2", "");
        String q4 = answers.getOrDefault("q4", "");
        String q5 = answers.getOrDefault("q5", "");
        String q6 = answers.getOrDefault("q6", "");
        String q7 = answers.getOrDefault("q7", "");
        String q8 = answers.getOrDefault("q8", "");

        return switch (teamName) {
            case "KIA 타이거즈" ->
                (q8.equals("A") ? "명문 전통 선호" : "승리 지향") + " + " + (q7.equals("D") ? "승리 지상주의" : "진지한 승부욕");
            case "삼성 라이온즈" ->
                (q5.equals("A") ? "계획적 스타일" : "체계적 성향") + " + " + (q6.equals("C") ? "내향 집중형" : "안정 추구");
            case "LG 트윈스" ->
                (q7.equals("A") ? "공격 야구 선호" : "분위기 중시") + " + " + (q6.equals("A") ? "외향적 성격" : "트렌디한 감각");
            case "두산 베어스" ->
                (q5.equals("A") ? "체계적 사고" : "강한 승부욕") + " + " + (q7.equals("D") ? "승리 지상주의" : "끈질긴 투지");
            case "KT 위즈" ->
                (q5.equals("D") ? "자유로운 성향" : "혁신 지향") + " + " + (q2.equals("D") ? "냉철한 판단" : "성장 지향");
            case "SSG 랜더스" ->
                (q1.equals("D") ? "데이터 분석형" : "열정적 성향") + " + " + (q7.equals("A") ? "공격적 플레이 선호" : "체계적 성향");
            case "롯데 자이언츠" ->
                (q4.equals("A") ? "충성스러운 응원" : "감동 추구") + " + " + (q8.equals("C") ? "스타 중심 선호" : "진한 감성");
            case "한화 이글스" ->
                (q4.equals("A") ? "어떤 상황에서도 응원" : "역전 드라마 선호") + " + " + (q6.equals("A") ? "열정적 성격" : "우직한 감성");
            case "NC 다이노스" ->
                (q1.equals("D") ? "전략 분석형" : "스마트한 감각") + " + " + (q6.equals("C") ? "조용한 집중형" : "체계적 성향");
            case "키움 히어로즈" ->
                (q8.equals("B") ? "언더독 서사 선호" : "개성 있는 성향") + " + " + (q7.equals("C") ? "화려한 플레이 선호" : "자유로운 마인드");
            default -> "성향 분석 결과 높은 적합도";
        };
    }

    // ─────────────────────────────────────────────
    // 팬 성향 프로필
    // ─────────────────────────────────────────────

    private String determineFanProfile(Map<String, String> answers) {
        String q1 = answers.getOrDefault("q1", "");
        String q2 = answers.getOrDefault("q2", "");
        String q3 = answers.getOrDefault("q3", "");
        String q4 = answers.getOrDefault("q4", "");
        String q5 = answers.getOrDefault("q5", "");
        String q6 = answers.getOrDefault("q6", "");
        String q7 = answers.getOrDefault("q7", "");
        String q8 = answers.getOrDefault("q8", "");

        int emotional   = (q2.equals("A") ? 3 : 0) + (q4.equals("A") ? 2 : 0) + (q8.equals("C") ? 2 : 0) + (q6.equals("B") ? 1 : 0);
        int analytical  = (q1.equals("D") ? 3 : 0) + (q2.equals("B") ? 2 : 0) + (q3.equals("D") ? 2 : 0) + (q5.equals("A") ? 1 : 0);
        int enthusiast  = (q1.equals("A") ? 3 : 0) + (q2.equals("C") ? 2 : 0) + (q3.equals("A") ? 2 : 0) + (q4.equals("A") ? 1 : 0);
        int traditional = (q8.equals("A") ? 3 : 0) + (q4.equals("B") ? 2 : 0) + (q5.equals("A") ? 2 : 0) + (q6.equals("C") ? 1 : 0);
        int challenger  = (q8.equals("B") ? 3 : 0) + (q7.equals("A") ? 2 : 0) + (q6.equals("A") ? 2 : 0) + (q4.equals("C") ? 1 : 0);
        int calm        = (q1.equals("C") ? 3 : 0) + (q3.equals("C") ? 2 : 0) + (q6.equals("C") ? 2 : 0) + (q7.equals("B") ? 1 : 0);

        Map<String, Integer> profileScores = new LinkedHashMap<>();
        profileScores.put("감성 몰입형 팬", emotional);
        profileScores.put("전략 분석형 팬", analytical);
        profileScores.put("열정 응원형 팬", enthusiast);
        profileScores.put("전통 중시형 팬", traditional);
        profileScores.put("도전 선호형 팬", challenger);
        profileScores.put("차분 관조형 팬", calm);

        return profileScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("열정 응원형 팬");
    }

    private String buildProfileDescription(String profile) {
        return switch (profile) {
            case "감성 몰입형 팬" ->
                "드라마틱한 역전 장면에 가슴이 뛰고, 경기의 감동과 스토리에 깊이 빠져드는 타입이에요. 팬들과 함께 울고 웃으며 야구를 즐깁니다.";
            case "전략 분석형 팬" ->
                "경기의 흐름을 데이터와 논리로 읽어내는 타입이에요. 투수 교체 타이밍, 작전, 선수 기용까지 분석하며 보는 재미가 남다릅니다.";
            case "열정 응원형 팬" ->
                "현장 분위기와 함께 호흡하며 응원가를 외치고, 팬들과의 일체감을 즐기는 타입이에요. 야구장에 가는 것 자체가 이벤트입니다.";
            case "전통 중시형 팬" ->
                "야구의 본질인 완성도 높은 경기와 팀의 역사를 소중히 여기는 타입이에요. 묵묵히 팀을 응원하며 진정한 팬십을 보여줍니다.";
            case "도전 선호형 팬" ->
                "기존의 틀을 깨는 파격적인 플레이와 떠오르는 강자에 열광하는 타입이에요. 변화와 도전이 있는 팀에서 에너지를 얻습니다.";
            case "차분 관조형 팬" ->
                "조용히 집중하여 경기의 디테일을 즐기는 타입이에요. 화려함보다 탄탄한 수비와 치밀한 작전을 높이 평가합니다.";
            default -> "야구를 다양한 시각으로 즐기는 팬이에요.";
        };
    }

    // ─────────────────────────────────────────────
    // 1위 팀 상세 추천 이유
    // ─────────────────────────────────────────────

    private String buildReason(String teamName, Map<String, String> answers) {
        String q1 = answers.getOrDefault("q1", "");
        String q2 = answers.getOrDefault("q2", "");
        String q4 = answers.getOrDefault("q4", "");
        String q5 = answers.getOrDefault("q5", "");
        String q6 = answers.getOrDefault("q6", "");
        String q7 = answers.getOrDefault("q7", "");
        String q8 = answers.getOrDefault("q8", "");

        return switch (teamName) {
            case "KIA 타이거즈" -> String.format(
                "당신의 %s 성향과 %s 모습이 KIA 타이거즈의 DNA와 딱 맞아요! " +
                "최다 우승 팀의 뜨거운 팬덤 속에서 야구의 진정한 재미를 느낄 수 있을 거예요.",
                q6.equals("B") ? "균형 잡힌 사교적인" : "열정적인",
                q7.equals("D") ? "승리를 최우선으로 하는" : "강한 팀 정신을 가진"
            );
            case "삼성 라이온즈" -> String.format(
                "체계적이고 %s 당신에게는 삼성 라이온즈가 딱입니다! " +
                "철저한 시스템과 안정적인 운영을 추구하는 팀 스타일이 당신의 성향과 완벽히 맞아요.",
                q5.equals("A") ? "계획적인" : "논리적인"
            );
            case "LG 트윈스" -> String.format(
                "트렌디하고 %s 당신은 LG 트윈스와 잘 맞아요! " +
                "서울의 화려한 감성 속에서 세련된 야구 문화를 즐길 수 있을 거예요.",
                q7.equals("A") ? "공격적인 야구를 좋아하는" : "외향적이고 열정적인"
            );
            case "두산 베어스" -> String.format(
                "%s 당신에게 두산 베어스를 추천해요! " +
                "위기에 강하고 강인한 저력으로 끝까지 포기하지 않는 팀 정신이 당신과 닮았어요.",
                q7.equals("D") ? "승리를 향한 집념이 강한" : "뚝심 있고 체계적인"
            );
            case "KT 위즈" ->
                "자유롭고 혁신을 즐기는 당신에게 KT 위즈가 잘 맞아요! " +
                "젊고 역동적인 구단 문화 속에서 함께 성장하는 재미를 느낄 수 있을 거예요.";
            case "SSG 랜더스" -> String.format(
                "%s 당신에게 SSG 랜더스를 추천해요! " +
                "공격적인 야구와 스타 플레이어들이 만들어내는 짜릿한 순간들이 기다리고 있어요.",
                q1.equals("D") ? "데이터로 야구를 즐기는 분석적인" : "화끈하고 열정적인"
            );
            case "롯데 자이언츠" -> String.format(
                "%s 당신은 롯데 자이언츠와 함께하면 야구의 진한 감동을 느낄 수 있어요! " +
                "부산 갈매기와 함께하는 사직구장의 뜨거운 응원은 잊을 수 없는 경험이 될 거예요.",
                q4.equals("A") ? "어떤 상황에서도 응원하는 열정적인" : "감성적이고 드라마를 좋아하는"
            );
            case "한화 이글스" -> String.format(
                "끈기 있고 %s 당신에게 한화 이글스가 잘 맞아요! " +
                "팬과의 깊은 유대감과 감동적인 응원 문화로 야구를 진심으로 즐길 수 있을 거예요.",
                q2.equals("A") ? "감성적인" : q4.equals("A") ? "충성스러운" : "우직한"
            );
            case "NC 다이노스" -> String.format(
                "스마트하고 %s 당신에게 NC 다이노스를 추천해요! " +
                "데이터 기반의 체계적인 야구를 분석하며 보는 즐거움이 남다를 거예요.",
                q1.equals("D") ? "분석적인" : "논리적인"
            );
            case "키움 히어로즈" -> String.format(
                "개성 넘치고 %s 당신에게 키움 히어로즈가 딱이에요! " +
                "젊고 화끈한 타격과 저비용 고효율의 도전적인 구단 스타일이 당신의 에너지와 잘 어울려요.",
                q8.equals("B") ? "언더독의 도전을 응원하는" : "자유롭고 도전적인"
            );
            default -> teamName + "은(는) 당신의 성향과 잘 맞는 팀입니다. 함께 응원해보세요!";
        };
    }

    // ─────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────

    private void add(Map<String, Integer> scores, String team, int value) {
        scores.merge(team, value, Integer::sum);
    }

    private static final String[] TEAM_NAMES = {
        "KIA 타이거즈", "삼성 라이온즈", "LG 트윈스", "두산 베어스", "KT 위즈",
        "SSG 랜더스", "롯데 자이언츠", "한화 이글스", "NC 다이노스", "키움 히어로즈"
    };

    private static final Map<String, String> TEAM_COLORS = Map.of(
        "KIA 타이거즈",  "#EA0029",
        "삼성 라이온즈", "#074CA1",
        "LG 트윈스",    "#C30452",
        "두산 베어스",   "#4a4a8a",
        "KT 위즈",      "#aaaaaa",
        "SSG 랜더스",   "#CE0E2D",
        "롯데 자이언츠", "#4a6fa5",
        "한화 이글스",   "#FF6600",
        "NC 다이노스",   "#2a5ca8",
        "키움 히어로즈", "#820024"
    );
}
