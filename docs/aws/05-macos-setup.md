# 맥북 처음이라면 — 여기부터

> `04-step-by-step.md` 1단계를 시작하기 전에 이 문서를 먼저 끝낸다.
> 맥에서 개발 환경을 처음 잡는 데 필요한 것만 담았다.

---

## 0. 먼저 알아둘 맥 기본

| 하고 싶은 것 | 맥에서는 |
|---|---|
| 복사 / 붙여넣기 | **⌘ + C** / **⌘ + V** (Ctrl 아니고 Command) |
| 앱 찾아서 실행 | **⌘ + Space** → 앱 이름 입력 → Enter (Spotlight) |
| 터미널에서 실행 중인 것 중단 | **Ctrl + C** (여기는 Ctrl이 맞다) |
| 창 닫기 / 앱 종료 | ⌘ + W / ⌘ + Q |

**Command 키(⌘)** 는 스페이스바 양옆에 있는 키다.

---

## 1. 터미널 열기

**⌘ + Space** → `터미널` 입력 → Enter

검은(또는 흰) 창이 뜨고 이런 게 보인다:

```
사용자이름@MacBook-Pro ~ %
```

- 맨 끝 `%` 뒤에 명령어를 친다
- `~` 는 **홈 폴더**를 뜻한다 (`/Users/사용자이름`)

> 💡 자주 쓸 거라 **Dock에 고정**해두면 편하다.
> 터미널이 실행 중일 때 Dock 아이콘 우클릭 → 옵션 → **Dock에 유지**

### 터미널 기본 명령어 5개

```bash
pwd          # 지금 내가 어느 폴더에 있는지
ls           # 이 폴더 안에 뭐가 있는지
cd 폴더이름   # 그 폴더로 들어가기
cd ..        # 한 단계 위로
cd ~         # 홈 폴더로
```

**꿀팁 3개**
- **Tab 키**: 폴더 이름 몇 글자 치고 Tab을 누르면 나머지가 자동완성된다. **오타를 막아준다**
- **↑ 방향키**: 직전에 친 명령어를 다시 불러온다
- **Finder에서 폴더를 터미널 창으로 드래그**하면 경로가 자동으로 입력된다

### 비밀번호를 물어볼 때

`Password:` 가 나오면 **맥 로그인 비밀번호**를 친다.
**화면에 아무것도 안 보이는 게 정상이다.** (별표도 안 나온다) 그냥 치고 Enter.

---

## 2. 내 맥이 어떤 칩인지 확인 — 이게 중요하다

```bash
uname -m
```

| 결과 | 뜻 |
|---|---|
| `arm64` | **Apple Silicon** (M1, M2, M3, M4) — 2020년 말 이후 대부분 |
| `x86_64` | **Intel** — 그 이전 모델 |

**나중에 프로그램을 받을 때 이걸 물어본다. 메모해둔다.**

---

## 3. Xcode Command Line Tools 설치 (git 등이 여기 들어있다)

```bash
git --version
```

- 버전이 바로 나오면 → 이미 설치됨. **4번으로**
- 팝업창이 뜨면 → **설치** 버튼 클릭 (5~10분 걸린다)

✅ **성공 확인**: `git version 2.x.x`

---

## 4. Homebrew 설치 — 맥의 프로그램 설치 도구

맥에는 `apt` 같은 게 기본으로 없다. Homebrew가 그 역할을 한다.
나중에 AWS CLI와 Terraform을 이걸로 설치할 것이다.

### 4-1. 설치

터미널에 아래를 **한 줄 통째로** 붙여넣고 Enter:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

- 중간에 `Press RETURN to continue` → Enter
- `Password:` → 맥 로그인 비밀번호 (안 보이는 게 정상)
- 5~10분 걸린다

### 4-2. ⚠️ Apple Silicon이면 여기서 한 단계 더 — **가장 많이 놓치는 부분**

설치가 끝나면 마지막에 이런 안내가 나온다:

```
==> Next steps:
- Run these two commands in your terminal to add Homebrew to your PATH:
```

**이걸 그냥 지나치면 `brew: command not found`가 난다.** 아래를 실행한다:

```bash
echo >> ~/.zprofile
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

> Intel 맥이면 이 단계가 필요 없다 (`/usr/local`에 깔려서 자동으로 잡힌다).

### 4-3. 확인

```bash
brew --version
```

✅ **성공 확인**: `Homebrew 4.x.x`

❌ `zsh: command not found: brew` → 4-2를 안 했거나, **터미널을 새로 열어야** 한다
(⌘ + N으로 새 창)

---

## 5. Docker Desktop 설치

프로젝트를 로컬에서 띄우려면 필요하다.

1. https://www.docker.com/products/docker-desktop/ 접속
2. **Download for Mac** 버튼 옆 화살표를 눌러 **2번에서 확인한 칩**에 맞는 걸 고른다
   - `arm64` → **Apple Silicon**
   - `x86_64` → **Intel Chip**
3. 받은 `.dmg` 파일을 더블클릭 → **Docker 아이콘을 Applications 폴더로 드래그**
4. Launchpad(⌘+Space → `Docker`)에서 Docker 실행
5. 약관 동의 → 권한 요청이 뜨면 비밀번호 입력
6. **상단 메뉴바에 고래 아이콘**이 생기고, 클릭했을 때 `Docker Desktop is running` 이면 준비 완료

### 확인

```bash
docker --version
docker compose version
```

✅ **성공 확인**: 둘 다 버전이 나온다

❌ `Cannot connect to the Docker daemon` → **Docker Desktop 앱이 실행 중이 아니다.**
고래 아이콘이 메뉴바에 있는지 확인한다. 맥을 껐다 켜면 Docker도 다시 실행해야 한다.

> 💡 Docker Desktop 설정에서 **Start Docker Desktop when you sign in** 을 켜두면 매번 안 켜도 된다.

---

## 6. 프로젝트 내려받기

아직 맥에 프로젝트가 없다면:

```bash
cd ~                                    # 홈 폴더로
mkdir -p dev && cd dev                  # dev 폴더를 만들고 들어간다
git clone https://github.com/Kimeunho0710/baseball_recommend.git
cd baseball_recommend
git checkout develop
git pull origin develop
```

**GitHub 로그인을 물어보면**: 비밀번호가 아니라 **Personal Access Token**이 필요하다.
GitHub → 우측 상단 프로필 → Settings → Developer settings → Personal access tokens →
Tokens (classic) → Generate new token → `repo` 체크 → 생성된 토큰을 비밀번호 자리에 붙여넣는다.

### 확인

```bash
pwd          # /Users/사용자이름/dev/baseball_recommend
ls           # backend  frontend  docs  docker-compose.yml ... 이 보인다
git log --oneline -3
```

✅ **성공 확인**: 최근 커밋에 `단계별 실행 가이드 추가` 가 보인다

> 이미 프로젝트가 맥 어딘가에 있다면, Finder에서 그 폴더를 찾아
> **터미널 창으로 드래그**하면 경로가 입력된다. 앞에 `cd ` 를 붙이고 Enter.

---

## 7. 여기까지 확인 — 전부 ✅ 여야 다음으로

터미널에 하나씩 쳐본다.

```bash
uname -m                  # arm64 또는 x86_64
git --version             # git version 2.x
brew --version            # Homebrew 4.x
docker --version          # Docker version 2x.x
docker compose version    # Docker Compose version v2.x
pwd                       # 프로젝트 폴더 경로
```

전부 나오면 **`04-step-by-step.md`의 1단계**로 간다.

---

## 8. 맥에서 자주 겪는 것들

| 증상 | 원인과 해결 |
|---|---|
| `zsh: command not found: xxx` | 설치가 안 됐거나 PATH에 없다. **터미널을 새 창으로 열어본다** (⌘+N). 그래도 안 되면 설치 단계를 다시 |
| `Cannot connect to the Docker daemon` | Docker Desktop 앱이 안 켜져 있다. 메뉴바 고래 아이콘 확인 |
| `Permission denied` | 명령 앞에 `sudo ` 를 붙인다. 단, **모르는 명령에 함부로 sudo를 붙이지 않는다** |
| `"개발자를 확인할 수 없기 때문에 열 수 없습니다"` | 시스템 설정 → 개인정보 보호 및 보안 → 아래로 스크롤 → **그래도 열기** |
| 터미널에서 붙여넣기가 안 됨 | Ctrl+V 말고 **⌘ + V** |
| 명령이 안 끝나고 멈춘 것 같음 | 진짜 오래 걸리는 것일 수 있다(빌드 등). 정말 멈췄으면 **Ctrl + C** |
| 한글 폴더명 때문에 경로가 이상함 | 프로젝트는 **영문 경로**에 두는 게 안전하다 (`~/dev/`) |

---

## 9. 알아두면 좋은 것

- **터미널을 껐다 켜도 폴더 위치는 홈(`~`)으로 돌아온다.** 매번 `cd ~/dev/baseball_recommend` 로 이동한다
- **명령어가 긴 건 외우지 않아도 된다.** ↑ 방향키로 불러오거나 이 문서를 다시 보면 된다
- **에러 메시지는 지우지 말고 그대로 복사해둔다.** 물어볼 때 그게 제일 중요한 정보다
