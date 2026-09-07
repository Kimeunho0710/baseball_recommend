# AWS 이관 준비 (Phase ①) — 무엇이 왜 바뀌었는가

AWS(ECS Fargate + ALB + RDS + S3/CloudFront)로 옮기기 전에,
**앱이 클라우드 환경에서 정상 동작하기 위한 최소 조건**을 코드에 반영한 단계다.
AWS 콘솔/Terraform 작업은 Phase ②(`01-architecture.md`)부터이며, 이 문서는 그 전제 조건을 정리한다.

---

## 1. 헬스체크 엔드포인트 추가

| | |
|---|---|
| 변경 | `spring-boot-starter-actuator` 추가, `GET /actuator/health` 공개 |
| 이유 | EC2/ALB/ECS 어디로 가든 "이 인스턴스가 트래픽을 받아도 되는가"를 판단할 경로가 필요하다. 지금까지는 그 경로가 없어서 헬스체크를 붙일 수 없었다. |

- `SecurityConfig`에서 `/actuator/health`, `/actuator/info`만 `permitAll`
- `management.endpoints.web.exposure.include=health,info` — 그 외 엔드포인트(env, beans 등)는 노출하지 않음
- `show-details: never` — DB 접속 정보 등이 응답에 새지 않도록

**확인 방법**
```bash
curl -s localhost:8080/actuator/health
# {"status":"UP"}
```

---

## 2. `@Scheduled` 중복 실행 방지 (ShedLock)

| | |
|---|---|
| 변경 | `StandingService.scheduledRefresh`, `GameService.scheduledRefresh`에 `@SchedulerLock` 적용 + `shedlock` 테이블 |
| 이유 | 두 메서드는 30분마다 KBO를 스크래핑한다. 인스턴스가 2대가 되는 순간 같은 스크래핑이 동시에 돌아 KBO 서버에 중복 요청이 나가고, `game` 테이블 upsert에서 경합이 발생한다. |

- `SchedulerConfig`에서 `JdbcTemplateLockProvider` 등록, `usingDbTime()`으로 인스턴스 간 시계 오차 회피
- `lockAtMostFor = PT10M` — 인스턴스가 죽어도 10분 뒤 락이 풀림
- `lockAtLeastFor = PT1M` — 작업이 너무 빨리 끝나도 1분간은 다른 인스턴스가 재실행하지 못함

> ECS Fargate 에서 task 를 2개로 늘리는 순간, 그리고 롤링 배포로 신·구 task 가 겹치는 순간부터 필수다.
> task 를 2개로 띄우고 CloudWatch 로그에서 한쪽만 스크래핑하는 것을 확인하면 동작을 직접 검증할 수 있다.

---

## 3. DB 스키마 관리: `ddl-auto: update` → Flyway + `validate`

| | |
|---|---|
| 변경 | `db/migration/V1__init_schema.sql`, `V2__shedlock.sql` 추가. `ddl-auto: validate` |
| 이유 | `update`는 애플리케이션이 운영 DB 스키마를 직접 바꾼다. 컬럼 삭제·타입 변경을 감지하지 못하고, 인스턴스 2대가 동시에 뜨면 DDL이 충돌한다. RDS로 가면 더 위험하다. |

- `V1__init_schema.sql`은 기존 JPA 엔티티에서 추출한 스키마와 동일 (Hibernate 스키마 생성 결과 기준)
- `baseline-on-migrate: true` — **기존 운영 DB에는 V1이 실행되지 않고 baseline 처리**되며 V2만 적용된다
- 신규 RDS(빈 DB)에는 V1 → V2 순서로 전부 실행된다
- 이후 스키마 변경은 반드시 `V3__xxx.sql` 파일 추가로만 한다

> CI(GitHub Actions)가 MySQL 컨테이너에 Flyway를 실행한 뒤 Hibernate `validate`를 돌린다.
> 즉 **마이그레이션과 엔티티가 어긋나면 CI에서 잡힌다.**

---

## 4. CORS 화이트리스트

| | |
|---|---|
| 변경 | `allowedOriginPattern("*")` → `app.cors.allowed-origins` (환경변수 `CORS_ALLOWED_ORIGINS`) |
| 이유 | `allowCredentials(true)`와 와일드카드 오리진을 같이 쓰는 것은 위험하다. Plan A에서는 프론트(S3/CloudFront)와 백엔드(EC2)의 오리진이 달라 CORS가 실제로 동작해야 하므로, 여기서 도메인을 정확히 지정한다. |

**⚠️ 배포 시 필수** — `CORS_ALLOWED_ORIGINS`가 비어 있으면 애플리케이션이 기동에 실패한다(의도된 fail-fast).

```
CORS_ALLOWED_ORIGINS=https://d123abc.cloudfront.net
```

로컬/도커는 `http://localhost:5173,http://localhost:3000`이 기본값으로 들어가 있어 별도 설정이 필요 없다.

---

## 5. 컨테이너 이미지 정리

- `JAVA_OPTS`에 `-XX:MaxRAMPercentage=70` — **t3.micro(1GB)에서 JVM이 컨테이너 메모리 한도를 인식**하도록. 없으면 힙을 과하게 잡아 OOM으로 죽는다.
- 비 root 사용자(`app`)로 실행
- 이미지 자체에 `HEALTHCHECK` 내장

---

## 배포 시 필요한 환경변수 (Phase ② 이후 사용)

| 변수 | 필수 | 비고 |
|---|:--:|---|
| `SPRING_PROFILES_ACTIVE` | ✅ | `prod` |
| `SPRING_DATASOURCE_URL` | ✅ | RDS 엔드포인트 |
| `SPRING_DATASOURCE_USERNAME` | ✅ | |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | SSM Parameter Store(SecureString) 권장 |
| `JWT_SECRET` | ✅ | 32자 이상, SSM 권장 |
| `CORS_ALLOWED_ORIGINS` | ✅ | CloudFront 도메인. 미설정 시 기동 실패 |
| `GROQ_API_KEY` | – | AI 코치 1순위 |
| `ANTHROPIC_API_KEY` | – | 2순위 |
| `GEMINI_API_KEY` | – | 3순위 + 추천 텍스트 생성 |

---

## 로컬 검증 절차

```bash
cp .env.example .env
docker compose up --build

curl -s localhost:8080/actuator/health          # {"status":"UP"}
docker compose exec mysql mysql -uroot -ppassword baseball_recommend \
  -e "select version, description, success from flyway_schema_history;"
```

`flyway_schema_history`에 `1 / init schema`, `2 / shedlock`이 `success=1`로 남으면 정상이다.
