# Terraform이 뭔가 — 기초부터

## 1. 문제부터 보자: 콘솔로 클릭해서 만들면 뭐가 문제인가

`01-architecture.md`에서 만들어야 할 리소스를 세어보면 VPC, 서브넷 4개, IGW,
라우트 테이블, 보안그룹 3개, ECR, RDS, ECS 클러스터/태스크/서비스, ALB, 타깃그룹,
S3, CloudFront, IAM 역할 2개, SSM 파라미터 6개... **30개가 넘는다.**

이걸 콘솔에서 클릭으로 만들면:

| 문제 | 구체적으로 |
|---|---|
| **재현이 안 된다** | 지웠다 다시 만들 자신이 없으니, 안 쓸 때도 못 끄고 켜둔다 → **돈이 샌다** |
| **기록이 없다** | 3주 뒤 "이 보안그룹 3306은 왜 열어놨더라?" 알 방법이 없다 |
| **사람은 실수한다** | 30번 클릭 중 한 번 서브넷을 잘못 고르면, 그걸 찾는 데 두 시간 쓴다 |
| **리뷰가 안 된다** | 팀원이 뭘 바꿨는지 볼 수가 없다 |

Terraform은 이 네 가지를 전부 해결한다. **인프라를 코드 파일로 적어서 깃에 넣는 것**,
이게 IaC(Infrastructure as Code)이고 Terraform은 그 도구 중 하나다.

---

## 2. Terraform이 하는 일 — 한 문장

> **원하는 최종 상태를 코드로 적어두면, Terraform이 현재 상태와 비교해서 차이만큼만 AWS API를 호출한다.**

여기서 핵심은 **"어떻게 만들지"가 아니라 "무엇이 있어야 하는지"를 적는다**는 것이다.
이걸 **선언형(declarative)** 이라고 한다.

```bash
# 명령형 — 쉘 스크립트. "이 명령을 실행해라"
aws ec2 create-vpc --cidr-block 10.0.0.0/16
# 두 번 실행하면? VPC가 2개 생긴다. 😱
```

```hcl
# 선언형 — Terraform. "VPC가 하나 있어야 한다"
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
}
# 두 번 실행하면? 이미 있으므로 아무것도 안 한다. ✅
```

이 성질을 **멱등성(idempotency)** 이라 한다. 몇 번을 실행해도 결과가 같다는 뜻이다.
**면접에서 "IaC의 장점"을 물으면 이 단어가 나와야 한다.**

---

## 3. 알아야 할 개념은 5개뿐

| 개념 | 뭐냐 | 비유 |
|---|---|---|
| **provider** | 어느 클라우드와 대화할지 | 어느 회사 API를 쓸지 정하는 것 (aws, google, azure...) |
| **resource** | 만들 것 하나하나 | VPC 하나, 보안그룹 하나 |
| **state** | 지금까지 뭘 만들었는지 기록한 파일 | Terraform의 기억. **제일 중요하다 (6장)** |
| **plan / apply** | 차이 계산 / 실제 실행 | 견적서 보기 / 결제하기 |
| **variable / output** | 입력값 / 결과값 | 함수의 파라미터 / 리턴값 |

---

## 4. 문법(HCL)은 이 한 줄 구조가 전부다

```
블록타입 "타입이름" "내가붙인이름" {
  키 = 값
}
```

```hcl
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
}
#  ↑         ↑          ↑
#  블록타입   AWS 리소스 타입   내가 붙인 이름(별명)
```

**다른 리소스를 참조할 때는 `타입.이름.속성` 으로 쓴다.**

```hcl
resource "aws_subnet" "public_a" {
  vpc_id     = aws_vpc.main.id     # ← 위에서 만든 VPC의 id
  cidr_block = "10.0.0.0/24"
}
```

이 참조 한 줄이 하는 일이 크다. **Terraform은 이걸 보고 "VPC를 먼저 만들고 서브넷을
나중에 만들어야 한다"는 순서를 스스로 파악한다.** 순서를 사람이 지정할 필요가 없다.
(의존성 그래프를 만든다 — 이것도 면접 단골이다.)

---

## 5. 가장 작은 예제 — 직접 따라 써보기

디렉터리를 만들고 `main.tf` 파일 하나만 만든다.

```bash
mkdir -p infra && cd infra
```

```hcl
# infra/main.tf

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"     # 5.x 안에서만 업데이트 (버전을 안 박으면 나중에 깨진다)
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"   # 서울
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "baseball-recommend-vpc"
  }
}

output "vpc_id" {
  value = aws_vpc.main.id
}
```

**딱 이만큼으로 한 사이클을 돌려본다.**

```bash
terraform init      # ① AWS provider 플러그인을 내려받는다 (최초 1회, .terraform/ 생성)
terraform plan      # ② 뭘 할지 보여준다. 아직 아무것도 안 만든다
terraform apply     # ③ yes 입력하면 실제로 만든다
terraform destroy   # ④ 지운다
```

**`plan` 출력을 읽는 법** — 이게 Terraform 사용의 절반이다.

```
  + create          # 새로 만든다
  ~ update in-place # 그 자리에서 고친다
  -/+ replace       # 지우고 새로 만든다  ← ⚠️ DB에 이게 뜨면 데이터가 날아간다. 반드시 확인!
  - destroy         # 지운다

Plan: 1 to add, 0 to change, 0 to destroy.
```

> **`apply` 전에 `plan`의 마지막 줄 숫자를 항상 읽는다.** `0 to destroy`인 줄 알았는데
> `3 to destroy`면 손을 멈춘다. 실무에서 사고는 여기서 난다.

---

## 6. state — 여기가 제일 중요하다

`apply` 하고 나면 `terraform.tfstate` 파일이 생긴다. 열어보면 JSON이다.

**왜 필요한가?** 내 코드엔 `aws_vpc.main`이라고 적혀 있고, AWS엔 `vpc-0a1b2c3d`가 있다.
**이 둘을 연결하는 매핑을 state가 들고 있다.** state가 없으면 Terraform은
"내가 만든 그 VPC"가 어느 것인지 모른다.

| 상황 | 벌어지는 일 |
|---|---|
| state 파일을 지웠다 | Terraform이 리소스를 잃어버린다. `apply` 하면 **똑같은 걸 또 만든다**. 기존 것은 고아가 되고 요금은 계속 나간다 |
| state를 깃에 올렸다 | **DB 비밀번호 같은 게 평문으로 들어있다.** 절대 올리면 안 된다 |
| 팀원 둘이 동시에 apply | state가 깨진다 → 그래서 **원격 state + 잠금(S3 + DynamoDB)** 이 필요하다 |

**지금 당장 할 일**: `infra/.gitignore`를 만들어 둔다.

```gitignore
.terraform/
*.tfstate
*.tfstate.*
*.tfvars
```

> **면접 질문**: "terraform.tfstate는 무엇이고 팀 작업에서 어떻게 관리하나요?"
> → 답의 뼈대: 코드와 실제 리소스의 매핑을 담은 파일이다 / 비밀값이 들어 있어 깃에 올리지 않는다 /
> 팀에서는 S3 backend에 두고 DynamoDB로 동시 실행을 잠근다.

---

## 7. 실무 워크플로

```
코드 수정 → terraform plan → 출력 확인 → terraform apply → 콘솔에서 검증
```

- **`plan` 없이 `apply` 하지 않는다.**
- 한 번에 다 만들지 않는다. VPC 만들고 확인, 보안그룹 만들고 확인 — 이렇게 쪼갠다.
  30개를 한 번에 apply 했다가 실패하면 어디가 문제인지 못 찾는다.
- **`destroy`도 연습한다.** 지웠다 다시 만들 수 있다는 확신이 있어야 안 쓸 때 끌 수 있고,
  그래야 돈이 안 샌다. 이게 이 프로젝트에서 Terraform을 쓰는 가장 실용적인 이유다.

---

## 8. 처음에 자주 하는 실수

| 증상 | 원인 |
|---|---|
| `Error: No valid credential sources found` | `aws configure`를 안 했거나 프로파일이 안 잡힘. `aws sts get-caller-identity`로 먼저 확인 |
| `apply` 했는데 콘솔에 안 보임 | 콘솔 우측 상단 **리전이 다르다** (서울인지 확인) |
| `plan`에 `-/+ replace`가 잔뜩 | 바꾸면 안 되는 속성(예: 서브넷의 CIDR)을 건드렸다. **DB에서 이게 뜨면 절대 apply 금지** |
| `destroy` 실패 | RDS `deletion_protection`, S3 버킷에 객체가 남음, ENI가 아직 안 떨어짐 |
| provider 버전이 멋대로 올라가서 깨짐 | `version = "~> 5.0"`을 안 박았다 |

---

## 9. 그래서 지금 할 것

1. **Terraform 설치** → `terraform -version`
2. 위 5장의 `main.tf`를 **그대로 따라 쓰고** `init → plan → apply → destroy` 한 사이클
   - `plan` 출력을 소리 내어 읽어본다. 뭘 만든다고 하는지
   - `apply` 후 콘솔 VPC 대시보드에서 실제로 보이는지 확인
   - `destroy` 후 사라졌는지 확인
3. 그게 되면 `docs/aws/02-hands-on.md`의 **과제 2**로 간다
   (서브넷 4개 + IGW + 라우트 테이블을 `01-architecture.md`의 표를 보고 직접 추가)

> VPC 하나를 만드는 예제는 위에 그대로 줬다. **서브넷부터는 직접 쓴다.**
> `aws_subnet`, `aws_internet_gateway`, `aws_route_table`, `aws_route_table_association` —
> 이 네 개를 [Terraform AWS Provider 문서](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)에서
> 찾아 쓰는 것까지가 과제다. **공식 문서에서 리소스 찾는 연습이 실무에서 제일 많이 쓴다.**
