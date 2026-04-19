# Baseball Recommend Project

## 프로젝트 개요
KBO 야구 입문자를 위한 팀 추천 서비스.
성향 설문(8문항 A/B/C/D 4지선다)을 기반으로 KBO 10개 팀 중 맞는 팀을 추천해줌.
추후 RAG, MCP, Claude AI API 등 AI 기능 추가 예정이며 확장성 있게 설계.

## 기술 스택
- Backend: Spring Boot 3.2.4 / Java 17 / Maven
- Frontend: Vue 3 (Vue Router 4, Pinia, Vite)
- DB: MySQL (로컬: localhost:3306/baseball_recommend)
- 스크래핑: Jsoup (KBO 순위 페이지, KBO WebService API)
- AI: 현재 규칙 기반 점수 계산 (`ClaudeClient`) → 추후 Claude API 실제 연동 예정
- 인증: Spring Security + JWT (JJWT 0.12.3)
- 배포: Railway (Backend + MySQL) + Vercel (Frontend) — 완료

## 브랜치 전략
```
main     ← 운영 (Railway 자동 배포)
develop  ← 개발 통합 (기본 작업 브랜치)
feature/* ← 기능 개발 (develop에서 분기)
```
흐름: `feature/*` → `develop` → `main` (PR or merge)

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
│   └── member      회원 도메인 (Member, MemberService, AuthController)
├── infra/claude    추천 엔진 (현재 규칙 기반, 추후 Claude API)
└── global/
    ├── config      CORS (CorsConfig), Security (SecurityConfig), ObjectMapper (RestClientConfig)
    ├── security    JWT (JwtUtil, JwtAuthenticationFilter)
    └── exception   공통 예외처리 (BusinessException, GlobalExceptionHandler, ErrorCode)
```

## 프론트엔드 구조
```
frontend/src/
├── api/
│   ├── http.js        공통 axios 인스턴스 (JWT 인터셉터 포함)
│   ├── surveyApi.js   팀/설문/추천/경기 API 호출
│   └── authApi.js     회원가입/로그인/내 정보 API 호출
├── router/index.js    페이지 라우팅
├── stores/
│   ├── surveyStore.js 설문 상태 관리
│   └── authStore.js   인증 상태 관리 (localStorage 영속화)
└── views/
    ├── HomeView.vue        홈 (히어로 + 팀 미리보기 + 이전 추천 기록)
    ├── SurveyView.vue      설문 (진행바, A/B/C/D 4지선다, 힌트 박스, 슬라이드 전환)
    ├── ResultView.vue      추천 결과 (Top 3, 팬 프로필, URL 공유, 온보딩 CTA)
    ├── OnboardingView.vue  팬 입문 온보딩 (/onboarding/:id)
    ├── TeamsView.vue       팀 목록 (그리드)
    ├── TeamDetailView.vue  팀 상세 (콘텐츠 허브)
    ├── TeamCompareView.vue 팀 비교 (두 팀 나란히)
    ├── StandingView.vue    순위표 (포스트시즌 강조)
    ├── LoginView.vue       로그인
    ├── SignupView.vue      회원가입
    └── MyPageView.vue      내 페이지 (추천 기록 목록)
```

## API 엔드포인트
| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | /api/teams | 불필요 | 전체 팀 목록 |
| GET | /api/teams/{id} | 불필요 | 팀 상세 정보 |
| GET | /api/teams/{id}/players | 불필요 | 팀 소속 선수 목록 |
| GET | /api/survey/questions | 불필요 | 설문 질문 8개 |
| POST | /api/survey/submit | 선택 | 설문 제출 → 추천 결과 반환 (로그인 시 기록 저장) |
| GET | /api/recommend/{id} | 불필요 | 추천 결과 조회 |
| GET | /api/standings | 불필요 | KBO 현재 순위 |
| GET | /api/games/today | 불필요 | 오늘의 경기 일정/결과 |
| GET | /api/games/recent | 불필요 | 최근 7일 경기 결과 (최대 15건) |
| GET | /api/games/form/{teamName} | 불필요 | 팀 최근 5경기 폼 (W/L/D) |
| POST | /api/auth/signup | 불필요 | 회원가입 → JWT 반환 |
| POST | /api/auth/login | 불필요 | 로그인 → JWT 반환 |
| GET | /api/auth/me | 필요 | 내 정보 조회 |
| GET | /api/auth/me/recommendations | 필요 | 내 추천 기록 목록 |

## 구현 완료 기능

### 팀 정보
- KBO 10개 팀 초기 데이터 (DataInitializer로 서버 시작 시 자동 삽입)
- 팀별 데이터: 이름, 도시, 홈구장, 설명, 특징, 색상코드, 창단년도, 우승횟수, 마스코트, 감독, 입문가이드

### 선수 도메인
- Player 엔티티: 팀(FK), 이름, 포지션, 등번호, 설명
- PlayerController → `GET /api/teams/{id}/players`
- DataInitializer에서 팀별 선수 데이터 함께 초기화

### 성향 설문
- 8문항 A/B/C/D 4지선다 설문 (경우의 수 65,536가지)
- Q1~Q4: 야구장·관람 상황 기반 / Q5~Q8: 일상 성향 기반
- 야구 용어 포함 문항(Q2 9회말 2아웃, Q3 직관, Q4 10연패)에 💡 힌트 문구 표시 (입문자 배려)
- `QuestionDto`에 `hint` 필드 추가 (null이면 프론트에서 미표시)
- 프론트: 선택지 2열 그리드 + A/B/C/D 라벨 (모바일 1열)
- 설문 답변 JSON으로 DB 저장 (SurveyResult)

### 팀 추천 (규칙 기반)
- `ClaudeClient`에 규칙 기반 로직 구현 (추후 실제 API로 교체 예정)
- 설문 답변별 10개 팀에 가중치 점수 부여
- 총점 최고 팀 선택 + 맞춤형 추천 이유 생성
- 추천 결과 DB 저장 (RecommendResult: `reason`, `top3_json`, `fan_profile`, `fan_profile_description`, `member_id`)
- 로그인 상태에서 설문 제출 시 member 자동 연동 (비로그인도 호환)

### 추천 결과 URL 공유
- 결과 페이지 라우트: `/result/:id` → 고유 URL로 공유 가능
- 설문 직후: Pinia store 결과 재사용 (API 호출 없음)
- 새로고침 / 직접 접근: `GET /api/recommend/{id}` 로 재조회
- 링크 복사 버튼 (`navigator.clipboard`) + 복사 완료 피드백

### 추천 정확도 강화 (Top 3 + 팬 성향 프로필)
- **Top 3 적합도 분석**: 상위 3개 팀을 퍼센트 바 차트로 시각화 (팀 고유 색상)
- **팀별 짧은 매칭 이유**: 설문 답변과 연결된 한 줄 설명
- **팬 성향 프로필 6종**: 감성 몰입형 / 전략 분석형 / 열정 응원형 / 전통 중시형 / 도전 선호형 / 차분 관조형

### 입문자 온보딩 플로우 (`/onboarding/:id`)
- **진입**: ResultView 하단 팀 색상 CTA 버튼 → `/onboarding/:id`
- **구성 섹션** (단일 스크롤 페이지):
  1. 왜 이 팀인가요? — recommend result의 reason
  2. 꼭 알아야 할 팀 이야기 3가지 — 팀별 정적 데이터 (아이콘 + 제목 + 설명)
  3. 입문자가 먼저 주목할 선수 3명 — `/api/teams/{teamId}/players` 상위 3명
  4. 오늘 볼 수 있는 경기 — `/api/games/today` 팀 필터
  5. 처음 팬이 된다면 이렇게 시작해보세요 — 팀별 3단계 가이드 + beginnerGuide
- 선수/경기 데이터는 `Promise.allSettled`로 병렬 로드

### 팀 상세 콘텐츠 허브 (`/teams/:id`)
- **퀵 네비게이션**: 히어로 하단 섹션 앵커 링크 바
- **기존 섹션**: 핵심 지표, 팀 소개, 팀 특징, 주요 선수, 감독, 역대 기록, 입문가이드
- **신규 섹션** (팀별 정적 데이터 — `TEAM_EXTRA` 객체):
  - 이 팀을 좋아하게 되는 이유, 팀 역사 타임라인, 대표 응원 문화, 영원한 라이벌, 입문자가 꼭 봐야 할 경기, 팬 스타일

### KBO 순위 스크래핑 (Standing)
- 대상: https://www.koreabaseball.com/record/teamrank/teamrankdaily.aspx
- 스크래핑: Jsoup HTTP 요청 (브라우저 없음)
- 갱신 주기: 서버 시작 60초 후 최초 실행, 이후 30분마다 (`@Scheduled`)
- DB 캐시: DB에 데이터 있으면 반환, 없으면 즉시 스크래핑
- 실패 시: DB 유지 (스케줄) 또는 fallback 하드코딩 순위 반환
- 현재 시즌: `CURRENT_SEASON = 2026` 하드코딩

### 경기 일정/결과 스크래핑 (Game)
- 데이터 소스: KBO 공식 WebService API (`koreabaseball.com/ws/Schedule.asmx/GetScheduleList`)
- **DB 캐시** (`game` 테이블): `(game_date, away_team, home_team)` unique 제약 → upsert 방식
- 갱신 주기: 서버 시작 90초 후 최초 실행, 이후 30분마다 (`@Scheduled`)
- 팀 약칭 매핑: `KIA→KIA 타이거즈`, `NC→NC 다이노스` 등 (TEAM_NAME_MAP)
- 실패 시: DB 데이터 유지 (스케줄) 또는 빈 목록 반환

### 회원 시스템 (Member)
- Member 엔티티: email(unique), password(BCrypt), nickname, role(USER/ADMIN), createdAt
- Spring Security + Stateless 세션 + JWT 인증
- `JwtUtil`: JJWT 0.12.3, HS256, 24시간 만료
- `JwtAuthenticationFilter`: Authorization 헤더 파싱 → SecurityContext 설정
- `POST /api/auth/signup`, `POST /api/auth/login` → AuthResponse(token, memberId, email, nickname)
- `GET /api/auth/me`, `GET /api/auth/me/recommendations` → 인증 필요
- 홈 화면: 이전 추천 기록 localStorage 캐시 → 팬 되기 / 결과 보기 버튼

## 개발 원칙
- 확장성 고려한 도메인 분리 구조 유지
- 프론트는 Vue Router + Pinia 구조 유지
- 스크래핑 실패에 대비한 fallback 항상 유지
- 작업은 develop 브랜치에서, 배포 시 main으로 머지

## 실행 방법

### 로컬 개발 (IDE)
```bash
# 백엔드 (포트 8080)
# IntelliJ 활성화된 프로파일: local
cd backend
./mvnw spring-boot:run

# 프론트엔드 (포트 5173)
cd frontend
npm install   # 최초 1회
npm run dev
```

### Docker Compose
```bash
# 최초 1회: 환경변수 파일 생성
cp .env.example .env

# 전체 실행 (MySQL + Backend + Frontend)
docker-compose up --build

# 접속: http://localhost:3000
# MySQL: localhost:3307 (로컬 3306 충돌 방지)
```
- `SPRING_PROFILES_ACTIVE=docker` → `application-docker.yml` 적용 (MySQL 호스트: `mysql`)
- Frontend Nginx가 `/api` → `backend:8080` 프록시 (VITE_API_URL 불필요)

## 환경 설정
| 파일 | 용도 | Git |
|------|------|-----|
| `application.yml` | 공통 설정 (JPA, JWT, logging) | 커밋 O |
| `application-local.yml` | 로컬 DB 접속 (비밀번호 포함) | 커밋 X |
| `application-prod.yml` | Railway 환경변수 참조 | 커밋 O |
| `application-docker.yml` | Docker Compose용 DB 설정 (mysql 호스트) | 커밋 O |

- 로컬 실행: IntelliJ **활성화된 프로파일** 칸에 `local` 입력
- `application-local.yml`: DB URL/username/password 직접 입력

## 배포 정보
- **Backend**: `https://baseballrecommend-production.up.railway.app`
- **Frontend**: Vercel (`baseball-recommend.vercel.app`)
- **DB**: Railway MySQL (Backend 환경변수로 자동 연결)
- Railway 환경변수: `SPRING_PROFILES_ACTIVE=prod`, `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `JWT_SECRET`
- Vercel 환경변수: `VITE_API_URL=https://baseballrecommend-production.up.railway.app/api`

## 주의사항
- `application-local.yml`은 gitignore (비밀번호 포함)
- Railway 배포 시 `JWT_SECRET` 환경변수 필수 (32자 이상)
- Railway 크레딧 소진 시 서비스 자동 중단 (추가 비용 없음)

## 향후 추가 예정 기능
- [x] 추천 결과 URL 공유 (`/result/:id`, 링크 복사 버튼)
- [x] 팀 탐색 완성 (선수 도메인, 입문가이드, 팀 비교 페이지)
- [x] 리그 데이터 확장 (오늘의 경기, 최근 결과, 최근 폼 — KBO WebService API + DB 캐시)
- [x] 추천 정확도 강화 (Top 3 분석 + 팬 성향 프로필 6종)
- [x] 입문자 온보딩 플로우 (`/onboarding/:id` — 추천 이후 5단계 가이드)
- [x] 팀 상세 콘텐츠 허브 (역사 타임라인, 라이벌, 응원문화, 경기 추천, 팬 스타일)
- [x] Railway + Vercel 배포
- [x] 회원 시스템 1단계 (Spring Security + JWT, 로그인/회원가입/마이페이지)
- [x] JWT Refresh Token (Access 1h + Refresh 7d, 자동 재발급)
- [x] 설문 고도화 (8문항 A/B → A/B/C/D 4지선다, 경우의 수 256 → 65,536, 힌트 문구 추가)
- [x] Docker Compose (MySQL + Backend + Frontend, Nginx 프록시, 멀티스테이지 빌드)
- [x] GitHub Actions CI (Backend+MySQL 서비스 컨테이너 / Frontend 빌드 / Docker 이미지 빌드 검증)
- [ ] Redis 캐싱 (순위·경기 데이터 DB 캐시 → Redis TTL 캐시)
- [ ] 소셜 로그인 (카카오/구글 OAuth2)
- [ ] 실제 Claude AI API 연동 (`infra/claude/ClaudeClient` 교체)
- [x] 결과 공유 기능 (카카오톡 공유 + 링크 복사, 결과 페이지)
- [x] 팀별 인기 통계 (`GET /api/recommend/popular-teams`, 홈 화면 바 차트)
- [ ] RAG 기반 추천 (벡터 DB + Claude)
- [ ] MCP 서버 연동
