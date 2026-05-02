package com.baseball.recommend.domain.coach;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @PostMapping("/chat")
    public ResponseEntity<CoachResponse> chat(@RequestBody CoachRequest request) {
        String reply = coachService.chat(
                request.recommendId(),
                request.message(),
                request.history() != null ? request.history() : java.util.List.of()
        );
        return ResponseEntity.ok(new CoachResponse(reply));
    }
}
