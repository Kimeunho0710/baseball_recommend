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
public class GroqClient {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GroqClient(ObjectMapper objectMapper, @Value("${groq.api.key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    /**
     * @return AI reply text, or null if API key missing/failed
     */
    public String chatReply(String systemPrompt, List<Map<String, String>> history, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            for (Map<String, String> msg : history) {
                // Gemini uses "model", OpenAI-compatible APIs use "assistant"
                String role = "model".equals(msg.get("role")) ? "assistant" : msg.get("role");
                messages.add(Map.of("role", role, "content", msg.get("content")));
            }
            messages.add(Map.of("role", "user", "content", message));

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("max_tokens", 500);
            body.put("temperature", 0.8);

            String raw = restClient.post()
                    .uri(GROQ_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            return root.path("choices").get(0).path("message").path("content").asText().strip();

        } catch (Exception e) {
            log.error("Groq API 채팅 실패: {}", e.getMessage());
            return null;
        }
    }
}
