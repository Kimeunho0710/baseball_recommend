# 실습 과제 체크리스트

> 📌 **명령어 하나하나까지 순서대로 따라가려면 `04-step-by-step.md` 를 본다.**
> 이 문서는 "무엇을 왜 하는가"의 요약이고, 04 는 "어떻게 하는가"의 전체 절차다.

> 규칙: **각 과제는 직접 한다.** 막히면 증상과 에러 메시지를 가지고 물어본다.
> 답을 바로 받지 말고, "어디를 봐야 하는지"부터 받는 편이 남는 게 많다.
>
> 각 과제에는 **학습 포인트**가 달려 있다. 면접에서 그대로 나올 수 있는 질문이므로,
> 과제를 끝낸 뒤 그 질문에 **말로 설명할 수 있는지** 스스로 확인한다.

---

## 과제 0. 이번 변경을 로컬에서 눈으로 확인하기

CI가 통과했다는 것만으로는 내 손에 남는 게 없다. 직접 돌려보고 확인한다.

```bash
cd baseball_recommend
cp .env.example .env
docker compose up --build
```

**확인할 것**

```bash
# 1) 헬스체크가 UP 인가
curl -s localhost:8080/actuator/health

# 2) Flyway 가 마이그레이션을 적용했는가
docker compose exec mysql mysql -uroot -ppassword baseball_recommend \
  -e "select installed_rank, version, description, success from flyway_schema_history;"

# 3) ShedLock 테이블이 생겼는가 (아직 행은 비어 있다)
docker compose exec mysql mysql -uroot -ppassword baseball_recommend \
  -e "select * from shedlock;"

# 4) 서버 시작 60~90초 뒤 스케줄러가 돌면 shedlock 에 행이 생긴다. 다시 조회해볼 것
```

**직접 해볼 실험 — ShedLock이 진짜 동작하는지**

```bash
docker compose -f docker-compose.scale.yml up --build --scale backend=2
```
(`docker-compose.scale.yml`은 backend의 `ports` 매핑만 뺀 실험용 파일이다.
호스트 포트를 고정하면 컨테이너 2개가 같은 8080을 잡으려 해서 뜨지 않는다.)
두 컨테이너 로그를 보면 **한쪽만** `[스케줄] KBO 순위 갱신 시작`을 찍는다.
`@SchedulerLock`을 주석 처리하고 다시 해보면 양쪽 다 찍는다 — **이 차이를 직접 보는 게 핵심이다.**

> **학습 포인트**
> - `ddl-auto: update`와 Flyway의 차이는 무엇이고, 운영 환경에서 왜 후자여야 하는가?
> - `flyway_schema_history`의 `checksum` 컬럼은 무엇을 막아주는가?
>   (이미 적용된 마이그레이션 파일을 수정하면 어떻게 되는지 직접 해볼 것)
> - 스케줄러 중복 실행을 막는 방법에는 ShedLock 말고 또 뭐가 있는가? (리더 선출, 전용 스케줄러 인스턴스, EventBridge)

---

## 과제 1. AWS 계정 준비 — **리소스를 만들기 전에 반드시 먼저**

| # | 할 일 | 왜 |
|---|---|---|
| 1 | 루트 계정에 **MFA 활성화** | 루트는 모든 걸 할 수 있다. 탈취당하면 끝이다 |
| 2 | 작업용 **IAM 사용자** 생성, 루트는 그 뒤로 안 쓴다 | 실무에서 루트로 작업하면 그 자체가 지적 사항 |
| 3 | IAM 사용자에 **MFA + AdministratorAccess** (실습용) | 실무라면 최소 권한이지만 학습 단계에선 관리자로 시작해도 된다 |
| 4 | **AWS Budgets 예산 알람** — 월 3만원, 50%/80%/100% 지점에서 메일 | **이걸 안 하면 요금 사고가 난다** |
| 5 | AWS CLI 설치 + `aws configure` | |
| 6 | 리전을 **서울(ap-northeast-2)** 로 고정 | |

**검증**
```bash
aws sts get-caller-identity      # IAM 사용자 ARN 이 나와야 한다 (root 가 아니라)
aws configure get region         # ap-northeast-2
```

> **학습 포인트**
> - IAM User / IAM Role / IAM Policy는 각각 무엇이고 언제 쓰는가?
> - Access Key를 코드나 깃에 넣으면 안 되는 이유, 그리고 대안은? (IAM Role, OIDC)
> - "최소 권한 원칙(least privilege)"을 이 프로젝트에 적용한다면 어디에?

---

## 과제 2. Terraform 첫걸음 — VPC까지만

한 번에 다 만들지 않는다. **VPC와 서브넷만** 만들고 `apply` → 콘솔 확인 → `destroy` 를 한 사이클 돌린다.

> **Terraform 을 처음 쓴다면 `docs/aws/03-terraform-basics.md` 를 먼저 읽는다.**
> 거기에 개념 설명과 VPC 1개짜리 최소 예제, `init/plan/apply/destroy` 한 사이클이 있다.
> 그 사이클을 돌려본 뒤에 이 과제로 돌아온다.

```bash
terraform -version    # 설치 확인
mkdir infra && cd infra
```

`docs/aws/01-architecture.md`의 **"1. 네트워크"** 표를 보고 직접 작성한다.
만들 것: VPC 1개, public subnet 2개(서로 다른 AZ), private subnet 2개, IGW, 라우트 테이블.

```bash
terraform init
terraform plan     # 만들어질 리소스 개수를 먼저 읽어본다
terraform apply
terraform destroy  # 지우는 것까지 해봐야 한 사이클이다
```

**검증**
- 콘솔 VPC 대시보드에서 서브넷 4개가 **서로 다른 AZ에 2개씩** 배치됐는지
- public subnet의 라우트 테이블에 `0.0.0.0/0 → igw-...` 가 있는지
- `terraform destroy` 후 콘솔에 아무것도 안 남았는지

> **학습 포인트**
> - `terraform plan`과 `apply`의 차이, 그리고 `plan`을 왜 항상 먼저 보는가?
> - **terraform.tfstate가 뭘 하는 파일이고, 이걸 깃에 올리면 안 되는 이유는?**
>   (팀 작업이면 어디에 둬야 하는가 → S3 backend + DynamoDB 잠금)
> - ALB가 서브넷을 최소 2개 AZ로 요구하는 이유는? (가용영역 장애 대비)
> - public subnet과 private subnet을 가르는 것은 정확히 무엇인가?
>   (서브넷 속성이 아니라 **라우트 테이블이 IGW를 가리키는지 여부**다 — 자주 나오는 질문)

---

## 과제 3. 그 다음 (과제 2를 끝낸 뒤에 연다)

`docs/aws/01-architecture.md`의 "구축 순서" 표를 따라 2번(보안그룹) → 10번까지 진행한다.
**각 단계마다 검증하고 넘어간다.** 한 번에 다 만들고 apply 하면 원인을 못 찾는다.

가장 많이 막히는 8번(ECS) 전에, 3번(ECR)에서 이미지 push까지는 반드시 성공시켜 둘 것.
