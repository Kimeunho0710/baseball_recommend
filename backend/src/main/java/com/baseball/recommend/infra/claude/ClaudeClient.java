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

        // q1: 주말 활동 (A=외향, B=내향)
        if ("A".equals(answers.get("q1"))) {
            add(scores, "KIA 타이거즈", 2); add(scores, "LG 트윈스", 2);
            add(scores, "KT 위즈", 2);      add(scores, "SSG 랜더스", 2);
            add(scores, "롯데 자이언츠", 2); add(scores, "키움 히어로즈", 2);
        } else {
            add(scores, "삼성 라이온즈", 2); add(scores, "두산 베어스", 2);
            add(scores, "NC 다이노스", 2);   add(scores, "한화 이글스", 1);
        }

        // q2: 의사결정 (A=논리, B=감성)
        if ("A".equals(answers.get("q2"))) {
            add(scores, "삼성 라이온즈", 2); add(scores, "NC 다이노스", 3);
            add(scores, "두산 베어스", 1);   add(scores, "KT 위즈", 1);
        } else {
            add(scores, "KIA 타이거즈", 2);  add(scores, "롯데 자이언츠", 2);
            add(scores, "한화 이글스", 2);   add(scores, "LG 트윈스", 1);
            add(scores, "SSG 랜더스", 1);
        }

        // q3: 생활 방식 (A=계획, B=즉흥)
        if ("A".equals(answers.get("q3"))) {
            add(scores, "삼성 라이온즈", 3); add(scores, "NC 다이노스", 2);
            add(scores, "두산 베어스", 2);   add(scores, "한화 이글스", 1);
        } else {
            add(scores, "LG 트윈스", 2);     add(scores, "키움 히어로즈", 2);
            add(scores, "SSG 랜더스", 1);    add(scores, "KT 위즈", 1);
            add(scores, "롯데 자이언츠", 1);
        }

        // q4: 도전 성향 (A=도전, B=안정)
        if ("A".equals(answers.get("q4"))) {
            add(scores, "KT 위즈", 3);       add(scores, "SSG 랜더스", 2);
            add(scores, "키움 히어로즈", 2); add(scores, "KIA 타이거즈", 1);
            add(scores, "LG 트윈스", 1);
        } else {
            add(scores, "삼성 라이온즈", 3); add(scores, "두산 베어스", 2);
            add(scores, "한화 이글스", 2);   add(scores, "NC 다이노스", 1);
        }

        // q5: 관람 스타일 (A=열정 응원, B=분석 관람)
        if ("A".equals(answers.get("q5"))) {
            add(scores, "롯데 자이언츠", 3); add(scores, "KIA 타이거즈", 2);
            add(scores, "SSG 랜더스", 2);    add(scores, "한화 이글스", 2);
            add(scores, "LG 트윈스", 1);
        } else {
            add(scores, "NC 다이노스", 3);   add(scores, "삼성 라이온즈", 2);
            add(scores, "두산 베어스", 2);   add(scores, "KT 위즈", 1);
        }

        // q6: 선호 스토리 (A=역전극, B=완벽한 경기)
        if ("A".equals(answers.get("q6"))) {
            add(scores, "롯데 자이언츠", 3); add(scores, "KIA 타이거즈", 2);
            add(scores, "한화 이글스", 2);   add(scores, "SSG 랜더스", 1);
            add(scores, "키움 히어로즈", 1);
        } else {
            add(scores, "삼성 라이온즈", 3); add(scores, "NC 다이노스", 2);
            add(scores, "두산 베어스", 2);   add(scores, "LG 트윈스", 1);
        }

        // q7: 관람 목적 (A=분위기/재미, B=승리/진지)
        if ("A".equals(answers.get("q7"))) {
            add(scores, "LG 트윈스", 3);     add(scores, "SSG 랜더스", 2);
            add(scores, "KT 위즈", 2);       add(scores, "키움 히어로즈", 2);
            add(scores, "롯데 자이언츠", 1);
        } else {
            add(scores, "두산 베어스", 3);   add(scores, "KIA 타이거즈", 2);
            add(scores, "삼성 라이온즈", 2); add(scores, "NC 다이노스", 1);
            add(scores, "한화 이글스", 1);
        }

        // q8: 성격 유형 (A=화끈감성, B=차분논리)
        if ("A".equals(answers.get("q8"))) {
            add(scores, "SSG 랜더스", 3);    add(scores, "롯데 자이언츠", 2);
            add(scores, "키움 히어로즈", 2); add(scores, "KIA 타이거즈", 2);
            add(scores, "한화 이글스", 1);   add(scores, "LG 트윈스", 1);
        } else {
            add(scores, "NC 다이노스", 3);   add(scores, "삼성 라이온즈", 2);
            add(scores, "두산 베어스", 2);   add(scores, "KT 위즈", 1);
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
        boolean isExtrovert  = "A".equals(answers.get("q1"));
        boolean isLogical    = "A".equals(answers.get("q2"));
        boolean isPlanned    = "A".equals(answers.get("q3"));
        boolean isDaring     = "A".equals(answers.get("q4"));
        boolean isPassionate = "A".equals(answers.get("q5"));
        boolean likeDrama    = "A".equals(answers.get("q6"));
        boolean forFun       = "A".equals(answers.get("q7"));
        boolean isHotBlood   = "A".equals(answers.get("q8"));

        return switch (teamName) {
            case "KIA 타이거즈" ->
                (likeDrama ? "역전 드라마 선호" : "승리 지향") + " + " + (isExtrovert ? "외향적 에너지" : "진지한 승부욕");
            case "삼성 라이온즈" ->
                (isLogical ? "논리적 판단력" : "체계적 성향") + " + " + (isPlanned ? "계획적 스타일" : "안정 추구");
            case "LG 트윈스" ->
                (forFun ? "분위기 중시" : "자유로운 성향") + " + " + (isExtrovert ? "외향적 성격" : "트렌디한 감각");
            case "두산 베어스" ->
                (isPlanned ? "체계적 사고" : "강한 승부욕") + " + " + (!isDaring ? "안정 추구" : "끈질긴 투지");
            case "KT 위즈" ->
                (isDaring ? "도전 정신" : "혁신 지향") + " + " + (forFun ? "개방적 마인드" : "성장 지향");
            case "SSG 랜더스" ->
                (isHotBlood ? "화끈한 감성" : "열정적 성향") + " + " + (isDaring ? "도전적 마인드" : "공격적 플레이 선호");
            case "롯데 자이언츠" ->
                (isPassionate ? "열정 응원" : "감동 추구") + " + " + (likeDrama ? "역전극 선호" : "진한 감동");
            case "한화 이글스" ->
                (likeDrama ? "역전극 선호" : "우직한 응원") + " + " + (!isDaring ? "끈기 있는 성향" : "변화 기대");
            case "NC 다이노스" ->
                (isLogical ? "분석적 사고" : "스마트한 감각") + " + " + (!isPassionate ? "전략 중시" : "체계적 성향");
            case "키움 히어로즈" ->
                (isDaring ? "도전 선호" : "개성 있는 성향") + " + " + (isHotBlood ? "화끈한 스타일" : "자유로운 마인드");
            default -> "성향 분석 결과 높은 적합도";
        };
    }

    // ─────────────────────────────────────────────
    // 팬 성향 프로필
    // ─────────────────────────────────────────────

    private String determineFanProfile(Map<String, String> answers) {
        boolean isExtrovert  = "A".equals(answers.get("q1"));
        boolean isLogical    = "A".equals(answers.get("q2"));
        boolean isPlanned    = "A".equals(answers.get("q3"));
        boolean isDaring     = "A".equals(answers.get("q4"));
        boolean isPassionate = "A".equals(answers.get("q5"));
        boolean likeDrama    = "A".equals(answers.get("q6"));
        boolean forFun       = "A".equals(answers.get("q7"));
        boolean isHotBlood   = "A".equals(answers.get("q8"));

        // 각 프로필 점수 계산 (0~6)
        int emotional   = (!isLogical ? 2 : 0) + (likeDrama ? 2 : 0) + (isPassionate ? 1 : 0) + (isHotBlood ? 1 : 0);
        int analytical  = (isLogical ? 2 : 0) + (isPlanned ? 2 : 0) + (!isPassionate ? 1 : 0) + (!isHotBlood ? 1 : 0);
        int enthusiast  = (isExtrovert ? 2 : 0) + (isPassionate ? 2 : 0) + (forFun ? 1 : 0) + (likeDrama ? 1 : 0);
        int traditional = (!isDaring ? 2 : 0) + (!likeDrama ? 2 : 0) + (!forFun ? 1 : 0) + (isPlanned ? 1 : 0);
        int challenger  = (isDaring ? 2 : 0) + (isHotBlood ? 2 : 0) + (isExtrovert ? 1 : 0) + (forFun ? 1 : 0);
        int calm        = (!isExtrovert ? 2 : 0) + (!isPassionate ? 2 : 0) + (!isHotBlood ? 1 : 0) + (isLogical ? 1 : 0);

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
        boolean isExtrovert  = "A".equals(answers.get("q1"));
        boolean isLogical    = "A".equals(answers.get("q2"));
        boolean isPlanned    = "A".equals(answers.get("q3"));
        boolean isDaring     = "A".equals(answers.get("q4"));
        boolean isPassionate = "A".equals(answers.get("q5"));
        boolean likeDrama    = "A".equals(answers.get("q6"));
        boolean forFun       = "A".equals(answers.get("q7"));
        boolean isHotBlood   = "A".equals(answers.get("q8"));

        return switch (teamName) {
            case "KIA 타이거즈" -> String.format(
                "당신의 %s 성향과 %s 모습이 KIA 타이거즈의 DNA와 딱 맞아요! " +
                "최다 우승 팀의 뜨거운 팬덤 속에서 야구의 진정한 재미를 느낄 수 있을 거예요.",
                isExtrovert ? "열정적이고 외향적인" : "강인하고 진지한",
                likeDrama ? "역전 드라마를 즐기는" : "승리를 갈망하는"
            );
            case "삼성 라이온즈" -> String.format(
                "체계적이고 %s 당신에게는 삼성 라이온즈가 딱입니다! " +
                "철저한 데이터와 안정적인 운영을 추구하는 팀 스타일이 당신의 성향과 완벽히 맞아요.",
                isLogical ? "논리적인" : "계획적인"
            );
            case "LG 트윈스" -> String.format(
                "트렌디하고 %s 당신은 LG 트윈스와 잘 맞아요! " +
                "서울의 화려한 감성 속에서 세련된 야구 문화를 즐길 수 있을 거예요.",
                forFun ? "재미를 추구하는" : "자유로운"
            );
            case "두산 베어스" -> String.format(
                "%s 당신에게 두산 베어스를 추천해요! " +
                "위기에 강하고 강인한 저력으로 끝까지 포기하지 않는 팀 정신이 당신과 닮았어요.",
                isPlanned ? "뚝심 있고 체계적인" : "진지하고 승부욕 강한"
            );
            case "KT 위즈" ->
                "도전을 두려워하지 않는 당신에게 KT 위즈가 잘 맞아요! " +
                "젊고 혁신적인 구단 문화 속에서 함께 성장하는 재미를 느낄 수 있을 거예요.";
            case "SSG 랜더스" -> String.format(
                "화끈하고 %s 당신에게 SSG 랜더스를 추천해요! " +
                "공격적인 야구와 스타 플레이어들이 만들어내는 짜릿한 순간들이 기다리고 있어요.",
                isHotBlood ? "감성적인" : "열정적인"
            );
            case "롯데 자이언츠" -> String.format(
                "%s 당신은 롯데 자이언츠와 함께하면 야구의 진한 감동을 느낄 수 있어요! " +
                "부산 갈매기와 함께하는 사직구장의 뜨거운 응원은 잊을 수 없는 경험이 될 거예요.",
                isPassionate ? "열정적으로 응원하는" : "감성적인"
            );
            case "한화 이글스" -> String.format(
                "끈기 있고 %s 당신에게 한화 이글스가 잘 맞아요! " +
                "팬과의 깊은 유대감과 감동적인 응원 문화로 야구를 진심으로 즐길 수 있을 거예요.",
                likeDrama ? "역전극에 감동받는" : "우직한"
            );
            case "NC 다이노스" -> String.format(
                "스마트하고 %s 당신에게 NC 다이노스를 추천해요! " +
                "데이터 기반의 체계적인 야구를 분석하며 보는 즐거움이 남다를 거예요.",
                isLogical ? "논리적인" : "분석적인"
            );
            case "키움 히어로즈" -> String.format(
                "개성 넘치고 %s 당신에게 키움 히어로즈가 딱이에요! " +
                "젊고 화끈한 타격과 저비용 고효율의 도전적인 구단 스타일이 당신의 에너지와 잘 어울려요.",
                isDaring ? "도전적인" : "자유로운"
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
