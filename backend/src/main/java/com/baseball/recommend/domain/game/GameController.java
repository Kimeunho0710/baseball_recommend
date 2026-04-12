package com.baseball.recommend.domain.game;

import com.baseball.recommend.domain.game.dto.GameDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/today")
    public ResponseEntity<List<GameDto>> getTodayGames() {
        return ResponseEntity.ok(gameService.getTodayGames());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<GameDto>> getRecentGames() {
        return ResponseEntity.ok(gameService.getRecentGames());
    }

    @GetMapping("/form/{teamName}")
    public ResponseEntity<Map<String, Object>> getTeamForm(@PathVariable String teamName) {
        List<String> form = gameService.getTeamRecentForm(teamName);
        return ResponseEntity.ok(Map.of("teamName", teamName, "form", form));
    }
}
