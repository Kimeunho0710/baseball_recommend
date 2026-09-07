# ─── build stage ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ─── runtime stage ───────────────────────────────────────────
# alpine(musl) 빌드의 Temurin 은 linux/amd64 만 배포된다 → Apple Silicon(arm64) 에서 빌드 불가.
# jammy 태그는 amd64/arm64 멀티아치라 로컬(M칩 맥)과 CI/배포(amd64) 양쪽에서 모두 빌드된다.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 헬스체크용 curl (ubuntu 기반 이미지에는 기본 포함되어 있지 않다)
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# 컨테이너 메모리 한도를 인식해 힙을 잡는다 (1GB 환경 OOM 방지)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=40 -Duser.timezone=Asia/Seoul"

# root 로 실행하지 않는다
RUN groupadd --system app && useradd --system --gid app --shell /usr/sbin/nologin app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080

# actuator 헬스체크 (docker-compose depends_on / ECS·ALB Target Group 과 동일한 경로)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
