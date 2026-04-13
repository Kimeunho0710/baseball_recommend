# Baseball Recommend Project

## 프로젝트 개요
KBO 야구 입문자를 위한 팀 추천 서비스.
성향 설문(8문항 A/B)을 기반으로 KBO 10개 팀 중 맞는 팀을 추천해줌.
추후 RAG, MCP, Claude AI API 등 AI 기능 추가 예정이며 확장성 있게 설계.

## 기술 스택
- Backend: Spring Boot 3.2.4 / Java 17 / Maven
- Frontend: Vue 3 (Vue Router 4, Pinia, Vite)
- DB: MySQL (로컬: localhost:3306/baseball_recommend)
- 스크래핑: Jsoup (KBO 순위 페이지, KBO WebService API)
- AI: 현재 규칙 기반 점수 계산 (`ClaudeClient`) → 추후 Claude API 실제 연동 예정
- 배포: AWS (EC2, RDS, S3, CloudFront) — 예정

## 패키지 구조
```
com.baseball.recommend
├── domain/
│   ├── survey      설문 도메인 (질문 제공, 답변 저장)
│   ├── recommend   추천 도메인 (결과 저장 및 조회)
│   ├── team        팀 정보 도메인 (10개 팀 데이터, DataInitializer)
│   ├── player      선수 도메인 (팀별 선수 목록)
│   ├── standing    순위 도메인 (KBO 스크래핑 + DB 캐시)
│   ├── game        경기 도메인 (KBO WebService API + DB 캐시)
│   └── member      회원 도메인 (미구현, 추후 추가)
├── infra/claude    추천 엔진 (현재 규칙 기반, 추후 Claude API)
└── global/
    ├── config      CORS 설정 (CorsConfig), ObjectMapper 빈 (RestClientConfig)
    └── exception   공통 예외처리 (BusinessException, GlobalExceptionHandler, ErrorCode)
```

## 프론트엔드 구조
```
frontend/src/
├── api/surveyApi.js      모든 API 호출 함수
├── router/index.js       페이지 라우팅
├── stores/surveyStore.js Pinia 상태 관리
└── views/
    ├── HomeView.vue        홈 (히어로 + 팀 미리보기)
    ├── SurveyView.vue      설문 (진행바, A/B 선택, 슬라이드 전환)
    ├── ResultView.vue      추천 결과 (Top 3, 팬 프로필, URL 공유)
    ├── TeamsView.vue       팀 목록 (그리드)
    ├── TeamDetailView.vue  팀 상세 (통계, 특징, 선수, 감독, 입문가이드)
    ├── TeamCompareView.vue 팀 비교 (두 팀 나란히)
    └── StandingView.vue    순위표 (포스트시즌 강조)
```

## API 엔드포인트
| Method | URL | 설명 |
|--------|-----|------|
| GET | /api/teams | 전체 팀 목록 |
| GET | /api/teams/{id} | 팀 상세 정보 |
| GET | /api/teams/{id}/players | 팀 소속 선수 목록 |
| GET | /api/survey/questions | 설문 질문 8개 |
| POST | /api/survey/submit | 설문 제출 → 추천 결과 반환 |
| GET | /api/recommend/{id} | 추천 결과 조회 |
| GET | /api/standings | KBO 현재 순위 |
| GET | /api/games/today | 오늘의 경기 일정/결과 |
| GET | /api/games/recent | 최근 7일 경기 결과 (최대 15건) |
| GET | /api/games/form/{teamName} | 팀 최근 5경기 폼 (W/L/D) |

## 구현 완료 기능

### 팀 정보
- KBO 10개 팀 초기 데이터 (DataInitializer로 서버 시작 시 자동 삽입)
- 팀별 데이터: 이름, 도시, 홈구장, 설명, 특징, 색상코드, 창단년도, 우승횟수, 마스코트, 감독, 입문가이드

### 선수 도메인
- Player 엔티티: 팀(FK), 이름, 포지션, 등번호, 설명
- PlayerController → `GET /api/teams/{id}/players`
- DataInitializer에서 팀별 선수 데이터 함께 초기화

### 성향 설문
- 8문항 A/B 선택형 설문
- 외향/내향, 논리/감성, 계획/즉흥, 도전/안정 등 8개 차원 측정
- 설문 답변 JSON으로 DB 저장 (SurveyResult)

### 팀 추천 (규칙 기반)
- `ClaudeClient`에 규칙 기반 로직 구현 (추후 실제 API로 교체 예정)
- 설문 답변별 10개 팀에 가중치 점수 부여
- 총점 최고 팀 선택 + 맞춤형 추천 이유 생성
- 추천 결과 DB 저장 (RecommendResult: `reason`, `top3_json`, `fan_profile`, `fan_profile_description`)

### 추천 결과 URL 공유
- 결과 페이지 라우트: `/result/:id` → 고유 URL로 공유 가능
- 설문 직후: Pinia store 결과 재사용 (API 호출 없음)
- 새로고침 / 직접 접근: `GET /api/recommend/{id}` 로 재조회
- 링크 복사 버튼 (`navigator.clipboard`) + 복사 완료 피드백

### 추천 정확도 강화 (Top 3 + 팬 성향 프로필)
- **Top 3 적합도 분석**: 상위 3개 팀을 퍼센트 바 차트로 시각화 (팀 고유 색상)
- **팀별 짧은 매칭 이유**: 설문 답변과 연결된 한 줄 설명
- **팬 성향 프로필 6종**: 감성 몰입형 / 전략 분석형 / 열정 응원형 / 전통 중시형 / 도전 선호형 / 차분 관조형

### KBO 순위 스크래핑 (Standing)
- 대상: https://www.koreabaseball.com/record/teamrank/teamrankdaily.aspx
- 스크래핑: Jsoup HTTP 요청 (브라우저 없음)
- 갱신 주기: 서버 시작 60초 후 최초 실행, 이후 30분마다 (`@Scheduled`)
- DB 캐시: DB에 데이터 있으면 반환, 없으면 즉시 스크래핑
- 실패 시: DB 유지 (스케줄) 또는 fallback 하드코딩 순위 반환
- 현재 시즌: `CURRENT_SEASON = 2026` 하드코딩

### 경기 일정/결과 스크래핑 (Game)
- 데이터 소스: KBO 공식 WebService API (`koreabaseball.com/ws/Schedule.asmx/GetScheduleList`)
  - 파라미터: `leId=1, srIdList=0,9,6 (정규시즌), seasonId, gameMonth, teamId`
  - 이번달 1회 POST 요청으로 전체 월 데이터 수집 (오늘+7일이 다음달이면 다음달도 추가 수집)
  - ※ 이전에 사용하던 네이버 스포츠 API(`gameCenter/gameList.nhn`)는 2026년 기준 404 폐기됨
- **DB 캐시** (`game` 테이블): `(game_date, away_team, home_team)` unique 제약 → upsert 방식
- 갱신 주기: 서버 시작 90초 후 최초 실행, 이후 30분마다 (`@Scheduled`)
- DB에 데이터 있으면 DB 반환, 없으면 즉시 스크래핑 후 DB 저장
- 팀 약칭 매핑: `KIA→KIA 타이거즈`, `NC→NC 다이노스` 등 약칭 → 정식명 (TEAM_NAME_MAP)
- 실패 시: DB 데이터 유지 (스케줄) 또는 빈 목록 반환

## 개발 원칙
- 확장성 고려한 도메인 분리 구조 유지
- 프론트는 Vue Router + Pinia 구조 유지
- 스크래핑 실패에 대비한 fallback 항상 유지

## 실행 방법
```bash
# 백엔드 (포트 8080)
cd backend
./mvnw spring-boot:run

# 프론트엔드 (포트 5173)
cd frontend
npm install   # 최초 1회
npm run dev
```

## 주의사항
- `application.yml`에 DB 비밀번호 평문 포함 → GitHub 업로드 금지
- `.gitignore`에 `backend/src/main/resources/application.yml` 추가 필요

## 향후 추가 예정 기능
- [x] 추천 결과 URL 공유 (`/result/:id`, 링크 복사 버튼)
- [x] 팀 탐색 완성 (선수 도메인, 입문가이드, 팀 비교 페이지)
- [x] 리그 데이터 확장 (오늘의 경기, 최근 결과, 최근 폼 — KBO WebService API + DB 캐시)
- [x] 추천 정확도 강화 (Top 3 분석 + 팬 성향 프로필 6종)
- [ ] 실제 Claude AI API 연동 (`infra/claude/ClaudeClient` 교체)
- [ ] 결과 공유 기능 (카카오톡 공유)
- [ ] 팀별 인기 통계
- [ ] 회원 시스템 (`domain/member`, Spring Security + JWT)
- [ ] RAG 기반 추천 (벡터 DB + Claude)
- [ ] MCP 서버 연동
