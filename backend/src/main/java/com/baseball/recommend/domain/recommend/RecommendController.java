package com.baseball.recommend.domain.recommend;

import com.baseball.recommend.domain.recommend.dto.PopularTeamDto;
import com.baseball.recommend.domain.recommend.dto.RecommendResponse;
import com.baseball.recommend.domain.recommend.dto.TeamRankItem;
import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendRepository recommendRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/popular-teams")
    public ResponseEntity<List<PopularTeamDto>> getPopularTeams() {
        List<Object[]> rows = recommendRepository.countByTeam();
        long total = rows.stream().mapToLong(r -> (Long) r[2]).sum();
        List<PopularTeamDto> result = rows.stream()
                .map(r -> PopularTeamDto.of((String) r[0], (String) r[1], (Long) r[2], total))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecommendResponse> getResult(@PathVariable Long id) {
        RecommendResult result = recommendRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SURVEY_NOT_FOUND));

        List<TeamRankItem> top3 = deserializeTop3(result.getTop3Json());

        return ResponseEntity.ok(RecommendResponse.from(result, top3));
    }

    private List<TeamRankItem> deserializeTop3(String top3Json) {
        if (top3Json == null) return null;
        try {
            return objectMapper.readValue(top3Json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("top3 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
