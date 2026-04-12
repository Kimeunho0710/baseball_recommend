package com.baseball.recommend.domain.game;

import com.baseball.recommend.domain.game.dto.GameDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {

    private volatile List<GameDto> gameCache = new ArrayList<>();
    private volatile LocalDateTime lastUpdated = null;

    private static final String SCHEDULE_URL =
            "https://www.koreabaseball.com/Schedule/Schedule.aspx";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    /** 팀 약칭 → 정식명 (긴 것을 먼저 매핑해야 "기아 타이거즈" 가 "기아" 에 먹히지 않음) */
    private static final LinkedHashMap<String, String> TEAM_NAME_MAP = new LinkedHashMap<>();
    static {
        TEAM_NAME_MAP.put("기아 타이거즈",  "KIA 타이거즈");
        TEAM_NAME_MAP.put("KIA 타이거즈",  "KIA 타이거즈");
        TEAM_NAME_MAP.put("삼성 라이온즈",  "삼성 라이온즈");
        TEAM_NAME_MAP.put("LG 트윈스",     "LG 트윈스");
        TEAM_NAME_MAP.put("두산 베어스",   "두산 베어스");
        TEAM_NAME_MAP.put("KT 위즈",       "KT 위즈");
        TEAM_NAME_MAP.put("SSG 랜더스",    "SSG 랜더스");
        TEAM_NAME_MAP.put("롯데 자이언츠", "롯데 자이언츠");
        TEAM_NAME_MAP.put("한화 이글스",   "한화 이글스");
        TEAM_NAME_MAP.put("NC 다이노스",   "NC 다이노스");
        TEAM_NAME_MAP.put("키움 히어로즈", "키움 히어로즈");
        // 약칭 (뒤에 등록해서 풀명이 우선 매칭됨)
        TEAM_NAME_MAP.put("기아",  "KIA 타이거즈");
        TEAM_NAME_MAP.put("KIA",   "KIA 타이거즈");
        TEAM_NAME_MAP.put("삼성",  "삼성 라이온즈");
        TEAM_NAME_MAP.put("LG",    "LG 트윈스");
        TEAM_NAME_MAP.put("두산",  "두산 베어스");
        TEAM_NAME_MAP.put("KT",    "KT 위즈");
        TEAM_NAME_MAP.put("SSG",   "SSG 랜더스");
        TEAM_NAME_MAP.put("롯데",  "롯데 자이언츠");
        TEAM_NAME_MAP.put("한화",  "한화 이글스");
        TEAM_NAME_MAP.put("NC",    "NC 다이노스");
        TEAM_NAME_MAP.put("키움",  "키움 히어로즈");
    }

    // ─────────────────────────────────────────────
    // 공개 API
    // ─────────────────────────────────────────────

    public List<GameDto> getTodayGames() {
        ensureCache();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<GameDto> result = gameCache.stream()
                .filter(g -> today.equals(g.getGameDate()))
                .collect(Collectors.toList());
        log.info("오늘({}) 경기: {}건", today, result.size());
        return result;
    }

    public List<GameDto> getRecentGames() {
        ensureCache();
        String cutoff = LocalDate.now().minusDays(7)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return gameCache.stream()
                .filter(GameDto::isCompleted)
                .filter(g -> g.getGameDate().compareTo(cutoff) >= 0
                          && g.getGameDate().compareTo(today) < 0)
                .sorted(Comparator.comparing(GameDto::getGameDate).reversed())
                .limit(15)
                .collect(Collectors.toList());
    }

    public List<String> getTeamRecentForm(String teamName) {
        ensureCache();
        String normalized = normalizeTeamName(teamName);
        if (normalized == null) normalized = teamName;
        final String team = normalized;

        return gameCache.stream()
                .filter(GameDto::isCompleted)
                .filter(g -> team.equals(g.getHomeTeam()) || team.equals(g.getAwayTeam()))
                .sorted(Comparator.comparing(GameDto::getGameDate).reversed())
                .limit(5)
                .map(g -> computeResult(g, team))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 스케줄 갱신
    // ─────────────────────────────────────────────

    @Scheduled(initialDelay = 90_000, fixedDelay = 30 * 60_000)
    public void scheduledRefresh() {
        log.info("[스케줄] 경기 일정 갱신 시작");
        refreshCache();
    }

    private void ensureCache() {
        if (gameCache.isEmpty() || lastUpdated == null
                || lastUpdated.isBefore(LocalDateTime.now().minusMinutes(30))) {
            refreshCache();
        }
    }

    private void refreshCache() {
        List<GameDto> games = scrapeSchedule();
        if (!games.isEmpty()) {
            gameCache = games;
            lastUpdated = LocalDateTime.now();
            log.info("경기 캐시 갱신 완료: {}경기", games.size());
        } else {
            log.warn("경기 스크래핑 결과 없음 — 기존 캐시 유지");
        }
    }

    // ─────────────────────────────────────────────
    // 스크래핑: GET → (데이터 없으면) ASPX POST
    // ─────────────────────────────────────────────

    private List<GameDto> scrapeSchedule() {
        LocalDate today = LocalDate.now();
        int year  = today.getYear();
        int month = today.getMonthValue();

        try {
            // ① GET 시도
            Document doc = Jsoup.connect(SCHEDULE_URL)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .header("Referer", "https://www.koreabaseball.com")
                    .timeout(15_000)
                    .get();

            log.info("스케줄 페이지 응답 길이: {}", doc.html().length());
            logHtmlSnippet(doc);

            List<GameDto> games = parseGames(doc, year, month);

            // ② GET으로 데이터가 없으면 ViewState POST 시도
            if (games.isEmpty()) {
                log.info("GET 파싱 결과 없음 → ASPX POST 시도");
                games = scrapeWithPost(doc, year, month);
            }

            return games;

        } catch (Exception e) {
            log.error("스크래핑 오류: {}", e.getMessage());
            return List.of();
        }
    }

    private List<GameDto> scrapeWithPost(Document getDoc, int year, int month) {
        try {
            String viewState    = getDoc.select("[name='__VIEWSTATE']").val();
            String vsGenerator  = getDoc.select("[name='__VIEWSTATEGENERATOR']").val();
            String evValidation = getDoc.select("[name='__EVENTVALIDATION']").val();

            if (viewState.isEmpty()) {
                log.warn("ViewState 없음 — POST 불가");
                return List.of();
            }

            log.info("ASPX POST 시도 (year={}, month={})", year, month);

            Document postDoc = Jsoup.connect(SCHEDULE_URL)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .header("Referer", SCHEDULE_URL)
                    .method(Connection.Method.POST)
                    .data("__EVENTTARGET", "ctl00$cphContents$ddlMonth")
                    .data("__EVENTARGUMENT", "")
                    .data("__VIEWSTATE", viewState)
                    .data("__VIEWSTATEGENERATOR", vsGenerator)
                    .data("__EVENTVALIDATION", evValidation)
                    .data("ctl00$cphContents$ddlSeries", "0")
                    .data("ctl00$cphContents$ddlYear", String.valueOf(year))
                    .data("ctl00$cphContents$ddlMonth", String.format("%02d", month))
                    .timeout(15_000)
                    .execute()
                    .parse();

            logHtmlSnippet(postDoc);
            List<GameDto> games = parseGames(postDoc, year, month);
            log.info("POST 파싱 결과: {}경기", games.size());
            return games;

        } catch (Exception e) {
            log.error("ASPX POST 오류: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────
    // HTML 파싱 — 팀명 기반 row 탐지
    // ─────────────────────────────────────────────

    private List<GameDto> parseGames(Document doc, int year, int month) {
        List<GameDto> games = new ArrayList<>();
        int currentDay = -1;

        Pattern dayPattern = Pattern.compile("(\\d{1,2})일");

        for (Element row : doc.select("tr")) {
            // ── 날짜 헤더 감지 (<th> 또는 colspan이 큰 <td>) ──
            String headerText = extractHeaderText(row);
            if (headerText != null) {
                Matcher m = dayPattern.matcher(headerText);
                if (m.find()) {
                    currentDay = Integer.parseInt(m.group(1));
                    log.debug("날짜 헤더 감지: {}일 (원문: {})", currentDay, headerText);
                    continue;
                }
            }

            // ── 팀명 기반 게임 row 탐지 ──
            String rowText = row.text();
            List<String> teamsInRow = extractTeamsFromText(rowText);

            if (teamsInRow.size() < 2) continue;

            String awayTeam = teamsInRow.get(0);
            String homeTeam = teamsInRow.get(1);

            String gameTime = extractTime(rowText);
            int[]  scores   = extractScore(row, rowText);
            String stadium  = extractStadium(row, rowText, awayTeam, homeTeam);
            boolean completed = (scores != null) || rowText.contains("종료");

            String dateStr = currentDay > 0
                    ? String.format("%04d-%02d-%02d", year, month, currentDay)
                    : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            GameDto game = GameDto.builder()
                    .gameDate(dateStr)
                    .gameTime(gameTime)
                    .awayTeam(awayTeam)
                    .homeTeam(homeTeam)
                    .awayScore(scores != null ? scores[0] : null)
                    .homeScore(scores != null ? scores[1] : null)
                    .stadium(stadium)
                    .completed(completed)
                    .build();

            // 중복 제거
            boolean dup = games.stream().anyMatch(g ->
                    g.getGameDate().equals(game.getGameDate())
                    && g.getAwayTeam().equals(awayTeam)
                    && g.getHomeTeam().equals(homeTeam));
            if (!dup) {
                games.add(game);
                log.debug("게임 파싱: {} {} vs {} ({})", dateStr, awayTeam, homeTeam,
                        completed ? scores[0] + ":" + scores[1] : gameTime);
            }
        }

        return games;
    }

    /** row에서 날짜 헤더 텍스트 추출 (<th> 또는 colspan>=5인 <td>) */
    private String extractHeaderText(Element row) {
        Element th = row.selectFirst("th");
        if (th != null) return th.text().trim();

        for (Element td : row.select("td")) {
            String colspan = td.attr("colspan");
            if (!colspan.isEmpty() && Integer.parseInt(colspan) >= 5) {
                return td.text().trim();
            }
        }
        return null;
    }

    /** 텍스트에서 KBO 팀명 순서대로 추출 */
    private List<String> extractTeamsFromText(String text) {
        List<String[]> found = new ArrayList<>(); // [index, normalizedName]
        for (Map.Entry<String, String> e : TEAM_NAME_MAP.entrySet()) {
            int idx = text.indexOf(e.getKey());
            if (idx >= 0) {
                // 이미 같은 정식명이 더 앞 인덱스로 등록됐는지 확인
                boolean alreadyAdded = found.stream()
                        .anyMatch(f -> f[1].equals(e.getValue()));
                if (!alreadyAdded) {
                    found.add(new String[]{String.valueOf(idx), e.getValue()});
                }
            }
        }
        return found.stream()
                .sorted(Comparator.comparingInt(a -> Integer.parseInt(a[0])))
                .map(a -> a[1])
                .distinct()
                .collect(Collectors.toList());
    }

    /** "18:30" 형태 시간 추출 */
    private String extractTime(String text) {
        Matcher m = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b").matcher(text);
        while (m.find()) {
            int hour = Integer.parseInt(m.group(1));
            if (hour >= 10 && hour <= 22) return m.group(0); // 경기 시간 범위
        }
        return null;
    }

    /** 스코어 추출 — "3:5", "3 : 5", 별도 셀 두 개 모두 지원 */
    private int[] extractScore(Element row, String rowText) {
        // "숫자:숫자" 패턴 (시간이 아닌 것)
        Matcher m = Pattern.compile("(?<![\\d:])(\\d{1,2})\\s*:\\s*(\\d{1,2})(?![\\d:])").matcher(rowText);
        while (m.find()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(2));
            // 경기 시간(18:30)이 아닌 것: 양 쪽 값이 모두 20 미만
            if (a < 20 && b < 20 && !(a >= 10 && b >= 0 && b % 10 == 0)) {
                return new int[]{a, b};
            }
        }

        // 셀을 순서대로 탐색하여 단독 숫자 2개 찾기
        List<Integer> nums = new ArrayList<>();
        for (Element td : row.select("td")) {
            String t = td.text().trim();
            if (t.matches("\\d{1,2}") && Integer.parseInt(t) < 30) {
                nums.add(Integer.parseInt(t));
                if (nums.size() == 2) return new int[]{nums.get(0), nums.get(1)};
            }
        }
        return null;
    }

    /** 구장명 추출 */
    private String extractStadium(Element row, String rowText, String awayTeam, String homeTeam) {
        for (Element td : row.select("td")) {
            String t = td.text().trim();
            if (t.length() >= 4
                    && !t.contains(awayTeam.substring(0, Math.min(2, awayTeam.length())))
                    && !t.contains(homeTeam.substring(0, Math.min(2, homeTeam.length())))
                    && !t.matches("[\\d:\\s]+")
                    && !t.contains("종료") && !t.contains("취소") && !t.contains("우천")
                    && normalizeTeamName(t) == null) {
                return t;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // 폼 계산
    // ─────────────────────────────────────────────

    private String computeResult(GameDto game, String teamName) {
        if (!game.isCompleted() || game.getAwayScore() == null || game.getHomeScore() == null)
            return "?";
        boolean isHome = teamName.equals(game.getHomeTeam());
        int my  = isHome ? game.getHomeScore() : game.getAwayScore();
        int opp = isHome ? game.getAwayScore()  : game.getHomeScore();
        if (my > opp) return "W";
        if (my < opp) return "L";
        return "D";
    }

    // ─────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────

    private String normalizeTeamName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (Map.Entry<String, String> e : TEAM_NAME_MAP.entrySet()) {
            if (raw.equals(e.getKey()) || raw.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private void logHtmlSnippet(Document doc) {
        String html = doc.html();
        log.info("HTML 스니펫(앞 800자): {}",
                html.substring(0, Math.min(800, html.length()))
                    .replaceAll("\\s+", " "));
    }
}
