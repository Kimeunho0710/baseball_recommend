# Baseball Recommend Project

## 프로젝트 개요
KBO 야구 입문자를 위한 팀 추천 서비스.
성향 설문(8문항 A/B)을 기반으로 KBO 10개 팀 중 맞는 팀을 추천해줌.
추후 RAG, MCP, Claude AI API 등 AI 기능 추가 예정이며 확장성 있게 설계.

## 기술 스택
- Backend: Spring Boot 3.2.4 / Java 17 / Maven
- Frontend: Vue 3 (Vue Router 4, Pinia, Vite)
- DB: MySQL (로컬: localhost:3306/baseball_recommend)
- AI: 현재 규칙 기반 점수 계산 → 추후 Claude API 실제 연동 예정
- 배포: AWS (EC2, RDS, S3, CloudFront) — 예정

## 패키지 구조
```
com.baseball.recommend
├── domain/
│   ├── survey      설문 도메인 (질문 제공, 답변 저장)
│   ├── recommend   추천 도메인 (결과 저장 및 조회)
│   ├── team        팀 정보 도메인 (10개 팀 데이터)
│   ├── standing    순위 도메인 (KBO 스크래핑 + DB 캐시)
│   └── member      회원 도메인 (미구현, 추후 추가)
├── infra/claude    추천 엔진 (현재 규칙 기반, 추후 Claude API)
└── global/
    ├── config      CORS, ObjectMapper 설정
    └── exception   공통 예외처리 (BusinessException, GlobalExceptionHandler)
```

## 프론트엔드 구조
```
frontend/src/
├── api/surveyApi.js     모든 API 호출 함수
├── router/index.js      페이지 라우팅
├── stores/surveyStore.js Pinia 상태 관리
└── views/
    ├── HomeView.vue      홈 (히어로 + 팀 미리보기)
    ├── SurveyView.vue    설문 (진행바, A/B 선택, 슬라이드 전환)
    ├── ResultView.vue    추천 결과
    ├── TeamsView.vue     팀 목록 (그리드)
    ├── TeamDetailView.vue 팀 상세 (통계, 특징, 감독)
    └── StandingView.vue  순위표 (포스트시즌 강조)
```

## API 엔드포인트
| Method | URL | 설명 |
|--------|-----|------|
| GET | /api/teams | 전체 팀 목록 |
| GET | /api/teams/{id} | 팀 상세 정보 |
| GET | /api/survey/questions | 설문 질문 8개 |
| POST | /api/survey/submit | 설문 제출 → 추천 결과 반환 |
| GET | /api/recommend/{id} | 추천 결과 조회 |
| GET | /api/standings | KBO 현재 순위 |

## 구현 완료 기능

### 팀 정보
- KBO 10개 팀 초기 데이터 (DataInitializer로 서버 시작 시 자동 삽입)
- 팀별 데이터: 이름, 도시, 홈구장, 설명, 특징, 색상코드, 창단년도, 우승횟수, 마스코트, 감독

### 성향 설문
- 8문항 A/B 선택형 설문
- 외향/내향, 논리/감성, 계획/즉흥, 도전/안정 등 8개 차원 측정
- 설문 답변 JSON으로 DB 저장 (SurveyResult)

### 팀 추천 (규칙 기반)
- 설문 답변별 10개 팀에 가중치 점수 부여
- 총점 최고 팀 선택
- 팀별 맞춤형 추천 이유 템플릿 생성
- 추천 결과 DB 저장 (RecommendResult)

### 추천 결과 URL 공유
- 결과 페이지 라우트를 `/result/:id`로 변경 → 고유 URL로 접근 가능
- 설문 직후 이동: Pinia store 결과 재사용 (불필요한 API 호출 없음)
- 새로고침 / 공유 링크 직접 접근: `GET /api/recommend/{id}` 로 결과 재조회
- 로딩 / 에러 상태 UI 처리
- 링크 복사 버튼 (`navigator.clipboard`) + 복사 완료 피드백

### KBO 순위 스크래핑
- 대상 URL: https://www.koreabaseball.com/record/teamrank/teamrankdaily.aspx
- 스크래핑 방식: Jsoup HTTP 요청 (브라우저 없음, Playwright 제거됨)
- 갱신 주기: 서버 시작 1분 후 최초 실행, 이후 30분마다 자동 갱신 (@Scheduled)
- DB 캐시: DB에 데이터 있으면 DB 반환, 없으면 즉시 스크래핑
- 스크래핑 실패 시: DB 유지 (스케줄) 또는 fallback 하드코딩 순위 반환

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
- application.yml에 DB 비밀번호 평문 포함 → GitHub 업로드 금지
- .gitignore에 backend/src/main/resources/application.yml 추가 필요

## 향후 추가 예정 기능
- [x] 추천 결과 URL 공유 (`/result/:id`, 링크 복사 버튼)
- [ ] 실제 Claude AI API 연동 (infra/claude/ClaudeClient 교체)
- [ ] 결과 공유 기능 (카카오톡 공유)
- [ ] 오늘의 경기 일정 스크래핑
- [ ] 팀별 인기 통계
- [ ] 선수 정보 도메인
- [ ] 회원 시스템 (domain/member, Spring Security + JWT)
- [ ] RAG 기반 추천 (벡터 DB + Claude)
- [ ] MCP 서버 연동
