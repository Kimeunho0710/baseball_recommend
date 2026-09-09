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

## 올리기

### 1. 인프라

```bash
cd ~/dev/baseball_recommend/infra
terraform apply
```

RDS 5~10분, CloudFront 5~15분. 전체 15~25분.

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

## 내리기

```bash
cd ~/dev/baseball_recommend/infra
terraform destroy
```

**작업이 끝나면 반드시 실행한다.** RDS·ALB·Fargate는 시간당 과금이고, RDS 스토리지는 인스턴스를 꺼도 나간다.

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

**원인**: 다른 터미널에서 `terraform apply` 가 진행 중 (RDS 생성은 5~10분 걸린다).

**조치**: `ps aux | grep '[t]erraform'` 으로 확인 → **프로세스가 살아있으면 기다린다.** 죽었을 때만 `terraform force-unlock <ID>`.

⚠️ **실행 중인데 force-unlock 하면 state가 깨진다.** AWS에는 리소스가 만들어졌는데 state에 없는 "유령 리소스"가 되어 요금만 계속 나간다.

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

### 7. 기타

| 증상 | 원인 |
|---|---|
| `terraform plan` → `No configuration files` | `infra/` 디렉터리 밖에서 실행. `terraform -chdir=infra plan` 도 가능 |
| `zsh: command not found: #` | 명령 뒤 `#` 주석까지 붙여넣음. zsh 대화형 셸은 `#` 을 주석으로 보지 않는다 |
| `aws logs tail --follow` 가 조용함 | `--follow` 는 이후 발생분만 보여준다. 과거 로그는 `--since 15m` |
| 콘솔에 리소스가 안 보임 | 우측 상단 리전 확인 (VPC 등은 리전 단위 리소스) |
