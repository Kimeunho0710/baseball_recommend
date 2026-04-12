package com.baseball.recommend.domain.survey;

import com.baseball.recommend.domain.recommend.RecommendService;
import com.baseball.recommend.domain.recommend.dto.RecommendResponse;
import com.baseball.recommend.domain.survey.dto.QuestionDto;
import com.baseball.recommend.domain.survey.dto.SurveyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final RecommendService recommendService;

    @GetMapping("/questions")
    public ResponseEntity<List<QuestionDto>> getQuestions() {
        return ResponseEntity.ok(surveyService.getQuestions());
    }

    @PostMapping("/submit")
    public ResponseEntity<RecommendResponse> submit(@Valid @RequestBody SurveyRequest request) {
        SurveyResult surveyResult = surveyService.saveSurvey(request);
        RecommendResponse response = recommendService.recommend(surveyResult);
        return ResponseEntity.ok(response);
    }
}
