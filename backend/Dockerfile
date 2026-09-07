# ─── build stage ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ─── runtime stage ───────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 컨테이너 메모리 한도를 인식해 힙을 잡는다 (t3.micro 1GB 환경 OOM 방지)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=40 -Duser.timezone=Asia/Seoul"

# root 로 실행하지 않는다
RUN addgroup -S app && adduser -S -G app app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080

# actuator 헬스체크 (docker-compose depends_on / ECS·ALB Target Group 과 동일한 경로)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
