package com.baseball.recommend.domain.standing;

import com.baseball.recommend.domain.standing.dto.StandingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
public class StandingController {

    private final StandingService standingService;

    @GetMapping
    public ResponseEntity<List<StandingDto>> getStandings() {
        return ResponseEntity.ok(standingService.getStandings());
    }
}
