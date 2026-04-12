package com.baseball.recommend.domain.standing;

import com.baseball.recommend.domain.standing.dto.StandingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StandingService {

    private final StandingRepository standingRepository;

    private static final String KBO_URL =
            "https://www.koreabaseball.com/record/teamrank/teamrankdaily.aspx";
    private static final int CURRENT_SEASON = 2026;

    private static final Map<String, String> TEAM_NAME_MAP = Map.ofEntries(
        Map.entry("기아",          "KIA 타이거즈"),
        Map.entry("기아 타이거즈",  "KIA 타이거즈"),
        Map.entry("KIA",           "KIA 타이거즈"),
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

    // ────────────────────────────── 공개 API ──────────────────────────────

    public List<StandingDto> getStandings() {
        List<Standing> dbStandings = standingRepository.findBySeasonOrderByRankAsc(CURRENT_SEASON);
        if (!dbStandings.isEmpty()) {
            log.info("DB 순위 데이터 반환 ({}팀)", dbStandings.size());
            return toDto(dbStandings);
        }

        log.info("DB 데이터 없음, 즉시 스크래핑 시작");
        List<StandingDto> scraped = scrapeKboStandings();
        if (!scraped.isEmpty()) {
            saveAll(scraped);
            return scraped;
        }

        log.warn("스크래핑 실패, fallback 반환");
        return getFallbackStandings();
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 30 * 60_000)
    @Transactional
    public void scheduledRefresh() {
        log.info("[스케줄] KBO 순위 갱신 시작");
        List<StandingDto> scraped = scrapeKboStandings();
        if (scraped.isEmpty()) {
            log.warn("[스케줄] 스크래핑 실패, DB 유지");
            return;
        }
        for (StandingDto dto : scraped) {
            standingRepository.findBySeasonAndTeamName(CURRENT_SEASON, dto.getTeamName())
                .ifPresentOrElse(
                    existing -> existing.update(
                        dto.getRank(), dto.getGames(), dto.getWins(),
                        dto.getLosses(), dto.getDraws(), dto.getWinRate(),
                        dto.getGamesBehind(), dto.getStreak()
                    ),
                    () -> standingRepository.save(buildEntity(dto))
                );
        }
        log.info("[스케줄] 순위 DB 갱신 완료 ({}팀)", scraped.size());
    }

    // ────────────────────────────── 스크래핑 ──────────────────────────────

    private List<StandingDto> scrapeKboStandings() {
        try {
            log.info("KBO 순위 페이지 요청: {}", KBO_URL);
            Document doc = Jsoup.connect(KBO_URL)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                               "AppleWebKit/537.36 (KHTML, like Gecko) " +
                               "Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Referer", "https://www.koreabaseball.com")
                    .timeout(15_000)
                    .get();

            log.info("응답 HTML 길이: {}", doc.html().length());

            List<StandingDto> result = parseTableHtml(doc);
            if (!result.isEmpty()) {
                log.info("테이블 파싱 성공 ({}팀)", result.size());
                return result;
            }

            log.warn("파싱 실패 - HTML 앞부분: {}",
                    doc.html().substring(0, Math.min(500, doc.html().length())));
            return List.of();

        } catch (Exception e) {
            log.error("스크래핑 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // ────────────────────────────── HTML 테이블 파싱 ──────────────────────────────

    private List<StandingDto> parseTableHtml(Document doc) {
        String[] selectors = {
            "#tblTeamRank",
            "table.tbl_type01",
            ".rank_wrap table",
            ".record_wrap table",
            "table[class*=rank]",
            "table[class*=record]",
            "table"
        };

        for (String selector : selectors) {
            for (Element table : doc.select(selector)) {
                List<StandingDto> result = parseRows(table.select("tbody tr"));
                if (result.size() >= 8) {
                    log.info("테이블 파싱 성공 (selector='{}', {}팀)", selector, result.size());
                    return result;
                }
            }
        }

        return List.of();
    }

    private List<StandingDto> parseRows(Elements rows) {
        List<StandingDto> standings = new ArrayList<>();
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 7) continue;
            try {
                int rank    = Integer.parseInt(cols.get(0).text().trim());
                String team = normalizeTeamName(cols.get(1).text().trim());
                if (team == null || rank < 1 || rank > 10) continue;

                int games      = parseIntSafe(cols.get(2).text());
                int wins       = parseIntSafe(cols.get(3).text());
                int losses     = parseIntSafe(cols.get(4).text());
                int draws      = parseIntSafe(cols.get(5).text());
                double winRate = parseDoubleSafe(cols.get(6).text());
                String gb      = cols.size() > 7 ? cols.get(7).text().trim() : "-";
                String streak  = cols.size() > 9 ? cols.get(9).text().trim()
                               : cols.size() > 8 ? cols.get(8).text().trim() : "-";

                standings.add(StandingDto.builder()
                        .rank(rank).teamName(team).games(games)
                        .wins(wins).losses(losses).draws(draws)
                        .winRate(winRate).gamesBehind(gb).streak(streak)
                        .build());
            } catch (Exception e) {
                log.debug("행 파싱 스킵: {}", row.text());
            }
        }
        return standings;
    }

    // ────────────────────────────── DB 헬퍼 ──────────────────────────────

    private void saveAll(List<StandingDto> dtos) {
        dtos.forEach(dto -> standingRepository.save(buildEntity(dto)));
        log.info("순위 DB 저장 완료 ({}팀)", dtos.size());
    }

    private Standing buildEntity(StandingDto dto) {
        return Standing.builder()
                .season(CURRENT_SEASON)
                .rank(dto.getRank()).teamName(dto.getTeamName()).games(dto.getGames())
                .wins(dto.getWins()).losses(dto.getLosses()).draws(dto.getDraws())
                .winRate(dto.getWinRate()).gamesBehind(dto.getGamesBehind()).streak(dto.getStreak())
                .build();
    }

    private List<StandingDto> toDto(List<Standing> list) {
        return list.stream()
                .map(s -> StandingDto.builder()
                        .rank(s.getRank()).teamName(s.getTeamName()).games(s.getGames())
                        .wins(s.getWins()).losses(s.getLosses()).draws(s.getDraws())
                        .winRate(s.getWinRate()).gamesBehind(s.getGamesBehind()).streak(s.getStreak())
                        .build())
                .collect(Collectors.toList());
    }

    // ────────────────────────────── 공통 유틸 ──────────────────────────────

    private String normalizeTeamName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String mapped = TEAM_NAME_MAP.get(raw);
        if (mapped != null) return mapped;
        for (Map.Entry<String, String> entry : TEAM_NAME_MAP.entrySet()) {
            if (raw.contains(entry.getKey())) return entry.getValue();
        }
        log.warn("팀명 정규화 실패: '{}'", raw);
        return null;
    }

    private int parseIntSafe(String text) {
        try { return Integer.parseInt(text.trim().replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    private double parseDoubleSafe(String text) {
        try { return Double.parseDouble(text.trim()); }
        catch (Exception e) { return 0.0; }
    }

    private List<StandingDto> getFallbackStandings() {
        return List.of(
            make(1,  "KIA 타이거즈"),  make(2,  "삼성 라이온즈"),
            make(3,  "LG 트윈스"),     make(4,  "두산 베어스"),
            make(5,  "KT 위즈"),       make(6,  "SSG 랜더스"),
            make(7,  "롯데 자이언츠"), make(8,  "한화 이글스"),
            make(9,  "NC 다이노스"),   make(10, "키움 히어로즈")
        );
    }

    private StandingDto make(int rank, String name) {
        return StandingDto.builder()
                .rank(rank).teamName(name).games(0).wins(0).losses(0)
                .draws(0).winRate(0.0).gamesBehind("-").streak("-").build();
    }
}
