# 하나부터 열까지 — 순서대로 따라하기

> 위에서부터 순서대로 한다. **각 단계의 "✅ 성공 확인"이 나와야 다음으로 넘어간다.**
> 안 나오면 그 자리에서 멈추고 물어본다. 다음 단계로 넘어가면 원인을 못 찾는다.

---

# 1단계. 로컬에서 지금 코드가 도는지 확인

## 1-1. 최신 코드 받기

```bash
cd baseball_recommend
git checkout develop
git pull origin develop
git log --oneline -5
```

✅ **성공 확인**: 로그에 `Terraform 기초 문서 추가`, `AWS 이관 준비` 가 보인다.

## 1-2. 환경변수 파일 만들기

```bash
cp .env.example .env
```

✅ **성공 확인**: `.env` 파일이 생겼다. (`.gitignore`에 있으므로 깃에 안 올라간다)

## 1-3. 띄우기

```bash
docker compose up --build
```

처음엔 Maven 의존성을 받느라 3~5분 걸린다. 로그가 멈춘 것처럼 보여도 기다린다.

✅ **성공 확인**: 로그 마지막에 `Started RecommendApplication in ... seconds` 가 보인다.

❌ **안 될 때**
- `docker: command not found` → Docker Desktop 설치 후 실행
- `port is already allocated` → 3306/8080/3000 포트를 이미 쓰는 게 있다. 그걸 끄거나 `docker-compose.yml`의 포트 번호를 바꾼다

## 1-4. 이번 변경사항 3개를 눈으로 확인

**새 터미널을 열고** (위 터미널은 서버가 돌고 있으니 그대로 둔다):

```bash
# ① 헬스체크 — Phase ①에서 추가한 것
curl -s localhost:8080/actuator/health
```
✅ `{"status":"UP"}`

```bash
# ② Flyway 마이그레이션이 적용됐는가
docker compose exec mysql mysql -uroot -ppassword baseball_recommend \
  -e "select version, description, success from flyway_schema_history;"
```
✅ 이렇게 나온다:
```
+---------+-------------+---------+
| version | description | success |
+---------+-------------+---------+
| 1       | init schema | 1       |
| 2       | shedlock    | 1       |
+---------+-------------+---------+
```

```bash
# ③ 테이블이 다 만들어졌는가
docker compose exec mysql mysql -uroot -ppassword baseball_recommend -e "show tables;"
```
✅ `team`, `player`, `member`, `game`, `standing`, `shedlock`, `flyway_schema_history` 등이 보인다.

```bash
# ④ 서버 시작 90초쯤 뒤, 스케줄러가 돌면 락 기록이 남는다
docker compose exec mysql mysql -uroot -ppassword baseball_recommend \
  -e "select name, lock_until, locked_by from shedlock;"
```
✅ `standing-refresh`, `game-refresh` 행이 생겨 있다. (아직 안 생겼으면 1~2분 더 기다린다)

## 1-5. 브라우저로 확인

http://localhost:3000 → 설문을 끝까지 해보고 추천 결과가 나오는지 본다.

✅ **성공 확인**: 결과 페이지가 뜬다. 여기까지 되면 **Flyway로 스키마를 바꿔도 앱이 정상 동작한다**는 게 증명된 것이다.

## 1-6. 정리

```bash
docker compose down
```

---

# 2단계. ShedLock이 진짜 동작하는지 실험

이 실험이 이번 작업에서 **면접에 쓸 수 있는 가장 좋은 재료**다. 꼭 한다.

## 2-1. 포트 매핑 잠시 제거

컨테이너를 2개로 늘리면 둘 다 호스트 8080을 잡으려 해서 충돌한다.
`docker-compose.yml`에서 **backend 쪽** `ports` 두 줄을 주석 처리한다.

```yaml
  backend:
    build: ./backend
    environment:
      ...
    # ports:
    #   - "8080:8080"
    depends_on:
```

## 2-2. 백엔드를 2개로 띄운다

```bash
docker compose up --build --scale backend=2
```

## 2-3. 로그 관찰 — **여기가 핵심**

시작 60~90초 뒤 스케줄러가 돈다. 로그에서 이 문구를 찾는다:

```
[스케줄] KBO 순위 갱신 시작
```

✅ **성공 확인**: `backend-1`, `backend-2` 중 **한쪽에서만** 찍힌다.

```bash
# 컨테이너별로 세어보기
docker compose logs backend | grep "순위 갱신 시작"
```

## 2-4. 대조 실험 — ShedLock을 끄면 어떻게 되나

`backend/src/main/java/com/baseball/recommend/domain/standing/StandingService.java`에서
`@SchedulerLock(...)` 줄을 **주석 처리**하고 다시 띄운다.

```bash
docker compose down
docker compose up --build --scale backend=2
```

✅ **이번엔 양쪽 다 찍힌다.** → KBO 서버에 중복 요청이 나가고 있다는 뜻이다.

확인했으면 **주석을 원래대로 되돌리고**, `docker-compose.yml`의 `ports`도 되돌린다.

```bash
git diff                 # 되돌릴 게 남아있는지 확인
git checkout -- .        # 실험용 수정 전부 되돌리기
docker compose down
```

> 📝 **여기서 배운 것을 한 줄로 정리해두자.**
> "스케줄러가 있는 서비스를 다중 인스턴스로 띄우면 중복 실행되고, ShedLock으로 DB 기반 분산 락을 걸어 해결했다.
> `--scale 2`로 직접 재현하고 확인했다." — 이게 면접 답변이다.

---

# 3단계. AWS 계정 준비

> ⚠️ **리소스를 하나라도 만들기 전에 이 단계를 끝낸다.** 특히 3-4번(예산 알람).

## 3-1. 계정 만들기

https://aws.amazon.com → 계정 생성. 신용카드가 필요하다(프리티어라도 등록해야 함).

## 3-2. 루트 계정에 MFA 걸기

1. 콘솔 우측 상단 계정 이름 → **보안 자격 증명**
2. **멀티 팩터 인증(MFA)** → **MFA 디바이스 할당**
3. 휴대폰에 Google Authenticator 또는 Microsoft Authenticator 설치 → QR 스캔

✅ **성공 확인**: 로그아웃 후 다시 로그인할 때 6자리 코드를 묻는다.

> **왜?** 루트 계정은 모든 걸 할 수 있고 권한 제한도 못 건다. 탈취당하면 끝이다.

## 3-3. 작업용 IAM 사용자 만들기

1. 콘솔 검색창에 **IAM** → **사용자** → **사용자 생성**
2. 사용자 이름: `terraform-admin` (아무거나)
3. **AWS Management Console에 대한 사용자 액세스 권한 제공** 체크
4. 권한: **직접 정책 연결** → `AdministratorAccess` 선택
   - 실무라면 최소 권한이지만, 학습 단계에선 관리자로 시작해도 된다
5. 생성 후 → 그 사용자 → **보안 자격 증명** 탭 → **액세스 키 만들기**
   - 사용 사례: **Command Line Interface (CLI)** 선택
   - **액세스 키 ID와 비밀 액세스 키를 저장한다. 비밀 키는 이 화면에서만 볼 수 있다.**

> ⚠️ **이 키를 절대 깃에 올리지 않는다.** 코드, 문서, 스크린샷 어디에도.

## 3-4. 예산 알람 — **건너뛰지 말 것**

1. 콘솔 검색창에 **Billing** 또는 **AWS Budgets**
2. **예산 생성** → 템플릿 사용 → **월별 비용 예산**
3. 예산 금액: **30,000원** (또는 $25)
4. 이메일 알림 받을 주소 입력

✅ **성공 확인**: 예산 목록에 항목이 하나 보인다.

> **왜?** 신입이 AWS 처음 쓰다 요금 사고 나는 건 거의 항상 NAT Gateway나 데이터 전송이다.
> 알람이 없으면 한 달 뒤 청구서를 보고 알게 된다.

---

# 4단계. AWS CLI 설치하고 연결

## 4-1. 설치

**macOS**
```bash
brew install awscli
```

**Windows**: https://awscli.amazonaws.com/AWSCLIV2.msi 다운로드 후 설치

**확인**
```bash
aws --version
```
✅ `aws-cli/2.x.x ...`

## 4-2. 자격증명 등록

```bash
aws configure
```
```
AWS Access Key ID     : (3-3에서 받은 키)
AWS Secret Access Key : (3-3에서 받은 비밀 키)
Default region name   : ap-northeast-2
Default output format : json
```

## 4-3. 확인

```bash
aws sts get-caller-identity
```

✅ **성공 확인**:
```json
{
    "UserId": "AIDA...",
    "Account": "123456789012",
    "Arn": "arn:aws:iam::123456789012:user/terraform-admin"
}
```

> **Arn 끝이 `:root`가 아니라 `:user/terraform-admin` 이어야 한다.** root면 3-3을 다시 한다.

```bash
aws configure get region
```
✅ `ap-northeast-2`

---

# 5단계. Terraform 설치

**macOS**
```bash
brew tap hashicorp/tap
brew install hashicorp/tap/terraform
```

**Windows**
1. https://developer.hashicorp.com/terraform/install 에서 Windows AMD64 zip 다운로드
2. 압축 풀어서 `terraform.exe`를 예를 들어 `C:\terraform\`에 둔다
3. 시스템 환경 변수 `Path`에 `C:\terraform` 추가
4. **터미널을 새로 연다** (Path 변경은 새 터미널부터 적용된다)

**확인**
```bash
terraform -version
```
✅ `Terraform v1.x.x`

---

# 6단계. 첫 Terraform — VPC 하나만

## 6-1. 디렉터리와 .gitignore

```bash
cd baseball_recommend
mkdir infra
cd infra
```

`infra/.gitignore` 파일을 만들고 아래 내용을 넣는다:

```gitignore
.terraform/
.terraform.lock.hcl
*.tfstate
*.tfstate.*
*.tfvars
```

> **왜?** `*.tfstate`에는 DB 비밀번호 같은 게 평문으로 들어간다. `.terraform/`는 수백 MB짜리 플러그인이다.

## 6-2. `infra/main.tf` 작성

**복사 붙여넣기 말고 직접 타이핑한다.** 손으로 쳐야 문법이 손에 붙는다.

```hcl
terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "baseball-vpc"
  }
}

output "vpc_id" {
  value = aws_vpc.main.id
}
```

**각 줄이 무슨 뜻인지**

| 줄 | 뜻 |
|---|---|
| `required_version` | 이 코드가 요구하는 Terraform 최소 버전 |
| `version = "~> 5.0"` | AWS provider를 5.x 안에서만 쓴다. **안 박으면 나중에 6.0이 나왔을 때 코드가 깨진다** |
| `provider "aws" { region }` | 어느 리전에 만들지 |
| `resource "aws_vpc" "main"` | `aws_vpc` 타입의 리소스를 만들고, 코드 안에서 `main`이라 부르겠다 |
| `enable_dns_hostnames` | 켜야 RDS 엔드포인트 DNS 이름이 해석된다. **끄면 나중에 DB 접속이 안 된다** |
| `tags = { Name = ... }` | 콘솔에서 보이는 이름. 안 붙이면 나중에 뭐가 뭔지 모른다 |
| `output` | apply 끝나고 화면에 출력할 값 |

## 6-3. init

```bash
terraform init
```

✅ **성공 확인**: `Terraform has been successfully initialized!`
(`.terraform/` 디렉터리와 `.terraform.lock.hcl` 파일이 생긴다)

❌ `Error: Failed to query available provider packages` → 인터넷 연결 확인

## 6-4. plan — **출력을 읽는 연습**

```bash
terraform plan
```

이런 게 나온다:
```
Terraform will perform the following actions:

  # aws_vpc.main will be created
  + resource "aws_vpc" "main" {
      + arn                    = (known after apply)
      + cidr_block             = "10.0.0.0/16"
      + enable_dns_hostnames   = true
      ...
    }

Plan: 1 to add, 0 to change, 0 to destroy.
```

**읽는 법**

| 기호 | 뜻 |
|---|---|
| `+ create` | 새로 만든다 |
| `~ update in-place` | 그 자리에서 고친다 |
| `-/+ replace` | **지우고 새로 만든다** ← DB에 이게 뜨면 데이터가 날아간다. 멈추고 확인 |
| `- destroy` | 지운다 |
| `(known after apply)` | 만들어봐야 아는 값 (id 등) |

✅ **성공 확인**: 마지막 줄이 `Plan: 1 to add, 0 to change, 0 to destroy.`

> **습관**: `apply` 전에 항상 이 마지막 줄의 숫자를 읽는다. 실무 사고는 여기서 난다.

## 6-5. apply

```bash
terraform apply
```

`Enter a value:` 물으면 **`yes`** 입력 (y 아님, yes 전체).

✅ **성공 확인**:
```
Apply complete! Resources: 1 added, 0 changed, 0 destroyed.

Outputs:
vpc_id = "vpc-0a1b2c3d4e5f"
```

## 6-6. 콘솔에서 눈으로 확인

1. AWS 콘솔 → 우측 상단 리전이 **서울(ap-northeast-2)** 인지 확인
2. 검색창에 **VPC** → 좌측 **VPC** 메뉴
3. `baseball-vpc` 가 보이는가?

✅ **성공 확인**: 목록에 보인다. **CIDR가 10.0.0.0/16 인지도 확인한다.**

❌ **안 보이면**: 리전이 다르다. 우측 상단에서 서울로 바꾼다.

## 6-7. state 파일 열어보기

```bash
cat terraform.tfstate
```

`"id": "vpc-0a1b..."` 가 보인다. **이게 내 코드의 `aws_vpc.main`과 실제 AWS 리소스를 잇는 매핑이다.**
이 파일을 지우면 Terraform은 이 VPC를 잃어버린다.

## 6-8. destroy — 지우는 것까지 해야 한 사이클

```bash
terraform destroy
```
`yes` 입력.

✅ **성공 확인**: `Destroy complete! Resources: 1 destroyed.` + 콘솔에서 사라짐

> **이 한 사이클(init → plan → apply → 확인 → destroy)을 돌려본 것이 6단계의 목표다.**
> "지웠다 다시 만들 수 있다"는 확신이 생겨야 비용 걱정 없이 실습할 수 있다.

---

# 7단계. 네트워크 전체 만들기 — 여기부터 직접

6단계까지 됐으면 이제 서브넷과 라우팅을 붙인다. **아래는 직접 작성한다.**

## 7-1. 만들 것

`01-architecture.md`의 "1. 네트워크" 표를 다시 본다.

| 리소스 | 개수 | Terraform 타입 |
|---|---|---|
| 퍼블릭 서브넷 | 2 (서로 다른 AZ) | `aws_subnet` |
| 프라이빗 서브넷 | 2 (서로 다른 AZ) | `aws_subnet` |
| 인터넷 게이트웨이 | 1 | `aws_internet_gateway` |
| 퍼블릭 라우트 테이블 | 1 | `aws_route_table` |
| 라우트 테이블 연결 | 2 | `aws_route_table_association` |

CIDR: public `10.0.0.0/24`, `10.0.1.0/24` / private `10.0.10.0/24`, `10.0.11.0/24`
AZ: `ap-northeast-2a`, `ap-northeast-2c`

## 7-2. 첫 서브넷 하나만 예시로 준다

```hcl
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.0.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true

  tags = {
    Name = "baseball-public-a"
  }
}
```

**나머지 3개 서브넷, IGW, 라우트 테이블, 연결은 직접 쓴다.**

공식 문서에서 찾는 법 — **이게 실무에서 제일 많이 하는 일이다**:
1. https://registry.terraform.io/providers/hashicorp/aws/latest/docs 접속
2. 좌측 검색창에 `internet_gateway` 입력
3. 페이지 상단의 **Example Usage** 를 본다
4. 필요한 속성은 **Argument Reference** 에서 찾는다

## 7-3. 힌트 (막혔을 때만 본다)

<details>
<summary>IGW와 라우트 테이블 연결이 감이 안 올 때</summary>

- `aws_internet_gateway`는 `vpc_id`만 있으면 된다
- `aws_route_table`은 `vpc_id` + `route` 블록. `route` 안에 `cidr_block = "0.0.0.0/0"` 과 `gateway_id`
- `aws_route_table_association`은 `subnet_id` + `route_table_id`. **서브넷 개수만큼 필요하다**
- 프라이빗 서브넷은 라우트 테이블을 안 만들어도 된다 (VPC 기본 라우트 테이블이 붙는다)

</details>

## 7-4. 검증

```bash
terraform plan     # 몇 개가 추가되는지 세어본다
terraform apply
```

✅ **성공 확인 — 콘솔에서 4가지를 본다**

1. **VPC → 서브넷**: 4개가 있고, **가용 영역이 2a 2개 / 2c 2개**로 나뉘어 있다
2. **VPC → 인터넷 게이트웨이**: `baseball-vpc`에 **Attached** 상태
3. **VPC → 라우팅 테이블** → 퍼블릭용 선택 → **라우팅** 탭:
   `0.0.0.0/0` → `igw-...` 가 있다
4. 같은 라우팅 테이블 → **서브넷 연결** 탭: 퍼블릭 서브넷 2개가 연결돼 있다

```bash
terraform destroy   # 확인 끝났으면 지운다
```

> 📝 **7단계를 끝내고 답해볼 질문**
> **"퍼블릭 서브넷과 프라이빗 서브넷을 가르는 건 정확히 무엇인가?"**
> 방금 만들면서 답을 봤을 것이다. 서브넷 자체의 설정이 아니다.
> **연결된 라우팅 테이블이 `0.0.0.0/0 → IGW` 를 가지고 있는지 여부**다. 면접 단골 질문이다.

---

# 8단계. 여기까지 되면

`infra/` 를 커밋한다. (`.gitignore` 덕분에 state와 tfvars는 안 올라간다)

```bash
cd ..
git status              # terraform.tfstate 가 목록에 없어야 한다!
git add infra
git commit -m "feat: Terraform VPC 네트워크 구성"
git push origin develop
```

⚠️ **`git status`에 `terraform.tfstate`가 보이면 커밋하지 말고 `.gitignore`부터 고친다.**

그 다음은 `01-architecture.md`의 구축 순서 **2번(보안그룹)** 부터다.
보안그룹은 "다른 보안그룹을 소스로 참조"하는 게 핵심이니, 그 부분을 눈여겨본다.
