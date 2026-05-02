package com.baseball.recommend.infra.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ClaudeApiClient {

    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-haiku-4-5";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public ClaudeApiClient(ObjectMapper objectMapper, @Value("${anthropic.api.key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    /**
     * @return AI reply text, or null if API key missing/failed (caller falls back)
     */
    public String chatReply(String systemPrompt, List<Map<String, String>> history, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            List<Map<String, Object>> systemContent = List.of(Map.of(
                    "type", "text",
                    "text", systemPrompt,
                    "cache_control", Map.of("type", "ephemeral")
            ));

            List<Map<String, Object>> messages = new ArrayList<>();
            for (Map<String, String> msg : history) {
                // Gemini uses "model", Claude uses "assistant"
                String role = "model".equals(msg.get("role")) ? "assistant" : msg.get("role");
                messages.add(Map.of("role", role, "content", msg.get("content")));
            }
            messages.add(Map.of("role", "user", "content", message));

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("max_tokens", 500);
            body.put("system", systemContent);
            body.put("messages", messages);

            String raw = restClient.post()
                    .uri(CLAUDE_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("anthropic-beta", "prompt-caching-2024-07-31")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            return root.path("content").get(0).path("text").asText().strip();

        } catch (Exception e) {
            log.error("Claude API 채팅 실패: {}", e.getMessage());
            return null;
        }
    }
}
