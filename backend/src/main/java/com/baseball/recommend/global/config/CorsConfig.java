package com.baseball.recommend.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정.
 *
 * <p>이전에는 {@code allowedOriginPattern("*")} + {@code allowCredentials(true)} 였으나,
 * 자격증명을 허용하면서 모든 오리진을 여는 구성은 위험하므로 화이트리스트 방식으로 변경했다.
 * 허용 오리진은 {@code app.cors.allowed-origins} (환경변수 {@code CORS_ALLOWED_ORIGINS}) 로 주입한다.
 */
@Slf4j
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public CorsFilter corsFilter() {
        if (allowedOrigins.isEmpty()) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins 가 비어 있습니다. CORS_ALLOWED_ORIGINS 환경변수를 설정하세요.");
        }
        log.info("CORS 허용 오리진: {}", allowedOrigins);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // setAllowedOriginPatterns 를 쓰면 https://*.vercel.app 같은 와일드카드 패턴도 지정할 수 있다.
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
