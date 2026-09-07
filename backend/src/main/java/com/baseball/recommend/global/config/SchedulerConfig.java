package com.baseball.recommend.global.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 스케줄러 분산 락 설정.
 *
 * <p>KBO 순위/경기 스크래핑은 30분마다 도는 {@code @Scheduled} 작업이다.
 * 인스턴스를 2대 이상으로 늘리면(EC2 오토스케일링, ECS task 다중화) 같은 스크래핑이
 * 동시에 실행되어 KBO 서버에 중복 요청이 나가고 upsert 경합이 발생한다.
 * ShedLock 이 DB({@code shedlock} 테이블)를 이용해 한 시점에 한 인스턴스만 실행하도록 보장한다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        // 인스턴스 간 시계 오차를 피하기 위해 DB 시간을 기준으로 잠금
                        .usingDbTime()
                        .build()
        );
    }
}
