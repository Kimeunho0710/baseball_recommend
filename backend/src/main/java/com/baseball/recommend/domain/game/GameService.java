package com.baseball.recommend.domain.game;

import com.baseball.recommend.domain.game.dto.GameDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper;

    private static final String KBO_SCHEDULE_URL =
            "https://www.koreabaseball.com/ws/Schedule.asmx/GetScheduleList";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    /** KBO 약칭 → 정식 팀명 */
    private static final Map<String, String> TEAM_NAME_MAP = Map.ofEntries(
        Map.entry("KIA",  "KIA 타이거즈"),
        Map.entry("기아",  "KIA 타이거즈"),
        Map.entry("삼성",  "삼성 라이온즈"),
        Map.entry("LG",   "LG 트윈스"),
        Map.entry("두산",  "두산 베어스"),
        Map.entry("KT",   "KT 위즈"),
        Map.entry("SSG",  "SSG 랜더스"),
        Map.entry("롯데",  "롯데 자이언츠"),
        Map.entry("한화",  "한화 이글스"),
        Map.entry("NC",   "NC 다이노스"),
        Map.entry("키움",  "키움 히어로즈")
    );

    // ─────────────────────────────────────────────
    // 공개 API
    // ─────────────────────────────────────────────

    public List<GameDto> getTodayGames() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<Game> dbGames = gameRepository.findByGameDate(today);
        if (!dbGames.isEmpty()) {
            log.info("오늘({}) 경기 DB 반환: {}건", today, dbGames.size());
            return toDto(dbGames);
        }
        log.info("오늘 경기 DB 없음, 즉시 스크래핑");
        List<GameDto> scraped = fetchCurrentMonths();
        if (!scraped.isEmpty()) upsertAll(scraped);
        return scraped.stream()
                .filter(g -> today.equals(g.getGameDate()))
                .collect(Collectors.toList());
    }

    public List<GameDto> getRecentGames() {
        String cutoff = LocalDate.now().minusDays(7)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String yesterday = LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<Game> dbGames = gameRepository
                .findByCompletedTrueAndGameDateBetweenOrderByGameDateDesc(cutoff, yesterday);
        if (!dbGames.isEmpty()) {
            log.info("최근 결과 DB 반환: {}건", dbGames.size());
            return toDto(dbGames).stream().limit(15).collect(Collectors.toList());
        }
        log.info("최근 결과 DB 없음, 즉시 스크래핑");
        List<GameDto> scraped = fetchCurrentMonths();
        if (!scraped.isEmpty()) upsertAll(scraped);
        return scraped.stream()
                .filter(GameDto::isCompleted)
                .filter(g -> g.getGameDate().compareTo(cutoff) >= 0
                          && g.getGameDate().compareTo(yesterday) <= 0)
                .limit(15)
                .collect(Collectors.toList());
    }

    public List<String> getTeamRecentForm(String teamName) {
        String team = Optional.ofNullable(TEAM_NAME_MAP.get(teamName)).orElse(teamName);
        return gameRepository
                .findByCompletedTrueAndAwayTeamOrCompletedTrueAndHomeTeamOrderByGameDateDesc(team, team)
                .stream()
                .limit(5)
                .map(g -> computeResult(g, team))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 스케줄 갱신 (서버 시작 90초 후, 이후 30분마다)
    // ─────────────────────────────────────────────

    @Scheduled(initialDelay = 90_000, fixedDelay = 30 * 60_000)
    public void scheduledRefresh() {
        log.info("[스케줄] 경기 일정 갱신 시작");
        List<GameDto> games = fetchCurrentMonths();
        if (games.isEmpty()) {
            log.warn("[스케줄] 스크래핑 실패, DB 유지");
            return;
        }
        upsertAll(games);
        log.info("[스케줄] 경기 DB 갱신 완료: {}건", games.size());
    }

    // ─────────────────────────────────────────────
    // KBO WebService 호출
    // ─────────────────────────────────────────────

    /** 이번 달 + 7일이 다음 달로 넘어가면 다음 달도 함께 수집 */
    private List<GameDto> fetchCurrentMonths() {
        LocalDate today = LocalDate.now();
        List<GameDto> all = new ArrayList<>(fetchMonth(today.getYear(), today.getMonthValue()));

        LocalDate future = today.plusDays(7);
        if (future.getMonthValue() != today.getMonthValue()) {
            all.addAll(fetchMonth(future.getYear(), future.getMonthValue()));
        }
        return all;
    }

    private List<GameDto> fetchMonth(int year, int month) {
        String season = String.valueOf(year);
        String gameMonth = String.format("%02d", month);
        try {
            String body = Jsoup.connect(KBO_SCHEDULE_URL)
                    .method(Connection.Method.POST)
                    .data("leId", "1")
                    .data("srIdList", "0,9,6")
                    .data("seasonId", season)
                    .data("gameMonth", gameMonth)
                    .data("teamId", "")
                    .userAgent(USER_AGENT)
                    .header("Referer", "https://www.koreabaseball.com/Schedule/Schedule.aspx")
                    .header("Accept", "application/json, text/javascript, */*")
                    .ignoreContentType(true)
                    .timeout(15_000)
                    .execute()
                    .body();

            List<GameDto> result = parseKboResponse(body, season);
            log.info("KBO API {}-{}: {}경기 수집", season, gameMonth, result.size());
            return result;
        } catch (Exception e) {
            log.warn("KBO API {}-{} 실패: {}", season, gameMonth, e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────
    // KBO 응답 파싱
    // ─────────────────────────────────────────────

    private List<GameDto> parseKboResponse(String json, String season) {
        List<GameDto> result = new ArrayList<>();
        String currentDate = null;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode rows = root.path("rows");
            if (!rows.isArray()) return result;

            for (JsonNode rowNode : rows) {
                JsonNode row = rowNode.path("row");
                if (!row.isArray() || row.size() == 0) continue;

                // 첫 셀 Class가 "day"이면 날짜 갱신
                String firstClass = row.get(0).path("Class").asText("");
                boolean hasDay = "day".equals(firstClass);
                if (hasDay) {
                    String dayText = Jsoup.parse(row.get(0).path("Text").asText("")).text();
                    currentDate = parseKboDate(dayText, season);
                }
                if (currentDate == null) continue;

                // 오프셋: day셀 있으면 +1
                int offset = hasDay ? 1 : 0;

                String timeText = cellText(row, offset);      // time
                String playHtml = cellHtml(row, offset + 1);  // play
                String stadium  = cellText(row, offset + 6);  // stadium

                if (playHtml == null) continue;

                GameDto game = parsePlayHtml(playHtml, currentDate, timeText, stadium);
                if (game != null) result.add(game);
            }
        } catch (Exception e) {
            log.warn("KBO 응답 파싱 오류: {}", e.getMessage());
        }
        return result;
    }

    /** "04.01(수)" + "2026" → "2026-04-01" */
    private String parseKboDate(String dayText, String season) {
        try {
            String cleaned = dayText.replaceAll("\\(.*\\)", "").trim(); // "04.01"
            String[] parts = cleaned.split("\\.");
            return season + "-" + parts[0] + "-" + parts[1];
        } catch (Exception e) {
            log.debug("날짜 파싱 실패: {}", dayText);
            return null;
        }
    }

    /**
     * play 셀 HTML 파싱
     * 완료: <span>KIA</span><em><span class="lose">2</span><span>vs</span><span class="win">7</span></em><span>LG</span>
     * 예정: <span>NC</span><em><span>vs</span></em><span>KIA</span>
     */
    /**
     * play 셀 HTML 파싱
     * 완료: <span>KIA</span><em><span class="lose">2</span><span>vs</span><span class="win">7</span></em><span>LG</span>
     * 예정: <span>NC</span><em><span>vs</span></em><span>KIA</span>
     */
    private GameDto parsePlayHtml(String html, String date, String timeText, String stadium) {
        try {
            Document doc = Jsoup.parseBodyFragment(html);

            // body 직접 자식 중 span 태그만 → away / home 팀
            List<Element> teamSpans = doc.body().children().stream()
                    .filter(e -> "span".equals(e.tagName()))
                    .collect(Collectors.toList());
            if (teamSpans.size() < 2) return null;

            String awayTeam = resolveTeam(teamSpans.get(0).text());
            String homeTeam = resolveTeam(teamSpans.get(teamSpans.size() - 1).text());
            if (awayTeam == null || homeTeam == null) return null;

            // em 안의 .win / .lose span → 점수 (완료 경기만 존재)
            Element em = doc.selectFirst("em");
            Integer awayScore = null, homeScore = null;
            boolean completed = false;

            if (em != null) {
                Elements scoreSpans = em.select("span.win, span.lose");
                if (scoreSpans.size() >= 2) {
                    awayScore = parseScore(scoreSpans.get(0).text());
                    homeScore = parseScore(scoreSpans.get(1).text());
                    completed = awayScore != null && homeScore != null;
                }
            }

            return GameDto.builder()
                    .gameDate(date)
                    .gameTime(completed ? null : stripHtml(timeText))
                    .awayTeam(awayTeam)
                    .homeTeam(homeTeam)
                    .awayScore(awayScore)
                    .homeScore(homeScore)
                    .stadium(stadium != null && !stadium.isBlank() ? stadium : null)
                    .completed(completed)
                    .build();
        } catch (Exception e) {
            log.debug("play 셀 파싱 오류: {}", e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────
    // DB upsert
    // ─────────────────────────────────────────────

    public void upsertAll(List<GameDto> dtos) {
        if (dtos.isEmpty()) return;

        // 대상 날짜 전체를 한 번에 조회 → N+1 제거
        Set<String> dates = dtos.stream().map(GameDto::getGameDate).collect(Collectors.toSet());
        Map<String, Game> existingMap = gameRepository.findByGameDateIn(dates).stream()
                .collect(Collectors.toMap(
                        g -> g.getGameDate() + "|" + g.getAwayTeam() + "|" + g.getHomeTeam(),
                        g -> g
                ));

        List<Game> toSave = new ArrayList<>();
        for (GameDto dto : dtos) {
            String key = dto.getGameDate() + "|" + dto.getAwayTeam() + "|" + dto.getHomeTeam();
            Game existing = existingMap.get(key);
            if (existing != null) {
                existing.update(dto.getGameTime(), dto.getAwayScore(),
                        dto.getHomeScore(), dto.getStadium(), dto.isCompleted());
                toSave.add(existing);
            } else {
                toSave.add(toEntity(dto));
            }
        }
        gameRepository.saveAll(toSave);
        log.info("upsert 완료: {}건 (신규/갱신)", toSave.size());
    }

    // ─────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────

    private String resolveTeam(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String mapped = TEAM_NAME_MAP.get(raw.trim());
        if (mapped != null) return mapped;
        for (Map.Entry<String, String> e : TEAM_NAME_MAP.entrySet()) {
            if (raw.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private Integer parseScore(String text) {
        try { return Integer.parseInt(text.trim()); }
        catch (Exception e) { return null; }
    }

    private String cellText(JsonNode row, int index) {
        if (index >= row.size()) return null;
        return stripHtml(row.get(index).path("Text").asText(""));
    }

    private String cellHtml(JsonNode row, int index) {
        if (index >= row.size()) return null;
        String text = row.get(index).path("Text").asText("");
        return text.isBlank() ? null : text;
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return null;
        String text = Jsoup.parse(html).text().trim();
        return text.isBlank() ? null : text;
    }

    private String computeResult(Game game, String teamName) {
        if (!game.isCompleted() || game.getAwayScore() == null || game.getHomeScore() == null) return "?";
        boolean isHome = teamName.equals(game.getHomeTeam());
        int my  = isHome ? game.getHomeScore() : game.getAwayScore();
        int opp = isHome ? game.getAwayScore()  : game.getHomeScore();
        if (my > opp) return "W";
        if (my < opp) return "L";
        return "D";
    }

    private Game toEntity(GameDto dto) {
        return Game.builder()
                .gameDate(dto.getGameDate()).gameTime(dto.getGameTime())
                .awayTeam(dto.getAwayTeam()).homeTeam(dto.getHomeTeam())
                .awayScore(dto.getAwayScore()).homeScore(dto.getHomeScore())
                .stadium(dto.getStadium()).completed(dto.isCompleted())
                .build();
    }

    private List<GameDto> toDto(List<Game> games) {
        return games.stream().map(g -> GameDto.builder()
                .gameDate(g.getGameDate()).gameTime(g.getGameTime())
                .awayTeam(g.getAwayTeam()).homeTeam(g.getHomeTeam())
                .awayScore(g.getAwayScore()).homeScore(g.getHomeScore())
                .stadium(g.getStadium()).completed(g.isCompleted())
                .build()
        ).collect(Collectors.toList());
    }
}
