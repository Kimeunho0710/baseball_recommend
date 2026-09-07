# AWS 아키텍처 명세 (Plan B — ECS Fargate)

> **이 문서는 Terraform 코드가 아니라 "무엇을 왜 만들어야 하는가"의 명세다.**
> `.tf` 파일은 직접 작성한다. 막히면 이 문서의 해당 항목을 기준으로 질문하면 된다.

---

## 목표 구성

```
                    ┌─────────────── CloudFront ───────────────┐
   사용자 ─────────▶│  /*        → S3 (Vue 정적 파일, OAC)     │
                    │  /api/*    → ALB (선택: 단일 도메인 구성) │
                    └──────────────────┬───────────────────────┘
                                       │
                            ┌──────────▼──────────┐
                            │  ALB (public subnet) │  :443 / :80
                            │  Target Group :8080  │  health: /actuator/health
                            └──────────┬──────────┘
                                       │  SG: ALB → ECS 만 허용
                            ┌──────────▼──────────────────┐
                            │  ECS Fargate Service         │
                            │  Spring Boot task × 1~2      │
                            └──────────┬──────────────────┘
                                       │  SG: ECS → RDS 3306 만 허용
                            ┌──────────▼──────────┐
                            │ RDS MySQL (private) │
                            └─────────────────────┘

  ECR ─(이미지)─▶ ECS        SSM Parameter Store ─(시크릿)─▶ ECS task
  CloudWatch Logs ◀─(로그)─ ECS
```

**VPC**: 2개 AZ, AZ당 public subnet 1개 + private subnet 1개 (총 4개)

---

## 리소스 명세

### 1. 네트워크

| 리소스 | 설정 | 왜 |
|---|---|---|
| VPC | `10.0.0.0/16`, DNS hostnames/support 켬 | RDS 엔드포인트 DNS 해석에 필요 |
| Public subnet × 2 | `10.0.0.0/24`, `10.0.1.0/24` (서로 다른 AZ) | **ALB는 최소 2개 AZ의 서브넷을 요구한다** |
| Private subnet × 2 | `10.0.10.0/24`, `10.0.11.0/24` | RDS 전용. DB subnet group도 2 AZ 필요 |
| Internet Gateway | VPC에 attach | |
| Route table (public) | `0.0.0.0/0` → IGW | |
| Route table (private) | 로컬 라우트만 | RDS는 인터넷에 나갈 일이 없다 |
| **NAT Gateway** | **만들지 않는다** | 월 4만원대로 가장 비싼 리소스. 아래 참고 |

> **NAT를 안 만드는 대신**: Fargate task를 **public subnet에 두고 `assign_public_ip = true`** 로 띄운다.
> ECR 이미지 pull, CloudWatch 로그 전송, Gemini/Groq API 호출이 전부 IGW로 나간다.
> 보안은 **보안그룹으로** 지킨다 — task의 inbound는 ALB SG에서 오는 8080만 허용하므로,
> 퍼블릭 IP가 있어도 외부에서 직접 접근할 수 없다.
> (교과서적 구성은 private subnet + NAT 또는 VPC Endpoint지만, 엔드포인트도 개당 월 $7 정도라 무료가 아니다.
> **면접에서 이 트레이드오프를 설명할 수 있으면 그 자체가 가산점이다.**)

### 2. 보안그룹 — 여기서 제일 많이 막힌다

| SG | Inbound | Outbound |
|---|---|---|
| `alb-sg` | 0.0.0.0/0 → 80, 443 | 전체 허용 |
| `ecs-sg` | **`alb-sg`에서** → 8080 (CIDR가 아니라 SG 참조) | 전체 허용 (ECR/API 호출) |
| `rds-sg` | **`ecs-sg`에서** → 3306 | 없어도 됨 |

> SG를 CIDR가 아니라 **다른 SG를 소스로** 참조하는 것이 핵심이다. IP가 바뀌어도 규칙이 깨지지 않는다.

### 3. ECR

| 항목 | 값 |
|---|---|
| 리포지토리 | `baseball-recommend-backend` |
| 이미지 태그 mutability | `MUTABLE` (초기엔 편하다) |
| **lifecycle policy** | **최근 5개만 보관** ← 안 걸면 이미지가 쌓여 프리티어 500MB를 금방 넘긴다 |
| scan on push | 켜기 (무료) |

> ⚠️ **CPU 아키텍처 주의 — M칩 맥에서 빌드할 때 반드시 걸리는 문제**
>
> Apple Silicon 맥에서 `docker build` 하면 **arm64 이미지**가 만들어진다.
> 그런데 **Fargate 의 기본 `runtimePlatform` 은 X86_64** 라, 그 이미지를 ECR 에 올려 배포하면
> 태스크가 `exec format error` 로 즉시 죽는다. 로그도 거의 안 남아서 원인 찾기 어렵다.
>
> 둘 중 하나를 택한다.
>
> 1. **빌드할 때 플랫폼을 맞춘다** (권장 — CI 와 동일한 결과물)
>    ```bash
>    docker build --platform linux/amd64 -t <ecr-url>:latest ./backend
>    ```
>    맥에서는 에뮬레이션이라 빌드가 느리다.
> 2. **Fargate 를 ARM 으로 돌린다** — task definition 의
>    `runtimePlatform = { cpu_architecture = "ARM64" }`.
>    ARM Fargate 가 **약 20% 싸다.** 대신 베이스 이미지가 arm64 를 지원해야 한다.
>
> 참고로 이 프로젝트의 `backend/Dockerfile` 런타임 베이스는 `eclipse-temurin:17-jre-jammy` 다.
> alpine 태그는 **amd64 만 배포**되어 M칩 맥에서 빌드가 실패하기 때문에 멀티아치인 jammy 로 바꿨다.
> (`docker manifest inspect eclipse-temurin:17-jre-alpine` 로 직접 확인해볼 수 있다.)

### 4. RDS

| 항목 | 값 | 왜 |
|---|---|---|
| 엔진 | MySQL 8.0 | 현재 코드가 MySQL 전용 (Flyway `flyway-mysql`) |
| 인스턴스 | `db.t4g.micro` | 프리티어 대상. t3.micro보다 t4g(ARM)가 싸다 |
| 스토리지 | 20GB gp3, autoscaling 끔 | 프리티어 한도 |
| Multi-AZ | **끔** | 켜면 요금 2배. 포트폴리오엔 불필요 |
| publicly accessible | **끔** | private subnet + SG로만 접근 |
| 백업 보관 | 1일 (또는 0) | 스냅샷 요금 절약 |
| `deletion_protection` | 실습 중엔 끔 | 켜두면 `terraform destroy`가 실패한다 |
| `skip_final_snapshot` | `true` | 위와 동일 |
| 파라미터 그룹 | `character_set_server=utf8mb4`, `time_zone=Asia/Seoul` | **한글 팀 데이터가 깨지는 사고가 여기서 난다** |

### 5. ECS

| 항목 | 값 | 왜 |
|---|---|---|
| 클러스터 | Fargate capacity provider | |
| Task CPU/Memory | **512 / 1024** (0.5 vCPU / 1GB) | 256/512는 Spring Boot 기동 중 OOM 위험. Dockerfile의 `MaxRAMPercentage=70`이 이 1GB를 기준으로 힙을 잡는다 |
| Task role / Execution role | 분리 | execution role = ECR pull + 로그 + SSM 읽기 / task role = 앱이 쓸 AWS 권한(현재 없음) |
| 컨테이너 포트 | 8080 | |
| 로그 드라이버 | `awslogs`, 로그 그룹 retention **7일** | retention 안 걸면 로그가 영구 보관되어 요금이 샌다 |
| desired_count | 1 (평소) / 2 (다중 인스턴스 시연 시) | **2로 올리면 ShedLock이 실제로 동작하는 걸 로그로 보여줄 수 있다** |
| health check grace period | **90초 이상** | Spring Boot 기동 + Flyway 마이그레이션 시간. 짧으면 뜨기도 전에 ALB가 죽였다고 판단해 무한 재시작한다 |

**환경변수 / 시크릿 주입**

`environment`(평문)와 `secrets`(SSM 참조)를 나눠서 넣는다.

| 키 | 방식 | 값 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | environment | `prod` |
| `SPRING_DATASOURCE_URL` | environment | `jdbc:mysql://<rds-endpoint>:3306/baseball_recommend?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `CORS_ALLOWED_ORIGINS` | environment | CloudFront 도메인. **비어 있으면 앱이 기동에 실패한다** |
| `SPRING_DATASOURCE_USERNAME` | secrets (SSM) | |
| `SPRING_DATASOURCE_PASSWORD` | secrets (SSM SecureString) | |
| `JWT_SECRET` | secrets (SSM SecureString) | 32자 이상 |
| `GROQ_API_KEY` 등 | secrets (SSM SecureString) | |

> SSM Parameter Store의 **Standard 티어는 무료**다. Secrets Manager는 시크릿당 월 $0.40이므로 여기선 SSM이 낫다.

### 6. ALB

| 항목 | 값 |
|---|---|
| Target type | **`ip`** (Fargate는 `instance` 불가) |
| Target group port | 8080, protocol HTTP |
| Health check path | **`/actuator/health`** ← Phase ①에서 추가한 그 경로 |
| Health check | interval 30s, timeout 5s, healthy 2 / unhealthy 3, matcher 200 |
| deregistration delay | 30초 (기본 300초는 배포가 느려진다) |

### 7. 프론트엔드 (S3 + CloudFront)

| 항목 | 값 | 왜 |
|---|---|---|
| S3 버킷 | 퍼블릭 액세스 **전부 차단** | |
| CloudFront OAC | Origin Access Control | 버킷을 공개하지 않고 CloudFront만 읽게 한다 (구식 OAI 말고 OAC) |
| Custom error response | **403, 404 → `/index.html` (200)** | **SPA 라우팅 필수.** 없으면 `/result/12` 새로고침 시 403이 뜬다 (기존 `vercel.json`의 rewrites가 하던 역할) |
| 캐시 정책 | `index.html`은 no-cache, 해시 붙은 assets는 장기 캐시 | 배포해도 옛날 화면이 나오는 사고 방지 |

빌드 시점 환경변수: `VITE_API_URL=https://<alb-도메인>/api`

---

## 파일 구조 제안

```
infra/
├── main.tf           provider, terraform block
├── versions.tf       required_version, required_providers
├── variables.tf      region, project_name, db_password 등
├── vpc.tf            VPC / subnet / IGW / route table
├── security.tf       보안그룹 3종
├── ecr.tf            리포지토리 + lifecycle policy
├── rds.tf            subnet group / parameter group / instance
├── ecs.tf            cluster / task definition / service
├── alb.tf            ALB / target group / listener
├── frontend.tf       S3 / CloudFront / OAC
├── iam.tf            execution role / task role
├── ssm.tf            파라미터 (값은 tfvars 말고 콘솔·CLI로 넣는 것도 방법)
├── outputs.tf        alb_dns_name, cloudfront_domain, ecr_repository_url
└── terraform.tfvars  ← .gitignore 대상
```

> **state 파일**: 처음엔 로컬 state로 시작해도 된다. 익숙해지면 S3 backend + DynamoDB 잠금으로 옮기는 걸
> 별도 단계로 해보면 좋다 — 이것도 면접 단골 주제다.

---

## 구축 순서 (의존성 순)

각 단계마다 **검증하고 넘어간다.** 한 번에 다 만들고 `apply` 하면 원인 찾기가 지옥이 된다.

| # | 단계 | 검증 방법 |
|---|---|---|
| 0 | **AWS Budgets 예산 알람 먼저 설정** (월 3만원 등), 루트 계정 MFA, 작업용 IAM 사용자 생성 | 알람 메일 수신 확인 |
| 1 | VPC / subnet / IGW / route table | 콘솔에서 서브넷이 각각 다른 AZ인지 확인 |
| 2 | 보안그룹 3종 | |
| 3 | ECR | `aws ecr get-login-password`로 로그인 → 로컬 이미지 push 성공 |
| 4 | RDS | 아직 붙을 수 없다. 콘솔에서 `available` 상태만 확인 |
| 5 | SSM 파라미터 | `aws ssm get-parameter --name ... --with-decryption` |
| 6 | IAM role 2종 | |
| 7 | ALB + Target Group | ALB DNS 접속 시 **503이 뜨면 정상** (타깃이 아직 없음) |
| 8 | ECS 클러스터 + Task Definition + Service | **여기가 최대 난관.** 아래 트러블슈팅 참고 |
| 9 | S3 + CloudFront | `npm run build` → `aws s3 sync` → CloudFront 도메인 접속 |
| 10 | `CORS_ALLOWED_ORIGINS`를 CloudFront 도메인으로 갱신 후 서비스 재배포 | 브라우저 콘솔에 CORS 에러 없음 |

---

## 자주 막히는 지점

| 증상 | 원인 |
|---|---|
| Task가 `PENDING` → `STOPPED` 반복, 로그도 없음 | execution role에 ECR pull / 로그 생성 권한 없음. 또는 public subnet인데 `assign_public_ip=false`라 ECR에 못 나감 |
| Task는 뜨는데 ALB가 계속 unhealthy | health check path 오타, target type이 `ip`가 아님, `ecs-sg`가 `alb-sg`로부터 8080을 안 열어줌, grace period가 짧음 |
| 앱 로그에 `CORS_ALLOWED_ORIGINS ... 비어 있습니다` | Phase ①의 fail-fast. 환경변수 누락 |
| 앱 로그에 Flyway/`SchemaManagementException` | RDS에 DB(`baseball_recommend`)가 안 만들어짐 → RDS `db_name` 파라미터 확인 |
| 팀 이름이 `???`로 저장됨 | RDS 파라미터 그룹의 `character_set_server` 미설정 |
| CloudFront에서 새로고침 시 403 | custom error response 미설정 |
| 배포했는데 화면이 그대로 | CloudFront 캐시. `create-invalidation --paths "/*"` |
| `terraform destroy` 실패 | RDS deletion protection, S3 버킷에 객체 남음, ENI가 아직 안 떨어짐 |

---

## 비용 관리 — 실습 전에 반드시 읽을 것

서울 리전(ap-northeast-2) 기준 **대략적인** 월 요금이다. 정확한 값은
[AWS Pricing Calculator](https://calculator.aws)에서 직접 확인할 것.

| 리소스 | 상시 가동 시 |
|---|---|
| ALB | 약 $17 (시간당 과금 — **트래픽이 0이어도 나간다**) |
| Fargate 0.5vCPU/1GB × 1 task | 약 $18 |
| RDS db.t4g.micro | 프리티어 12개월 무료 / 이후 약 $14 |
| S3 + CloudFront | 사실상 무료 (CloudFront 월 1TB 영구 무료) |
| ECR | 500MB까지 무료 |
| **NAT Gateway (안 만듦)** | (만들면 +$40) |
| **합계** | **월 $35 안팎 ≈ 5만원** |

**핵심 전략 — 상시 켜두지 않는다.**

포트폴리오는 24시간 떠 있을 필요가 없다. 요금의 대부분(ALB + Fargate)은 시간당 과금이므로:

```bash
# 실습/시연이 끝나면
terraform destroy

# 면접 전날 다시
terraform apply
```

**이게 Terraform을 쓰는 진짜 이유다.** 콘솔로 만들었으면 지우고 다시 만드는 게 무서워서
계속 켜두게 되고, 그러면 돈이 샌다. `destroy` → `apply`가 5분이면 끝난다는 걸 확인하는 순간
IaC의 가치를 몸으로 알게 된다. **그리고 그 경험담이 면접 답변이 된다.**

- RDS만은 destroy하면 데이터가 사라지니, 스냅샷을 남기거나 `DataInitializer`가 팀 데이터를 다시 채우는 것을 이용한다 (설문/추천 기록은 사라진다)
- 매일 아침 요금 확인: `aws ce get-cost-and-usage` 또는 콘솔 Cost Explorer
- **AWS Budgets 알람은 첫날 반드시 설정한다**

---

## Phase ①이 여기에 어떻게 쓰이는가

| Phase ① 변경 | Plan B에서의 역할 |
|---|---|
| `/actuator/health` | ALB Target Group health check 경로. **없으면 서비스가 절대 healthy가 안 된다** |
| ShedLock | Fargate task를 2개로 늘리는 순간 필수. 롤링 배포 중 신·구 task가 겹칠 때도 동작 |
| Flyway | task가 여러 개 떠도 마이그레이션은 한 번만 적용된다(Flyway 자체 잠금). `ddl-auto: update`였다면 DDL이 충돌했다 |
| CORS 화이트리스트 | CloudFront와 ALB의 오리진이 다르므로 실제로 필요. 도메인을 정확히 지정 |
| `MaxRAMPercentage=70` | Fargate 1GB 한도 안에서 힙을 잡는다 |
