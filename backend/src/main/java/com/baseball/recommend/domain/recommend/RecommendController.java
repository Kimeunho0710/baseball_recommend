package com.baseball.recommend.domain.recommend;

import com.baseball.recommend.domain.recommend.dto.RecommendResponse;
import com.baseball.recommend.global.exception.BusinessException;
import com.baseball.recommend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendRepository recommendRepository;

    @GetMapping("/{id}")
    public ResponseEntity<RecommendResponse> getResult(@PathVariable Long id) {
        RecommendResult result = recommendRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SURVEY_NOT_FOUND));
        return ResponseEntity.ok(RecommendResponse.from(result));
    }
}
