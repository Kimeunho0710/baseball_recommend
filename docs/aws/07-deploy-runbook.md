# 배포 · 정리 런북

> 인프라를 올리고 내리는 실제 절차. **비용 때문에 자주 반복하게 되므로** 순서를 여기에 고정한다.
> 셸 변수는 반드시 `"${VAR}"` 형태로 쓸 것 (아래 트러블슈팅 2번 참고).

---

## 완성된 구성

```
사용자
  │ HTTPS
  ▼
CloudFront ─┬─ /*      → S3 (Vue 정적, OAC)
            └─ /api/*  → ALB → ECS Fargate → RDS MySQL (프라이빗)
                                     │
                              CloudWatch Logs
                              SSM Parameter Store (DB 비밀번호 / JWT 시크릿)
```

- `/api/*` 를 CloudFront가 프록시하므로 **브라우저 입장에서는 동일 오리진** → CORS·mixed content 문제 없음
- NAT Gateway 미사용. Fargate를 퍼블릭 서브넷에 두고 보안그룹으로 통제

---

## Terraform state (원격 백엔드)

state 는 S3 에 있다. `infra/main.tf` 의 `backend "s3"` 블록이 가리킨다.

```
s3://baseball-tfstate-273144883894/baseball/terraform.tfstate
```

**이 버킷은 `terraform destroy` 대상이 아니다.** Terraform 관리 밖에 두고 AWS CLI 로 한 번 만들었다.
같은 구성에 넣으면 `destroy` 가 자기 state 를 담은 버킷을 지우려 든다.

### 버킷을 다시 만들어야 할 때 (계정 이전 등)

```bash
export TFSTATE_BUCKET="baseball-tfstate-273144883894"

aws s3api create-bucket --bucket "${TFSTATE_BUCKET}" --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2

aws s3api put-bucket-versioning --bucket "${TFSTATE_BUCKET}" \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption --bucket "${TFSTATE_BUCKET}" \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

aws s3api put-public-access-block --bucket "${TFSTATE_BUCKET}" \
  --public-access-block-configuration BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
```

그다음 `terraform init -migrate-state` → `yes`.

| 설정 | 이유 |
|---|---|
| **버저닝** | state 가 잘못 덮어써졌을 때 되돌릴 유일한 수단. **없이 원격화하면 오히려 위험하다** |
| 암호화 | state 에 DB 비밀번호·JWT 시크릿이 평문으로 들어있다 |
| 퍼블릭 차단 | 같은 이유 |

### 잠금

`use_lockfile = true` — S3 조건부 쓰기를 이용한 Terraform 자체 잠금 (1.10+). 별도 리소스가 없다.
apply 중에는 `.tflock` 파일이 생기고, 다른 실행은 `Error acquiring the state lock` 으로 거부된다.

로컬 state 시절의 잠금은 이 맥 안에서만 유효했다. 지금은 다른 사람·다른 컴퓨터·CI 까지 전부 막힌다.

> 예전 방식은 잠금 전용 DynamoDB 테이블(`dynamodb_table`)이었다. 1.10 부터 S3 네이티브 잠금으로 대체됐고 DynamoDB 방식은 폐기 예정. 기존 코드베이스에는 아직 많이 남아 있다.

### 주의

- **백엔드 블록에는 변수를 쓸 수 없다.** Terraform 이 변수를 계산하기 전에 state 를 읽어야 하므로 값을 직접 적는다
- 백엔드 블록 자체는 커밋한다 (버킷 이름·경로뿐, 비밀 없음). state 파일은 계속 `.gitignore` 대상
- 로컬에 남은 `terraform.tfstate` 는 마이그레이션 백업일 뿐 더 이상 읽히지 않는다

---

## 올리기

### 1. 인프라

```bash
cd ~/dev/baseball_recommend/infra
terraform plan -out=tfplan
terraform apply tfplan
```

RDS 5~10분, CloudFront 5~15분. 전체 15~25분.

저장된 plan 을 적용하면 **확인 프롬프트가 없고, 검토한 그대로만** 실행된다.
`-out` 없이 `apply` 하면 Terraform 이 다시 계산하므로 방금 본 것과 달라질 수 있다 (CI/CD 가 `-out` 을 쓰는 이유).

⚠️ `tfplan` 에는 DB 비밀번호·JWT 시크릿이 들어있다. **적용 후 `rm tfplan`.**

### 2. 백엔드 이미지

```bash
export ECR_URL=$(aws ecr describe-repositories --repository-names baseball-recommend-backend --query 'repositories[0].repositoryUri' --output text)
export ECR_REGISTRY=${ECR_URL%%/*}
echo "${ECR_URL}"
```
**두 변수 모두 값이 찍히는지 확인하고 넘어간다.**

```bash
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

cd ~/dev/baseball_recommend
docker build --platform linux/amd64 -t "${ECR_URL}:latest" ./backend
docker push "${ECR_URL}:latest"
```

**푸시 확인은 반드시 원격에서** (`docker images` 는 로컬만 본다):
```bash
aws ecr describe-images --repository-name baseball-recommend-backend \
  --query 'imageDetails[].[imageTags[0],imageSizeInBytes]' --output table
```

### 3. ECS 배포

```bash
aws ecs update-service --cluster baseball-cluster --service baseball-backend --force-new-deployment
```

```bash
aws logs tail /ecs/baseball-backend --since 10m
```
`Started RecommendApplication in ... seconds` 확인.

```bash
cd infra
curl "http://$(terraform output -raw alb_dns_name)/actuator/health"
```

### 4. 프론트엔드

```bash
cd ~/dev/baseball_recommend/frontend
npm ci
npm run build
```

`VITE_API_URL` 은 **주지 않는다.** 미설정 시 `http.js` 가 `/api` 를 쓰고, 그게 CloudFront 프록시 경로다.

```bash
export BUCKET="baseball-recommend-frontend-273144883894"

aws s3 sync dist/ "s3://${BUCKET}/" --delete \
  --exclude "index.html" \
  --cache-control "public,max-age=31536000,immutable"

aws s3 cp dist/index.html "s3://${BUCKET}/index.html" \
  --cache-control "no-cache,no-store,must-revalidate" \
  --content-type "text/html"
```

해시가 붙은 자산은 장기 캐시, `index.html` 은 캐시 금지. **index.html 을 캐시하면 배포해도 옛 화면이 계속 보인다.**

```bash
DIST_ID=$(aws cloudfront list-distributions \
  --query "DistributionList.Items[?Comment=='baseball-recommend'].Id | [0]" --output text)
aws cloudfront create-invalidation --distribution-id "${DIST_ID}" --paths "/*"
```

### 5. 접속

```bash
cd ../infra
echo "https://$(terraform output -raw cloudfront_domain)"
```

---

## 자동 배포 (CD)

`main` 에 푸시하면 GitHub Actions 가 빌드부터 ECS 배포까지 처리한다.
위 **2. 백엔드 이미지**, **3. ECS 배포** 는 인프라를 새로 올린 직후처럼 CD 를 쓸 수 없을 때의 수동 절차다.

```
git push origin main
      │
      ▼
.github/workflows/cd.yml
  ① OIDC 로 임시 자격증명 획득   (저장된 액세스 키 없음)
  ② docker build → ECR push      태그 = 커밋 SHA
  ③ 현재 태스크 정의 조회 → image 만 교체
  ④ ECS 롤링 배포 → 헬스체크 통과까지 대기
```

소요 시간 약 6분 (이미지 빌드 1분, ECS 배포 4~5분).

### 최초 1회 설정

`terraform apply` 로 역할이 만들어진 뒤, 저장소에 ARN 을 등록한다.

```bash
cd infra
terraform output github_actions_role_arn
```

GitHub → Settings → Secrets and variables → Actions → **Variables** 탭 → New repository variable

| Name | Value |
|---|---|
| `AWS_ROLE_ARN` | 위 output 값 |

**Secrets 가 아니라 Variables 다.** 역할 ARN 은 비밀이 아니다 — 신뢰 정책이 저장소·브랜치를 검사하므로 ARN 만으로는 아무것도 못 한다.
Secrets 에 넣으면 로그에서 `***` 로 가려져 디버깅만 어려워진다.

인프라를 `destroy` 후 다시 `apply` 하면 역할 ARN 은 그대로다 (이름 기반). 재등록 불필요.

### 왜 OIDC 인가

액세스 키를 GitHub Secrets 에 넣는 방식은 **만료가 없다.** 유출되면 직접 지울 때까지 유효하다.
OIDC 는 GitHub 이 서명한 토큰을 AWS 가 검증하고 그 자리에서 임시 자격증명을 발급한다. 저장되는 비밀이 0 개다.

### 신뢰 정책의 `sub` 조건 ⚠️

`aws_iam_role.github_actions` 의 신뢰 정책에서 이 줄이 보안의 전부다.

```hcl
"token.actions.githubusercontent.com:sub" = "repo:Kimeunho0710/baseball_recommend:ref:refs/heads/main"
```

느슨하게 두면 (`repo:...:*` 또는 `repo:*`) 다른 브랜치·PR·심지어 남의 저장소에서 이 역할을 맡을 수 있다.
브랜치를 늘릴 때만 `StringLike` + 와일드카드를 쓰고, 그 외에는 `StringEquals` 로 정확히 못 박는다.

### 배포 확인

```bash
aws ecs describe-services --cluster baseball-cluster --services baseball-backend \
  --query 'services[0].taskDefinition' --output text

aws ecr describe-images --repository-name baseball-recommend-backend \
  --query 'sort_by(imageDetails,&imagePushedAt)[-1].imageTags' --output text
```

이미지 태그가 방금 푸시한 커밋 SHA 와 같아야 한다.
태스크 정의 리비전 번호는 `destroy` 해도 초기화되지 않고 계속 올라간다.

### Terraform 과 CD 의 경계

| 대상 | 관리 주체 |
|---|---|
| 태스크 정의의 구조 (환경변수·시크릿·CPU/메모리) | Terraform |
| 태스크 정의의 이미지 태그 | CD |

`aws_ecs_service` 에 아래를 두어 Terraform 이 CD 의 배포를 되돌리지 않게 한다.

```hcl
lifecycle {
  ignore_changes = [task_definition, desired_count]
}
```

이게 없으면 다음 `terraform apply` 가 이미지를 `:latest` 로 되돌린다.

---

## 모니터링 (CloudWatch 알람 + SNS)

```
CloudWatch 알람 ──▶ SNS 주제(baseball-alerts) ──▶ 이메일
```

알람이 이메일 주소를 직접 알지 않는다. SNS 에 던지고 구독자가 받아간다.
Slack·Lambda·SMS 를 추가해도 알람 정의는 건드릴 필요가 없다.

### 거는 알람 (4개)

| 알람 | 지표 | 조건 | 성격 |
|---|---|---|---|
| `baseball-alb-5xx` | `HTTPCode_Target_5XX_Count` | 5분간 5건 이상 | 증상 — 사용자가 실제로 에러를 겪는 중 |
| `baseball-alb-unhealthy-target` | `UnHealthyHostCount` | 60초 × 3회 연속 1개 이상 | 증상 — 컨테이너 헬스체크 실패 |
| `baseball-rds-low-storage` | `FreeStorageSpace` | 2GB 미만 | 예방 — 디스크가 차면 DB 가 멈춘다 |
| `baseball-ecs-high-memory` | `MemoryUtilization` | 5분 × 2회 85% 초과 | 예방 — OOM 직전 신호 |

**알람은 적을수록 좋다.** 기준은 "이게 울리면 지금 일어나서 뭔가 해야 하나?" — 아니면 알람이 아니라 대시보드 항목이다.
RDS CPU 는 뺐다. 높아도 응답이 정상이면 장애가 아니다(원인 기반 지표).

`period × evaluation_periods` 가 예민함을 정한다. 한 번만 넘어도 울리게 하면 **배포할 때마다 알람이 온다** → 사람이 무시하게 된다.

### 공통 설정

```hcl
alarm_actions      = [aws_sns_topic.alerts.arn]
ok_actions         = [aws_sns_topic.alerts.arn]
treat_missing_data = "notBreaching"
```

- `ok_actions` — 복구됐을 때도 알린다. 없으면 언제 정상으로 돌아왔는지 모른다
- `treat_missing_data = "notBreaching"` — **이 구성에서 특히 중요.** `destroy` 하면 지표가 사라지는데, 기본값이면 알람이 `INSUFFICIENT_DATA` 로 남거나 잘못 울린다. "데이터 없음 = 정상" 으로 둬야 destroy/apply 사이클과 맞는다

### 이메일 주소

`infra/terraform.tfvars` (gitignore 대상) 에 둔다.

```hcl
alert_email = "..."
```

`variable` 에 `default` 로 박으면 동작은 하지만 개인 이메일이 저장소에 커밋된다.

⚠️ **SNS 이메일 구독은 사람이 확인 메일을 클릭해야 활성화된다.** Terraform 으로 만들면 `PendingConfirmation` 상태로 생긴다. 자동화할 수 없는 지점(스팸 방지).
네이버 메일은 스팸함에 걸리는 경우가 있다.

```bash
aws sns list-subscriptions \
  --query 'Subscriptions[?contains(TopicArn,`baseball-alerts`)].[Endpoint,SubscriptionArn]' --output text
```

`PendingConfirmation` 이 아니라 실제 ARN 이면 활성화됐다.

### 전달 경로 검증 — 반드시 한다

설정만 하고 검증하지 않으면 정작 장애 때 안 온다.

```bash
aws cloudwatch set-alarm-state --alarm-name baseball-alb-5xx \
  --state-value ALARM --state-reason "알람 전달 경로 테스트"
```

지표와 무관하게 상태를 강제로 바꾼다 → `alarm_actions` 실행 → 메일.
실제 5xx 가 없으므로 다음 평가 주기에 `OK` 로 되돌아가며 `ok_actions` 로 한 통 더 온다. **두 통이 오면 복구 알림까지 동작하는 것.**

이 테스트가 검증하는 것은 **전달 경로뿐**이다. 임계값이 적절한지, 지표 차원이 올바른지는 실제 트래픽에서만 드러난다. 처음 한 달은 임계값을 조정하는 기간으로 본다.

---

## 내리기

```bash
cd ~/dev/baseball_recommend/infra
terraform destroy
```

**작업이 끝나면 반드시 실행한다.** RDS·ALB·Fargate는 시간당 과금이고, RDS 스토리지는 인스턴스를 꺼도 나간다.

### ECR은 남는다 (의도된 동작)

`destroy` 는 **이미지가 든 ECR 리포지토리를 지우지 않는다.**

```
Error: ECR Repository (baseball-recommend-backend) not empty, consider using force_delete
```

37개 중 36개가 지워지고 ECR 하나만 남는다. 안전장치이므로 실패로 보지 않는다.

**남겨두는 쪽을 권장한다.** 이미지 약 138MB → **월 20원 정도**이고, 다음에 다시 올릴 때 10분짜리 Docker 빌드·푸시를 건너뛸 수 있다.

정말 지우려면 `aws_ecr_repository.backend` 에 `force_delete = true` 를 넣고 `apply` → `destroy`.
운영 환경에서는 쓰지 않는다. 확인 없이 이미지를 전부 지운다.

### 다 내려갔는지 확인

시간당 과금이 붙는 셋만 보면 된다.

```bash
aws elbv2 describe-load-balancers --query 'LoadBalancers[].LoadBalancerName' --output text
aws rds describe-db-instances --query 'DBInstances[].DBInstanceIdentifier' --output text
aws ecs list-clusters --query 'clusterArns' --output text
```

셋 다 빈 줄이면 시간당 과금은 0원이다. `terraform state list` 에 `aws_ecr_repository.backend` 만 남아 있으면 정상이다.

---

## 비용 (서울 리전, 대략)

| 리소스 | 시간당 |
|---|---:|
| ALB | 약 32원 (트래픽 0이어도) |
| Fargate 0.5vCPU/1GB | 약 40원 |
| RDS db.t4g.micro | 약 30원 |
| **합계** | **약 100원/시간** |

VPC·서브넷·IGW·라우팅·보안그룹·ECR·S3·CloudFront는 사실상 무료.

```
실습 3시간 × 20일 = 60시간 → 약 6,000원
24시간 상시 가동 = 월 720시간 → 약 7만원
```

**12배 차이.** destroy/apply 사이클이 IaC를 쓰는 실질적인 이유다.

안전장치: 예산 알람 $10 / $30, 비용 이상 징후 감지.

---

## 트러블슈팅 — 실제로 겪은 것들

### 1. `no match for platform in manifest: not found` (로컬 빌드 실패)

**증상**: M칩 맥에서 `docker compose up --build` 시 백엔드 이미지 빌드 실패.

**원인**: `eclipse-temurin:17-jre-alpine` 은 **linux/amd64만 배포**된다. alpine(musl) + arm64 조합이 없다. Railway와 GitHub Actions는 amd64라 드러나지 않았다.

**확인**: `docker manifest inspect <이미지> | grep architecture`

**조치**: 런타임 베이스를 멀티아치인 `eclipse-temurin:17-jre-jammy` 로 교체. alpine 전용 명령(`addgroup`/`adduser`)도 `groupadd`/`useradd` 로, 헬스체크 `wget` 은 `curl` 로 변경.

### 2. `backendatest` — zsh 파라미터 수식어

**증상**: `docker build -t $ECR_URL:latest` 로 만든 이미지 이름이 `...backendatest:latest` 가 되고, push가 실패해도 눈치채지 못함.

**원인**: **zsh는 `$var:l` 을 "소문자 변환" 수식어로 해석한다.** `$ECR_URL:latest` → `소문자(ECR_URL)` + 남은 문자 `atest`. bash에서는 안 나는 문제.

**조치**: 항상 `"${ECR_URL}:latest"` 처럼 중괄호로 감싼다.

### 3. `CannotPullContainerError: ... not found`

**증상**: ECS 태스크가 계속 뜨고 죽음. **CloudWatch 로그 그룹이 완전히 비어 있음.**

**진단 요령**: **로그가 비어 있다 = 컨테이너가 시작조차 못 했다.** 앱 문제가 아니라 그 이전 단계(이미지 pull / 시크릿 주입 / 네트워크)를 봐야 한다.

```bash
aws ecs describe-services --cluster baseball-cluster --services baseball-backend \
  --query 'services[0].events[0:5].message' --output text
```

**원인**: 2번 때문에 ECR에 이미지가 없었음. `docker image inspect` 는 **로컬**을 보는 명령이라 검증이 되지 않았다.

**교훈**: "올렸다"의 확인은 반드시 `aws ecr describe-images` 로 **원격에서** 한다.

### 4. `Error acquiring the state lock`

**증상**: `terraform plan` 이 잠금 오류로 거부됨.

**원인**: 둘 중 하나다.
1. 다른 터미널에서 `terraform apply` 가 진행 중 (RDS 생성은 5~10분)
2. **`Ctrl+C` 로 끊은 plan/apply 가 잠금을 남김** — 원격 백엔드로 옮긴 뒤 자주 만난다

**판단**: `ps aux | grep '[t]erraform'` — 다만 **무엇이 보이는지**를 봐야 한다.

| 보이는 것 | 판단 |
|---|---|
| `terraform plan` / `terraform apply` (본체) | 🔴 살아있음. **기다린다** |
| `terraform-provider-*` 만 | 🟢 고아 프로세스. 잠금을 풀 능력이 없으므로 `force-unlock` 가능 |

Terraform 은 프로바이더를 별도 프로세스로 띄우고 gRPC 로 통신한다. 중간에 끊기면 프로바이더만 고아로 남는다.

```bash
kill $(pgrep -f 'terraform-provider') 2>/dev/null
terraform force-unlock <ID>
```

⚠️ **본체가 실행 중인데 force-unlock 하면 state가 깨진다.** AWS에는 리소스가 만들어졌는데 state에 없는 "유령 리소스"가 되어 요금만 계속 나간다.

**예방**: `Ctrl+C` 는 **한 번만** 누르고 "Gracefully shutting down..." 이 끝날 때까지 기다린다. 두 번 누르거나 창을 닫으면 잠금이 남는다.
`terraform plan | grep ...` 처럼 파이프로 묶으면 진행 상황이 안 보여 멈춘 것처럼 느껴진다 → `terraform plan -no-color > /tmp/plan.txt` 후 파일을 grep 한다.

### 5. `plan` 만 하고 `apply` 를 안 함

**증상**: 콘솔에 리소스가 안 보임.

**원인**: `terraform plan` 은 **읽기 전용**이다. 아무것도 만들지 않는다.

**확인**: `terraform state list` 가 비어 있으면 아직 안 만든 것.

### 6. `-/+ replace` 를 만났을 때

`plan` 에 `-/+ destroy and then create replacement` 가 뜨면 **무엇이 교체되는지**를 본다.

| 대상 | 판단 |
|---|---|
| RDS, EBS 볼륨, S3 버킷 등 **상태를 담는 것** | 멈추고 확인. 데이터가 날아간다 |
| 태스크 정의, 보안그룹 규칙 등 **정의만 담는 것** | 정상. 태스크 정의는 원래 불변이라 새 리비전을 만드는 게 정상 동작 |

### 7. `RepositoryNotEmptyException` (destroy 실패)

**증상**: `terraform destroy` 가 36개는 지우고 ECR 하나에서 멈춤.

**원인**: 이미지가 남아 있는 ECR 리포지토리는 삭제를 거부한다. 실수로 이미지를 날리는 것을 막는 안전장치다.

**조치**: 남겨둔다 (월 20원, 다음 배포 때 빌드 생략). 지우려면 `force_delete = true` → `apply` → `destroy`. 위 **내리기** 절 참고.

### 8. 기타

| 증상 | 원인 |
|---|---|
| `terraform plan` → `No configuration files` | `infra/` 디렉터리 밖에서 실행. `terraform -chdir=infra plan` 도 가능 |
| `zsh: command not found: #` | 명령 뒤 `#` 주석까지 붙여넣음. zsh 대화형 셸은 `#` 을 주석으로 보지 않는다 |
| `aws logs tail --follow` 가 조용함 | `--follow` 는 이후 발생분만 보여준다. 과거 로그는 `--since 15m` |
| 콘솔에 리소스가 안 보임 | 우측 상단 리전 확인 (VPC 등은 리전 단위 리소스) |
