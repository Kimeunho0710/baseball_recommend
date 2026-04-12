package com.baseball.recommend.domain.game;

import com.baseball.recommend.domain.game.dto.GameDto;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class GameService {

    private volatile List<GameDto> gameCache = new ArrayList<>();
    private volatile LocalDateTime lastUpdated = null;

    private static final String SCHEDULE_URL =
            "https://www.koreabaseball.com/Schedule/Schedule.aspx";

    private static final Map<String, String> TEAM_NAME_MAP = Map.ofEntries(
        Map.entry("기아",          "KIA 타이거즈"),
        Map.entry("KIA",           "KIA 타이거즈"),
        Map.entry("기아 타이거즈",  "KIA 타이거즈"),
        Map.entry("KIA 타이거즈",  "KIA 타이거즈"),
        Map.entry("삼성",          "삼성 라이온즈"),
        Map.entry("삼성 라이온즈",  "삼성 라이온즈"),
        Map.entry("LG",            "LG 트윈스"),
        Map.entry("LG 트윈스",     "LG 트윈스"),
        Map.entry("두산",          "두산 베어스"),
        Map.entry("두산 베어스",   "두산 베어스"),
        Map.entry("KT",            "KT 위즈"),
        Map.entry("KT 위즈",       "KT 위즈"),
        Map.entry("SSG",           "SSG 랜더스"),
        Map.entry("SSG 랜더스",    "SSG 랜더스"),
        Map.entry("롯데",          "롯데 자이언츠"),
        Map.entry("롯데 자이언츠", "롯데 자이언츠"),
        Map.entry("한화",          "한화 이글스"),
        Map.entry("한화 이글스",   "한화 이글스"),
        Map.entry("NC",            "NC 다이노스"),
        Map.entry("NC 다이노스",   "NC 다이노스"),
        Map.entry("키움",          "키움 히어로즈"),
        Map.entry("키움 히어로즈", "키움 히어로즈")
    );

    // ─────────────────────────────────────────────
    // 공개 API
    // ─────────────────────────────────────────────

    public List<GameDto> getTodayGames() {
        ensureCache();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return gameCache.stream()
                .filter(g -> today.equals(g.getGameDate()))
                .collect(Collectors.toList());
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
            log.info("경기 캐시 갱신: {}경기", games.size());
        } else {
            log.warn("경기 스크래핑 실패 또는 결과 없음");
        }
    }

    // ─────────────────────────────────────────────
    // 스크래핑
    // ─────────────────────────────────────────────

    private List<GameDto> scrapeSchedule() {
        LocalDate today = LocalDate.now();
        String url = SCHEDULE_URL + "?seriesId=0&year=" + today.getYear()
                + "&month=" + String.format("%02d", today.getMonthValue());
        try {
            log.info("경기 일정 스크래핑: {}", url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                             + "AppleWebKit/537.36 (KHTML, like Gecko) "
                             + "Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .header("Referer", "https://www.koreabaseball.com")
                    .timeout(15_000)
                    .get();

            List<GameDto> games = parseGames(doc, today.getYear(), today.getMonthValue());
            log.info("파싱 완료: {}경기", games.size());
            return games;
        } catch (Exception e) {
            log.error("경기 스크래핑 오류: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────
    // HTML 파싱
    // ─────────────────────────────────────────────

    private List<GameDto> parseGames(Document doc, int year, int month) {
        List<GameDto> games = new ArrayList<>();

        // 날짜 헤더를 트래킹하며 테이블 파싱
        String[] tableSelectors = {
            "#scheduleList table", ".schedule table",
            ".tbl_schedule", "table.tbl_type01", "table"
        };

        for (String sel : tableSelectors) {
            List<GameDto> result = tryParseTable(doc.select(sel).first(), year, month);
            if (!result.isEmpty()) {
                log.info("경기 파싱 성공 (selector='{}', {}경기)", sel, result.size());
                return result;
            }
        }

        // 테이블 없이 전체 tr 탐색
        return tryParseAllRows(doc.select("tr"), year, month);
    }

    private List<GameDto> tryParseTable(Element table, int year, int month) {
        if (table == null) return List.of();
        return tryParseAllRows(table.select("tr"), year, month);
    }

    private List<GameDto> tryParseAllRows(Elements rows, int year, int month) {
        List<GameDto> games = new ArrayList<>();
        int currentDay = -1;

        for (Element row : rows) {
            String rowText = row.text().trim();

            // 날짜 헤더 행 감지: "12일" 또는 "4월 12일" 패턴
            if (rowText.matches(".*\\d{1,2}일.*")) {
                try {
                    String[] parts = rowText.split("[월일\\s]+");
                    for (String p : parts) {
                        int val = Integer.parseInt(p.trim());
                        if (val >= 1 && val <= 31) { currentDay = val; break; }
                    }
                } catch (Exception ignored) {}
                continue;
            }

            if (currentDay < 0) continue;

            Elements cells = row.select("td");
            if (cells.size() < 5) continue;

            GameDto game = tryParseGameRow(cells, year, month, currentDay);
            if (game != null) games.add(game);
        }
        return games;
    }

    private GameDto tryParseGameRow(Elements cells, int year, int month, int day) {
        String awayTeam = null, homeTeam = null;
        Integer awayScore = null, homeScore = null;
        String gameTime = null, stadium = null;
        boolean completed = false;

        for (Element cell : cells) {
            String text = cell.text().trim();
            if (text.isEmpty()) continue;

            // 시간 패턴 (18:30)
            if (text.matches("\\d{1,2}:\\d{2}") && gameTime == null) {
                gameTime = text;
                continue;
            }

            // 팀명 인식
            String norm = normalizeTeamName(text);
            if (norm != null) {
                if (awayTeam == null) awayTeam = norm;
                else if (homeTeam == null && !norm.equals(awayTeam)) homeTeam = norm;
                continue;
            }

            // 스코어 패턴 "3:5" 또는 단독 숫자
            if (text.matches("\\d+:\\d+") && awayTeam != null) {
                try {
                    String[] parts = text.split(":");
                    awayScore = Integer.parseInt(parts[0].trim());
                    homeScore = Integer.parseInt(parts[1].trim());
                    completed = true;
                } catch (Exception ignored) {}
                continue;
            }
            if (text.matches("\\d{1,2}") && awayTeam != null && awayScore == null) {
                awayScore = Integer.parseInt(text);
                continue;
            }
            if (text.matches("\\d{1,2}") && awayScore != null && homeScore == null) {
                homeScore = Integer.parseInt(text);
                completed = true;
                continue;
            }

            // 종료 상태 명시
            if ("종료".equals(text)) completed = true;

            // 구장명 (길이 4+ 이상이고 다른 패턴 아닌 것)
            if (text.length() >= 4 && !text.contains("경기") && stadium == null
                    && normalizeTeamName(text) == null && !text.matches(".*\\d.*")) {
                stadium = text;
            }
        }

        if (awayTeam == null || homeTeam == null) return null;

        String gameDate = String.format("%04d-%02d-%02d", year, month, day);
        return GameDto.builder()
                .gameDate(gameDate)
                .gameTime(gameTime)
                .awayTeam(awayTeam)
                .homeTeam(homeTeam)
                .awayScore(awayScore)
                .homeScore(homeScore)
                .stadium(stadium)
                .completed(completed)
                .build();
    }

    // ─────────────────────────────────────────────
    // 폼 계산
    // ─────────────────────────────────────────────

    private String computeResult(GameDto game, String teamName) {
        if (!game.isCompleted() || game.getAwayScore() == null || game.getHomeScore() == null)
            return "?";
        boolean isHome = teamName.equals(game.getHomeTeam());
        int myScore  = isHome ? game.getHomeScore() : game.getAwayScore();
        int oppScore = isHome ? game.getAwayScore()  : game.getHomeScore();
        if (myScore > oppScore) return "W";
        if (myScore < oppScore) return "L";
        return "D";
    }

    // ─────────────────────────────────────────────
    // 팀명 정규화
    // ─────────────────────────────────────────────

    private String normalizeTeamName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String mapped = TEAM_NAME_MAP.get(raw);
        if (mapped != null) return mapped;
        for (Map.Entry<String, String> e : TEAM_NAME_MAP.entrySet()) {
            if (raw.contains(e.getKey())) return e.getValue();
        }
        return null;
    }
}
