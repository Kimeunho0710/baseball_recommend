<template>
  <div class="detail-page">
    <div v-if="loading" class="loading">불러오는 중...</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <template v-else-if="team">
      <!-- 히어로 배너 -->
      <div class="hero-banner" :style="{ background: `linear-gradient(135deg, ${team.primaryColor}cc, #0f3460)` }">
        <div class="hero-inner">
          <RouterLink to="/teams" class="back-link">← 팀 목록</RouterLink>
          <div class="hero-content">
            <span class="mascot-icon">{{ mascotIcon(team.mascot) }}</span>
            <div>
              <h1 class="team-name">{{ team.name }}</h1>
              <p class="team-sub">📍 {{ team.city }} · 🏟️ {{ team.stadium }}</p>
            </div>
          </div>

          <!-- 섹션 퀵 네비 -->
          <nav class="quick-nav">
            <a v-for="nav in quickNavs" :key="nav.id" :href="`#${nav.id}`" class="qnav-item">{{ nav.label }}</a>
          </nav>
        </div>
      </div>

      <div class="content-area">

        <!-- 핵심 지표 -->
        <div class="stats-row" id="stats">
          <div class="stat-box">
            <span class="stat-num" :style="{ color: team.primaryColor }">{{ team.foundedYear }}</span>
            <span class="stat-label">창단연도</span>
          </div>
          <div class="stat-box">
            <span class="stat-num" :style="{ color: team.primaryColor }">{{ team.championships }}</span>
            <span class="stat-label">우승 횟수</span>
          </div>
          <div class="stat-box">
            <span class="stat-num" :style="{ color: team.primaryColor }">{{ 2026 - team.foundedYear }}</span>
            <span class="stat-label">구단 역사 (년)</span>
          </div>
          <div class="stat-box">
            <span class="stat-num" :style="{ color: team.primaryColor }">{{ team.mascot }}</span>
            <span class="stat-label">마스코트</span>
          </div>
        </div>

        <!-- 팀 소개 -->
        <div class="section" id="intro">
          <h2 class="section-title">팀 소개</h2>
          <p class="description">{{ team.description }}</p>
        </div>

        <!-- 팀 특징 -->
        <div class="section">
          <h2 class="section-title">팀 특징</h2>
          <div class="char-tags">
            <span
              v-for="c in characteristics"
              :key="c"
              class="char-tag"
              :style="{ borderColor: team.primaryColor, color: team.primaryColor }"
            >{{ c }}</span>
          </div>
        </div>

        <!-- 이 팀을 좋아하게 되는 이유 -->
        <div class="section" id="why">
          <h2 class="section-title">이 팀을 좋아하게 되는 이유</h2>
          <div class="why-love-grid">
            <div
              v-for="(reason, i) in teamExtra.whyLove"
              :key="i"
              class="why-love-card"
              :style="{ borderColor: team.primaryColor + '44' }"
            >
              <span class="why-love-icon">{{ reason.icon }}</span>
              <p>{{ reason.text }}</p>
            </div>
          </div>
        </div>

        <!-- 주요 선수 -->
        <div class="section" id="players">
          <h2 class="section-title">주요 선수</h2>
          <div v-if="playersLoading" class="players-loading">선수 정보 불러오는 중...</div>
          <div v-else class="players-grid">
            <div
              v-for="player in players"
              :key="player.id"
              class="player-card"
              :style="{ '--team-color': team.primaryColor }"
            >
              <div class="player-top" :style="{ background: team.primaryColor + '22', borderColor: team.primaryColor + '44' }">
                <span class="jersey-num" :style="{ color: team.primaryColor }">#{{ player.jerseyNumber }}</span>
                <span class="position-badge">{{ player.position }}</span>
              </div>
              <div class="player-body">
                <p class="player-name">{{ player.name }}</p>
                <p class="player-desc">{{ player.description }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 감독 -->
        <div class="section">
          <h2 class="section-title">현재 감독</h2>
          <p class="manager">👨‍💼 {{ team.manager }}</p>
        </div>

        <!-- 팀 역사 타임라인 -->
        <div class="section" id="history">
          <h2 class="section-title">팀 역사 타임라인</h2>
          <div class="timeline">
            <div
              v-for="(item, i) in teamExtra.timeline"
              :key="i"
              class="timeline-item"
            >
              <div class="timeline-left">
                <span class="timeline-year" :style="{ color: team.primaryColor }">{{ item.year }}</span>
              </div>
              <div class="timeline-center">
                <div class="timeline-dot" :style="{ background: team.primaryColor }"></div>
                <div v-if="i < teamExtra.timeline.length - 1" class="timeline-line"></div>
              </div>
              <div class="timeline-right">
                <p class="timeline-event" :class="{ 'is-highlight': item.highlight }">{{ item.event }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 대표 응원 문화 -->
        <div class="section" id="cheer">
          <h2 class="section-title">대표 응원 문화</h2>
          <div class="cheer-list">
            <div
              v-for="cheer in teamExtra.cheerCulture"
              :key="cheer.title"
              class="cheer-item"
            >
              <span class="cheer-icon" :style="{ background: team.primaryColor + '22', color: team.primaryColor }">
                {{ cheer.icon }}
              </span>
              <div class="cheer-text">
                <strong>{{ cheer.title }}</strong>
                <p>{{ cheer.desc }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 영원한 라이벌 -->
        <div class="section" id="rival">
          <h2 class="section-title">영원한 라이벌</h2>
          <div class="rival-card" :style="{ borderColor: teamExtra.rival.color + '66' }">
            <div class="rival-vs">
              <span class="rival-team" :style="{ color: team.primaryColor }">{{ team.name }}</span>
              <span class="rival-vs-label">VS</span>
              <span class="rival-team" :style="{ color: teamExtra.rival.color }">{{ teamExtra.rival.name }}</span>
            </div>
            <p class="rival-desc">{{ teamExtra.rival.description }}</p>
            <div class="rival-tags">
              <span
                v-for="tag in teamExtra.rival.tags"
                :key="tag"
                class="rival-tag"
              >{{ tag }}</span>
            </div>
          </div>
        </div>

        <!-- 입문자가 꼭 봐야 할 경기 -->
        <div class="section" id="watch">
          <h2 class="section-title">입문자가 꼭 봐야 할 경기</h2>
          <div class="watch-list">
            <div
              v-for="(game, i) in teamExtra.watchGames"
              :key="i"
              class="watch-item"
            >
              <div class="watch-num" :style="{ background: team.primaryColor }">{{ i + 1 }}</div>
              <div class="watch-text">
                <strong>{{ game.title }}</strong>
                <p>{{ game.desc }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 이 팀 팬 스타일 -->
        <div class="section" id="fanstyle">
          <h2 class="section-title">이 팀 팬은 보통 이런 스타일</h2>
          <div class="fan-style-card" :style="{ borderColor: team.primaryColor + '55' }">
            <div class="fan-style-header">
              <span class="fan-style-emoji">{{ teamExtra.fanStyle.emoji }}</span>
              <strong class="fan-style-title" :style="{ color: team.primaryColor }">
                {{ teamExtra.fanStyle.title }}
              </strong>
            </div>
            <p class="fan-style-desc">{{ teamExtra.fanStyle.description }}</p>
            <div class="fan-style-tags">
              <span
                v-for="tag in teamExtra.fanStyle.tags"
                :key="tag"
                class="fan-tag"
                :style="{ borderColor: team.primaryColor + '66', color: team.primaryColor }"
              >{{ tag }}</span>
            </div>
          </div>
        </div>

        <!-- 역대 기록 -->
        <div class="section">
          <h2 class="section-title">역대 기록</h2>
          <div class="records">
            <div class="record-item">
              <span class="record-icon">🏆</span>
              <div>
                <div class="record-title">한국시리즈 우승</div>
                <div class="record-value">총 {{ team.championships }}회</div>
              </div>
            </div>
            <div class="record-item">
              <span class="record-icon">📅</span>
              <div>
                <div class="record-title">창단</div>
                <div class="record-value">{{ team.foundedYear }}년 ({{ 2026 - team.foundedYear }}년 역사)</div>
              </div>
            </div>
            <div class="record-item">
              <span class="record-icon">🏟️</span>
              <div>
                <div class="record-title">홈구장</div>
                <div class="record-value">{{ team.stadium }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 입문 가이드 -->
        <div v-if="team.beginnerGuide" class="section" id="guide">
          <h2 class="section-title">입문자 가이드</h2>
          <div class="guide-box" :style="{ borderLeftColor: team.primaryColor }">
            <span class="guide-icon">💡</span>
            <p class="guide-text">{{ team.beginnerGuide }}</p>
          </div>
        </div>

        <!-- 버튼 영역 -->
        <div class="cta-area">
          <RouterLink to="/compare" class="cta-btn compare-btn">
            다른 팀과 비교하기
          </RouterLink>
          <RouterLink to="/survey" class="cta-btn" :style="{ background: team.primaryColor }">
            내가 이 팀에 맞는지 확인하기 →
          </RouterLink>
        </div>

      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { fetchTeam, fetchTeamPlayers } from '../api/surveyApi'

const route = useRoute()
const team = ref(null)
const players = ref([])
const loading = ref(false)
const playersLoading = ref(false)
const error = ref(null)

// ── 퀵 네비게이션 ─────────────────────────────────────
const quickNavs = [
  { id: 'why',      label: '좋아하는 이유' },
  { id: 'players',  label: '선수' },
  { id: 'history',  label: '역사' },
  { id: 'cheer',    label: '응원 문화' },
  { id: 'rival',    label: '라이벌' },
  { id: 'watch',    label: '경기 추천' },
  { id: 'fanstyle', label: '팬 스타일' },
  { id: 'guide',    label: '입문 가이드' },
]

// ── 마스코트 아이콘 ────────────────────────────────────
const mascotIconMap = {
  '호랑이': '🐯', '사자': '🦁', '쌍둥이': '👯', '곰': '🐻',
  '마법사': '🧙', '랜더': '🚀', '갈매기': '🐦', '독수리': '🦅',
  '공룡': '🦕', '영웅': '⚡'
}
function mascotIcon(mascot) { return mascotIconMap[mascot] || '⚾' }

const characteristics = computed(() =>
  team.value?.characteristics
    ? team.value.characteristics.split(',').map(c => c.trim())
    : []
)

// ── 팀별 확장 데이터 ──────────────────────────────────
const TEAM_EXTRA = {
  'KIA 타이거즈': {
    whyLove: [
      { icon: '🏆', text: 'KBO 역대 최다 11회 우승. 역사적인 팀을 응원하는 자부심이 남달라요.' },
      { icon: '⚡', text: '2024 MVP 김도영의 폭발적인 플레이. 시대의 스타와 함께하는 설렘이 있어요.' },
      { icon: '🔥', text: '광주 홈 응원의 열기는 KBO 최고 수준. 경기장에 가면 빠져나올 수 없어요.' },
      { icon: '🐯', text: '해태 시절부터 이어진 40년 전통. 한국 야구사 자체를 응원하는 느낌이에요.' },
    ],
    timeline: [
      { year: 1982, event: '해태 타이거즈로 창단' },
      { year: 1983, event: '첫 번째 한국시리즈 우승' },
      { year: 1986, event: '3년 연속 우승 시작 (1986~1988)', highlight: true },
      { year: 1993, event: '8년 만의 우승 재개' },
      { year: 1996, event: '연속 우승 (1996~1997)', highlight: true },
      { year: 2001, event: 'KIA 자동차 인수 → KIA 타이거즈로 재창단' },
      { year: 2009, event: 'KIA 시대 첫 우승', highlight: true },
      { year: 2017, event: '9번째 우승' },
      { year: 2024, event: 'KBO 역대 최다 11번째 우승 — 신기록 달성', highlight: true },
    ],
    cheerCulture: [
      { icon: '🎺', title: '빨간 물결 응원', desc: '홈 경기마다 챔피언스 필드를 가득 채우는 빨간 응원복과 응원도구가 장관이에요.' },
      { icon: '🎵', title: '선수별 개인 응원가', desc: '각 선수마다 팬들이 만든 응원가가 있어요. 경기장에서 따라 부르는 순간 완전한 팬이 돼요.' },
      { icon: '🦁', title: '원정 응원도 강력', desc: 'KIA 팬들은 원정지에서도 홈처럼 경기장을 메워요. 어디서나 KIA 팬을 만날 수 있어요.' },
    ],
    rival: {
      name: '롯데 자이언츠',
      color: '#4a6fa5',
      description: '광주와 부산, 호남과 영남의 지역 감정이 담긴 KBO 최고의 전통 라이벌전이에요. 두 팀이 맞붙는 날이면 전국이 주목해요.',
      tags: ['호남-영남 더비', '40년 전통', '지역 자존심 대결'],
    },
    watchGames: [
      { title: 'KIA vs 롯데 더비', desc: '호남-영남 라이벌전. 두 팀 팬의 열기가 폭발하는 KBO 최고의 전통 매치예요.' },
      { title: '홈 개막전', desc: '챔피언스 필드의 빨간 물결을 처음 경험하기 가장 좋은 날이에요.' },
      { title: '포스트시즌 경기', desc: 'KIA는 포스트시즌에 강해요. 이때의 경기장 분위기는 정규시즌과 차원이 달라요.' },
    ],
    fanStyle: {
      emoji: '🏆',
      title: '전통과 역사를 중시하는 진성 팬',
      description: '해태 시절부터 이어온 역사에 자부심이 강해요. 성적에 일희일비하기보다 팀의 여정을 함께한다는 의미를 더 소중히 여겨요. 광주 연고 팬들은 지역 정체성과 야구를 완전히 하나로 느껴요.',
      tags: ['역사 중시', '지역 애착', '우승 DNA', '진성 팬'],
    },
  },

  '삼성 라이온즈': {
    whyLove: [
      { icon: '👑', text: '2011~2014년 4년 연속 우승 왕조의 자부심. KBO에서 유일한 4연패 기록이에요.' },
      { icon: '📊', text: '체계적이고 탄탄한 구단 운영. 팀이 어떻게 돌아가는지 알아갈수록 더 빠져들어요.' },
      { icon: '⚾', text: '원태인의 정교한 투구. 교과서 같은 피칭을 감상하는 재미가 독보적이에요.' },
      { icon: '🦁', text: '대구 삼성 라이온즈 파크는 KBO 최고 수준의 시설을 자랑해요.' },
    ],
    timeline: [
      { year: 1982, event: '삼성 라이온즈 창단' },
      { year: 1985, event: '첫 번째 한국시리즈 우승', highlight: true },
      { year: 2002, event: '8년 만의 우승 재개' },
      { year: 2005, event: '연속 우승 (2005~2006)' },
      { year: 2011, event: '4년 연속 우승 시작 (2011~2014) — KBO 최장 연속 우승 신기록', highlight: true },
      { year: 2014, event: '4연패 달성, 완전한 왕조 완성', highlight: true },
      { year: 2016, event: '대구 삼성 라이온즈 파크 개장 — KBO 최고 시설 구장 완성' },
      { year: 2020, event: '구단 대대적 세대교체 및 재건 시작' },
    ],
    cheerCulture: [
      { icon: '💙', title: '파란 물결 응원', desc: '대구 라이온즈 파크를 가득 메우는 파란색 응원이 인상적이에요. 차분하면서도 강렬해요.' },
      { icon: '🎵', title: '정통 응원 문화', desc: '응원단의 리드에 맞춰 정해진 응원가를 함께 부르는 전통적인 야구 응원 문화예요.' },
      { icon: '🏟️', title: '최고급 구장 경험', desc: '2016년 개장한 라이온즈 파크는 최신식 시설과 편의성으로 최고의 관람 환경을 제공해요.' },
    ],
    rival: {
      name: 'KIA 타이거즈',
      color: '#EA0029',
      description: '2010년대 포스트시즌에서 가장 자주 만났던 맞수. 두 팀이 모두 강할 때 맞붙는 경기는 KBO 최고 수준의 긴장감을 보여줘요.',
      tags: ['포스트시즌 단골 맞대결', '명문 대 명문', '전략전 최고'],
    },
    watchGames: [
      { title: '삼성 vs KIA 포스트시즌', desc: '두 명문이 포스트시즌에서 맞붙는 경기는 KBO 역사상 가장 긴장감 넘치는 장면들을 만들어왔어요.' },
      { title: '원태인 선발 경기', desc: '원태인의 투구는 야구의 투수 전술을 배우기 가장 좋은 교과서예요. 어떻게 타자를 잡는지 주목해봐요.' },
      { title: '대구 홈 경기', desc: '라이온즈 파크에서 경기를 보면 KBO에서 가장 편안하고 쾌적한 관람 경험을 할 수 있어요.' },
    ],
    fanStyle: {
      emoji: '📊',
      title: '전략 분석을 즐기는 이성적 팬',
      description: '감정보다 분석으로 야구를 즐기는 팬이 많아요. 왜 저 타자를 여기서 올리는지, 왜 투수를 교체하는지 이해하며 관람해요. 4연패 왕조를 직접 경험한 팬들은 강팀의 기준이 높아요.',
      tags: ['전략 중시', '분석형', '명문 의식', '냉정한 시각'],
    },
  },

  'LG 트윈스': {
    whyLove: [
      { icon: '😭', text: '29년의 기다림 끝에 2023년 우승. 그 감동을 함께한 팬들의 눈물을 잊을 수 없어요.' },
      { icon: '💛', text: '오지환이라는 진정한 프랜차이즈 스타. 한 팀만 바라보는 충성심이 팬들에게 감동을 줘요.' },
      { icon: '🌆', text: '잠실이라는 서울 한복판의 야구장. 경기 후 강남, 잠실 일대와 연결되는 도심 감성이 독보적이에요.' },
      { icon: '✨', text: '트렌디한 팬 문화와 세련된 굿즈. 야구를 라이프스타일로 즐기는 팬들이 많아요.' },
    ],
    timeline: [
      { year: 1990, event: 'MBC 청룡을 인수해 LG 트윈스로 창단 및 첫 우승', highlight: true },
      { year: 1994, event: '두 번째 우승', highlight: true },
      { year: 1994, event: '이후 29년간 우승 없는 긴 가뭄 시작' },
      { year: 2002, event: '잠실야구장을 두산과 함께 홈구장으로 공유' },
      { year: 2023, event: '29년 만에 한국시리즈 우승 — 팬들의 한을 풀다', highlight: true },
    ],
    cheerCulture: [
      { icon: '❤️', title: '트윈스 응원가', desc: '오지환, 박해민 등 인기 선수들의 개인 응원가가 특히 유명해요. 서울 감성이 담긴 경쾌한 분위기예요.' },
      { icon: '🌃', title: '잠실 야경과 함께하는 야구', desc: '잠실 야구장의 야경은 KBO에서 손꼽히는 명장면이에요. 서울 한복판 나이트 게임의 감성이 있어요.' },
      { icon: '👯', title: '쌍둥이 컨셉 팬 문화', desc: '마스코트 쌍둥이 컨셉을 활용한 귀여운 굿즈와 팬 문화가 발달해 있어요.' },
    ],
    rival: {
      name: '두산 베어스',
      color: '#4a4a8a',
      description: '같은 잠실 야구장을 홈으로 쓰는 진짜 라이벌. 홈경기조차 상대 팬과 나눠 써야 하는 특수한 관계예요. 잠실 더비는 KBO 최고의 라이벌 매치예요.',
      tags: ['잠실 더비', '공동 홈구장', '서울 패권 다툼'],
    },
    watchGames: [
      { title: 'LG vs 두산 잠실 더비', desc: '같은 구장을 쓰는 두 팀의 대결. 관중석이 두 팀 팬으로 반반 나뉘는 독특한 분위기예요.' },
      { title: '오지환 시즌 특별 경기', desc: '오지환의 홈런이나 결정적인 플레이 순간에 잠실이 들썩이는 경험을 해봐요.' },
      { title: '잠실 야간 경기', desc: '서울 도심 한복판에서 야경을 보며 즐기는 야구는 LG 팬이 아니어도 경험해 볼 만해요.' },
    ],
    fanStyle: {
      emoji: '✨',
      title: '감성과 트렌드를 즐기는 서울 팬',
      description: '야구를 문화와 라이프스타일로 소비하는 팬이 많아요. 굿즈, 응원복, SNS 공유에 적극적이에요. 29년 한을 기억하는 올드팬과 2023 우승 이후 새롭게 유입된 뉴팬이 공존해요.',
      tags: ['서울 감성', '트렌디', '감성 팬', '라이프스타일형'],
    },
  },

  '두산 베어스': {
    whyLove: [
      { icon: '💪', text: '절대 포기하지 않는 두산 야구. 9회에도 역전이 일어나는 팀을 응원하면 끝까지 눈을 뗄 수 없어요.' },
      { icon: '📜', text: '1982년 OB 베어스부터 이어진 KBO 원년 구단의 역사. 한국 야구의 시작과 함께한 팀이에요.' },
      { icon: '🐻', text: '이승엽 감독이라는 KBO 레전드의 귀환. 레전드의 감독 커리어를 지금 목격할 수 있어요.' },
      { icon: '⚔️', text: '잠실 더비의 주인공. LG와의 라이벌전은 가장 치열하고 재미있는 경기예요.' },
    ],
    timeline: [
      { year: 1982, event: 'OB 베어스로 창단 — KBO 원년 멤버', highlight: true },
      { year: 1982, event: '원년 한국시리즈 우승' },
      { year: 1983, event: '2연패 달성' },
      { year: 1995, event: '세 번째 우승' },
      { year: 2001, event: '두산 그룹 인수 → 두산 베어스로 재창단' },
      { year: 2015, event: '4번째 우승 — 두산 시대 첫 우승', highlight: true },
      { year: 2016, event: '5번째 우승 연속 달성' },
      { year: 2019, event: '6번째 우승', highlight: true },
      { year: 2024, event: '홈런왕 이승엽이 감독으로 취임해 재건 가도' },
    ],
    cheerCulture: [
      { icon: '🐻', title: '베어스 특유의 끈끈한 응원', desc: '두산 팬들은 어떤 상황에도 팀을 믿어요. 점수가 뒤져도 응원의 열기가 식지 않아요.' },
      { icon: '⚾', title: '잠실 더비 분위기', desc: 'LG와의 더비 경기 때 관중석이 두 색으로 갈리는 장면은 KBO 최고의 명장면 중 하나예요.' },
      { icon: '📣', title: '역전 드라마 연출', desc: '9회 역전 순간 터지는 잠실의 함성은 일생에 한 번은 꼭 경험해야 할 장면이에요.' },
    ],
    rival: {
      name: 'LG 트윈스',
      color: '#C30452',
      description: '같은 잠실야구장을 공유하는 유일한 라이벌 관계. 어느 경기보다 팬들의 감정이 격해지는 잠실 더비예요.',
      tags: ['잠실 더비', '같은 홈구장', 'KBO 최고 라이벌'],
    },
    watchGames: [
      { title: '두산 vs LG 잠실 더비', desc: '잠실구장이 두 팀의 팬으로 갈라지는 순간은 KBO에서 가장 드라마틱한 장면이에요.' },
      { title: '9회 이후에도 주목', desc: '두산 경기는 끝날 때까지 끝난 게 아니에요. 9회 역전 드라마를 목격하면 진짜 팬이 돼요.' },
      { title: '포스트시즌', desc: '두산은 포스트시즌에서 진가를 발휘해요. 2015~2019년 동안 매년 한국시리즈에 진출한 팀이에요.' },
    ],
    fanStyle: {
      emoji: '🐻',
      title: '뚝심과 끈기로 응원하는 저력형 팬',
      description: '포기 없는 두산 야구처럼 팬들도 끝까지 믿고 응원해요. 역전 드라마를 많이 경험한 팬들이라 어떤 상황에서도 희망을 잃지 않아요. OB 시절부터 알아온 중장년 팬들도 많아요.',
      tags: ['뚝심형', '끈기', '역전 경험자', '원년 팬'],
    },
  },

  'KT 위즈': {
    whyLove: [
      { icon: '🚀', text: '창단 7년 만의 첫 우승. 언더독이 강자를 꺾는 드라마를 응원하는 느낌이 짜릿해요.' },
      { icon: '💥', text: '강백호라는 KBO 최고 스타의 폭발적인 타격. 스윙 하나하나가 팬들을 열광시켜요.' },
      { icon: '🌱', text: '아직 역사가 짧아 새로운 역사를 함께 쓰는 느낌. 팬으로서 성장하는 과정을 함께할 수 있어요.' },
      { icon: '🏙️', text: '수원·경기도 연고팀. 서울-경기 지역 팬들이 응원하기 가장 가까운 팀 중 하나예요.' },
    ],
    timeline: [
      { year: 2015, event: 'KT 위즈 창단 — KBO 10번째 구단', highlight: true },
      { year: 2015, event: '퓨처스리그(2군)로 첫 시즌 시작' },
      { year: 2016, event: '1군 정규 시즌 첫 참가' },
      { year: 2018, event: '강백호 신인왕 — KT의 스타 탄생', highlight: true },
      { year: 2021, event: '창단 7년 만에 한국시리즈 첫 우승 달성 — KBO 역대 최단기간 우승', highlight: true },
    ],
    cheerCulture: [
      { icon: '🧙', title: '마법사 응원 문화', desc: '마스코트 마법사 컨셉에 맞는 개성 있는 응원이에요. 젊은 팬층이 많아 활기차고 트렌디한 분위기예요.' },
      { icon: '🎪', title: '수원 위즈파크의 열기', desc: '수원 KT 위즈파크는 가성비 좋고 접근성 뛰어난 구장이에요. 경기도 야구 문화의 중심지가 되고 있어요.' },
      { icon: '📱', title: '젊은 팬덤', desc: 'KBO 팀 중 가장 젊은 팬덤을 보유해요. SNS와 디지털 문화에 적극적인 팬들이 많아요.' },
    ],
    rival: {
      name: 'NC 다이노스',
      color: '#2a5ca8',
      description: '같은 시기 신생팀으로 출발해 비슷한 성장 과정을 밟아온 라이벌. 두 팀 모두 창단 초기의 어려움을 극복하고 빠르게 강팀이 된 공통점이 있어요.',
      tags: ['신생팀 더비', '성장 스토리 라이벌', '2020년대 경쟁'],
    },
    watchGames: [
      { title: '강백호 타석 집중 관람', desc: '강백호의 타석 접근법과 스윙을 분석하며 봐요. 어떤 공을 노리는지 파악하면 타격의 재미가 배가돼요.' },
      { title: '2021 한국시리즈 하이라이트', desc: '창단 7년 만의 우승 여정을 유튜브로 찾아봐요. KT 팬이 되기 전 꼭 봐야 할 영상이에요.' },
      { title: '수원 홈 경기', desc: '서울보다 덜 붐비지만 열기는 뜨거운 수원 위즈파크. 편안하게 첫 야구 경험을 하기 좋은 구장이에요.' },
    ],
    fanStyle: {
      emoji: '🚀',
      title: '성장 스토리를 함께하는 도전형 팬',
      description: 'KT의 성장 스토리와 함께 팬이 된 경우가 많아요. 강백호 같은 스타에 열광하며 팀의 도전적인 야구에 공감해요. 상대적으로 젊은 팬층이 많고 야구 입문자도 많아요.',
      tags: ['도전형', '젊은 팬', '스타 중심', '성장 동행'],
    },
  },

  'SSG 랜더스': {
    whyLove: [
      { icon: '💣', text: '최정의 KBO 역대 홈런 신기록. 살아있는 레전드의 홈런을 실시간으로 목격할 수 있어요.' },
      { icon: '✈️', text: 'MLB 경험을 바탕으로 돌아온 김광현. 세계 수준의 투구를 KBO에서 볼 수 있는 특권이에요.' },
      { icon: '🔥', text: 'SK 와이번스 시절부터 이어온 강팀 DNA. 인천 팬들의 자부심이 매우 강해요.' },
      { icon: '⚡', text: '화끈한 공격 야구. 점수가 펑펑 나는 경기를 자주 볼 수 있어요.' },
    ],
    timeline: [
      { year: 2000, event: 'SK 와이번스 창단 — SK 텔레콤이 인수', highlight: true },
      { year: 2007, event: '첫 한국시리즈 우승', highlight: true },
      { year: 2008, event: '2연패 달성' },
      { year: 2010, event: '세 번째 우승' },
      { year: 2018, event: '네 번째 우승', highlight: true },
      { year: 2021, event: 'SSG 랜더스로 구단명 변경 — 신세계 그룹 인수' },
      { year: 2022, event: 'SSG 체제 첫 우승 (5번째)', highlight: true },
    ],
    cheerCulture: [
      { icon: '🔴', title: '인천의 붉은 열정', desc: 'SSG 랜더스 필드는 인천 팬들의 강렬한 응원으로 유명해요. 원정팀이 분위기에 압도되는 경우가 많아요.' },
      { icon: '⚾', title: '강타자 팀의 홈런 응원', desc: '최정 홈런 때의 팬들 반응은 KBO 최고 수준이에요. 홈런 예감이 오는 타석이면 미리부터 분위기가 달아올라요.' },
      { icon: '🏆', title: '우승 경험이 만든 자부심', desc: '5번의 우승 경험이 있는 팬들은 기준이 높아요. 하지만 그만큼 우승의 짜릿함을 가장 잘 아는 팬들이에요.' },
    ],
    rival: {
      name: '롯데 자이언츠',
      color: '#4a6fa5',
      description: '인천과 부산, 두 항구 도시의 자존심을 건 라이벌. 인천-부산 항구 더비로도 불리며 두 팀 팬들의 감정이 뜨겁게 달아오르는 경기예요.',
      tags: ['항구 더비', '인천-부산', '경상-경기 자존심'],
    },
    watchGames: [
      { title: '최정 통산 홈런 도전 경기', desc: '최정이 KBO 역대 홈런 기록을 갱신하는 순간이 언제 올지 모르니 매 경기 주목해봐요.' },
      { title: '김광현 선발 경기', desc: 'MLB에서 검증된 세계적 투수의 피칭을 직접 볼 수 있는 기회예요. 타자를 요리하는 과정이 일품이에요.' },
      { title: 'SSG vs 롯데 항구 더비', desc: '인천과 부산 팬들의 열기가 충돌하는 경기예요. 두 팀의 응원 문화 차이도 관전 포인트예요.' },
    ],
    fanStyle: {
      emoji: '💪',
      title: '강팀 DNA를 믿는 자부심 강한 팬',
      description: 'SK 시절부터 쌓아온 우승 경험이 팬들에게 강한 자부심을 심어줬어요. 성적이 조금 부진해도 언제든 우승 경쟁을 할 수 있다는 믿음이 있어요. 인천 연고 팬들의 지역 애착도 강해요.',
      tags: ['자부심 강함', '우승 경험자', '인천 연고', '강팀 기대'],
    },
  },

  '롯데 자이언츠': {
    whyLove: [
      { icon: '🐦', text: '갈매기 응원단과 사직구장의 응원 문화는 KBO 최고예요. 한 번 가면 절대 잊을 수 없어요.' },
      { icon: '🌊', text: '부산 전체가 롯데 팬. 그 도시 전체의 감성과 열정이 경기장 안으로 쏟아져 들어와요.' },
      { icon: '💔', text: '1992년 이후 우승이 없는 기다림의 팀. 언젠가 올 그날을 함께 기다리는 특별한 연대감이 있어요.' },
      { icon: '⚾', text: 'KBO에서 가장 오래된 연고 구단 중 하나. 한국 야구의 살아있는 역사예요.' },
    ],
    timeline: [
      { year: 1975, event: '롯데 자이언츠 창단 — KBO 원년 이전부터 존재한 최고(最古) 팀', highlight: true },
      { year: 1984, event: '첫 번째 한국시리즈 우승', highlight: true },
      { year: 1992, event: '두 번째 우승 — 이후 현재까지 우승 없음', highlight: true },
      { year: 2000, event: '사직야구장 재단장 — 갈매기 응원단 문화 정착' },
      { year: 2008, event: '한국시리즈 준우승 — 아쉬운 무관의 연속' },
      { year: 2024, event: '레전드 감독 김태형 부임 — 재건 시작' },
    ],
    cheerCulture: [
      { icon: '🐦', title: '갈매기 응원단', desc: '부산 갈매기를 상징하는 응원단의 열정적인 응원은 KBO 최고예요. 치어리더와 팬이 하나가 돼요.' },
      { icon: '🎵', title: '찬조출연 응원가 문화', desc: '롯데 팬들의 응원가는 독보적이에요. 경기 내내 멈추지 않는 응원가가 사직구장을 하나로 만들어요.' },
      { icon: '🌊', title: '부산 전체가 응원단', desc: '홈 경기가 있는 날 부산 시내 분위기가 달라져요. 경기장 밖에서도 롯데 팬임을 느낄 수 있어요.' },
    ],
    rival: {
      name: 'KIA 타이거즈',
      color: '#EA0029',
      description: '호남과 영남을 대표하는 두 팀의 지역 감정이 담긴 KBO 최고의 전통 라이벌. 경기가 열리는 날이면 전국이 주목하는 특별한 매치예요.',
      tags: ['호남-영남 더비', 'KBO 최고 전통 라이벌', '지역 자존심'],
    },
    watchGames: [
      { title: '사직구장 홈 경기', desc: '갈매기 응원단의 열기를 직접 경험해봐요. 사직 외야석은 KBO에서 가장 열광적인 응원 현장이에요.' },
      { title: '롯데 vs KIA 호남-영남 더비', desc: '두 팀이 맞붙으면 전국이 주목해요. 지역 감정이 담긴 라이벌전의 분위기는 다른 경기와 차원이 달라요.' },
      { title: '부산 나들이와 함께', desc: '부산 여행을 계획한다면 롯데 홈 경기를 넣어봐요. 야구 + 부산 감성의 조합은 최고예요.' },
    ],
    fanStyle: {
      emoji: '💙',
      title: '한(恨)과 열정이 공존하는 뚝심 팬',
      description: '1992년 이후 우승을 못 했지만 팬들의 충성심은 KBO 최고예요. 기다림이 오히려 팬심을 더 강하게 만들었어요. 부산이라는 도시 정체성과 야구가 완전히 하나로 연결돼 있어요.',
      tags: ['뚝심 팬', '부산 정체성', '기다림의 팬심', '최강 충성도'],
    },
  },

  '한화 이글스': {
    whyLove: [
      { icon: '🌏', text: 'MLB 투수 류현진이 지금 이 팀에서 뛰어요. 세계 수준의 투구를 KBO에서 볼 수 있는 기회예요.' },
      { icon: '🔥', text: '대전 팬들의 끈끈한 응원 문화는 KBO에서도 독보적이에요. 팀이 어려울수록 더 뭉치는 진성 팬심이에요.' },
      { icon: '💪', text: '재건의 드라마를 함께 쓰는 느낌. 노시환 등 젊은 스타들과 팀이 성장하는 과정을 직접 봐요.' },
      { icon: '🦅', text: '대전 이글스 파크의 오렌지 물결. 주황색으로 물든 경기장 풍경은 특별해요.' },
    ],
    timeline: [
      { year: 1986, event: '빙그레 이글스로 창단' },
      { year: 1999, event: '첫 번째이자 현재까지 유일한 한국시리즈 우승', highlight: true },
      { year: 2000, event: '한화 그룹이 인수 → 한화 이글스로 재창단' },
      { year: 2006, event: '류현진 입단 — KBO 최고 투수로 성장', highlight: true },
      { year: 2012, event: '류현진 메이저리그 진출 — LA 다저스와 계약' },
      { year: 2022, event: '류현진 KBO 복귀 — 한화로 돌아옴', highlight: true },
      { year: 2024, event: '노시환 홈런 선두 달성 — 재건의 희망' },
    ],
    cheerCulture: [
      { icon: '🟠', title: '오렌지 물결 응원', desc: '한화 특유의 주황색 응원복으로 채워진 이글스 파크는 독보적인 색감이에요.' },
      { icon: '❤️', title: '팬과 팀의 강한 유대감', desc: '성적이 어려울 때도 팬들이 끝까지 함께하는 문화가 정착됐어요. KBO에서 가장 끈끈한 팬-구단 관계예요.' },
      { icon: '🦅', title: '대전 야구 문화', desc: '대전 시민들에게 한화는 단순한 팀이 아니에요. 지역 문화의 일부로 자리잡았어요.' },
    ],
    rival: {
      name: 'KT 위즈',
      color: '#aaaaaa',
      description: '중부권 팀으로 비슷한 연고 지역을 공유하며 묘한 경쟁 관계가 형성됐어요. 두 팀 모두 최근 재건을 향해 나아가는 공통점이 있어요.',
      tags: ['중부권 라이벌', '재건 경쟁', '팬심 대결'],
    },
    watchGames: [
      { title: '류현진 선발 경기', desc: 'MLB에서 돌아온 류현진의 투구는 지금 당장 봐야 할 KBO 최고의 볼거리예요. 예고 경기를 꼭 챙겨봐요.' },
      { title: '노시환 타석 주목', desc: '한화 재건의 주인공 노시환의 홈런 타격을 주목해봐요. 터질 듯한 파워가 매력이에요.' },
      { title: '이글스 파크 홈 경기', desc: '오렌지 물결의 대전 이글스 파크 분위기를 직접 경험해봐요. 팬들의 열정이 경기장을 뜨겁게 달구어요.' },
    ],
    fanStyle: {
      emoji: '🦅',
      title: '팀과 함께 고난을 견디는 의리 팬',
      description: '성적이 좋지 않아도 팀을 지키는 팬들이 많아요. 이 팬들의 의리와 열정은 KBO에서 가장 감동적이에요. 류현진 복귀 이후 새로운 희망을 품고 팀의 부활을 함께 만들어가고 있어요.',
      tags: ['의리형', '끈기 팬', '재건 동반자', '대전 연고'],
    },
  },

  'NC 다이노스': {
    whyLove: [
      { icon: '📊', text: '데이터와 분석 기반의 스마트한 야구. 왜 저 투수를 올리는지, 왜 타자를 저렇게 잡는지 이해할수록 재미있어요.' },
      { icon: '🦕', text: '2020년 창단 9년 만의 우승. 데이터로 야구를 이기는 스마트한 팀이에요.' },
      { icon: '🏟️', text: '창원 NC 파크는 KBO에서 가장 아름다운 구장 중 하나로 꼽혀요.' },
      { icon: '⚾', text: '손아섭, 박민우 등 정교한 타자들의 플레이. 화끈한 홈런보다 깊은 야구를 배울 수 있어요.' },
    ],
    timeline: [
      { year: 2012, event: 'NC 다이노스 창단', highlight: true },
      { year: 2013, event: '1군 정규시즌 첫 참가' },
      { year: 2014, event: '첫 포스트시즌 진출 — 빠른 성장세' },
      { year: 2016, event: '한국시리즈 준우승 — 우승 문턱에서 아쉬움' },
      { year: 2019, event: '창원 NC 파크 개장 — 홈구장 완성', highlight: true },
      { year: 2020, event: '창단 9년 만에 첫 한국시리즈 우승', highlight: true },
    ],
    cheerCulture: [
      { icon: '💙', title: '창원 NC 파크의 감성', desc: '2019년 개장한 NC 파크는 KBO에서 가장 세련된 구장이에요. 경기 외적인 경험도 뛰어나요.' },
      { icon: '🦕', title: '공룡 응원 문화', desc: '공룡 마스코트를 활용한 개성 있는 응원 문화예요. 창단 이후 독자적인 팬덤을 빠르게 형성했어요.' },
      { icon: '📣', title: '경남 야구 팬의 성지', desc: 'NC 창단 이후 경남 지역 야구 팬들의 구심점이 됐어요. 창원 지역의 야구 문화를 이끌어가고 있어요.' },
    ],
    rival: {
      name: '롯데 자이언츠',
      color: '#4a6fa5',
      description: '같은 경남권을 연고로 하는 진짜 라이벌. 창원과 부산, 두 경남 도시의 자존심을 건 경기예요. NC 창단 이후 자연스럽게 형성된 지역 라이벌 관계예요.',
      tags: ['경남 더비', '창원 vs 부산', '영남 패권 다툼'],
    },
    watchGames: [
      { title: 'NC vs 롯데 경남 더비', desc: '창원과 부산의 지역 자존심 대결. 경남 야구의 주도권을 놓고 벌이는 경기예요.' },
      { title: '손아섭의 타격 교과서', desc: '정교한 배트 컨트롤의 달인 손아섭의 타석을 분석해봐요. 타격의 깊이를 배울 수 있어요.' },
      { title: '창원 NC 파크 직관', desc: 'KBO에서 가장 아름다운 구장이에요. 경기 결과와 상관없이 직접 가볼 가치가 있어요.' },
    ],
    fanStyle: {
      emoji: '📊',
      title: '데이터와 전략을 즐기는 분석형 팬',
      description: '야구를 깊이 이해하며 즐기는 팬들이 많아요. NC의 데이터 야구 철학에 공감하는 팬들이 자연스럽게 모여요. 2020 우승을 경험한 팬들은 팀이 어떻게 강팀이 되는지 알고 있어요.',
      tags: ['분석형', '전략 이해', '데이터 야구 지지자', '창원 연고'],
    },
  },

  '키움 히어로즈': {
    whyLove: [
      { icon: '🏛️', text: '국내 유일의 돔구장 고척 스카이돔. 날씨 걱정 없이 사계절 야구를 즐길 수 있는 유일한 팀이에요.' },
      { icon: '🌱', text: '자체 육성의 달인. 류현진, 이정후 등 KBO 최고 스타들을 키워낸 팀이에요. 다음 스타를 먼저 발견하는 재미가 있어요.' },
      { icon: '⚡', text: '안우진이라는 KBO 최고 강속구 투수. 직구 하나하나가 타자를 압도하는 장면이 짜릿해요.' },
      { icon: '💡', text: '저비용 고효율 구단 운영. 작은 예산으로도 포스트시즌에 꾸준히 진출하는 팀의 전략이 흥미로워요.' },
    ],
    timeline: [
      { year: 2008, event: '우리 히어로즈로 창단', highlight: true },
      { year: 2010, event: '넥센 히어로즈로 구단명 변경' },
      { year: 2014, event: '고척 스카이돔 개장 — 국내 유일 돔구장 입주', highlight: true },
      { year: 2019, event: '한국시리즈 준우승 — 첫 우승 문턱' },
      { year: 2019, event: '키움 히어로즈로 구단명 변경' },
      { year: 2020, event: '한국시리즈 준우승 — 2년 연속 준우승' },
      { year: 2023, event: '이정후 MLB 진출 — KBO 스타 배출 전통 이어감' },
    ],
    cheerCulture: [
      { icon: '🏛️', title: '돔구장 특유의 응원 문화', desc: '실내 돔의 폐쇄적 공간에서 울리는 응원 함성은 일반 구장과 완전히 다른 경험이에요.' },
      { icon: '⚡', title: '영웅 팬덤', desc: '히어로즈라는 팀명처럼 팬들도 영웅이 되는 느낌의 응원 문화예요. 자부심 있는 팬덤이 형성돼 있어요.' },
      { icon: '🌱', title: '신인 발굴의 기쁨', desc: '키움 팬들은 팀이 키운 신인이 KBO 스타가 되는 과정을 함께해왔어요. 유망주 발굴에 예민한 팬들이 많아요.' },
    ],
    rival: {
      name: 'LG 트윈스',
      color: '#C30452',
      description: '서울을 연고로 하는 두 팀의 수도권 라이벌. 고척과 잠실, 서울의 두 야구장을 홈으로 하는 팀들의 맞대결이에요.',
      tags: ['서울 라이벌', '수도권 더비', '돔 vs 일반 구장'],
    },
    watchGames: [
      { title: '고척 돔 경기', desc: '국내 유일의 돔구장을 직접 경험해봐요. 실내 음향과 분위기가 일반 구장과 완전히 달라요.' },
      { title: '안우진 선발 경기', desc: 'KBO 최고 수준의 강속구 투수 안우진의 투구를 감상해봐요. 직구 하나로 타자를 제압하는 장면이 압권이에요.' },
      { title: '젊은 신인 주목 경기', desc: '키움이 키운 새로운 유망주를 먼저 발견해봐요. 다음 스타를 먼저 아는 키움 팬이 되는 첫 걸음이에요.' },
    ],
    fanStyle: {
      emoji: '🌱',
      title: '자체 육성 스타를 사랑하는 발굴형 팬',
      description: '키움이 키운 선수들이 KBO 스타가 되는 과정을 즐겨요. 유망주를 먼저 발견하는 안목을 자부심으로 여기는 팬들이 많아요. 돔구장이라는 독특한 환경도 팬덤의 특색을 만들어요.',
      tags: ['발굴형', '유망주 선호', '서울 연고', '돔 마니아'],
    },
  },
}

const teamExtra = computed(() =>
  team.value ? (TEAM_EXTRA[team.value.name] || {
    whyLove: [], timeline: [], cheerCulture: [],
    rival: { name: '-', color: '#888', description: '', tags: [] },
    watchGames: [],
    fanStyle: { emoji: '⚾', title: '', description: '', tags: [] },
  }) : {}
)

onMounted(async () => {
  loading.value = true
  try {
    team.value = await fetchTeam(route.params.id)
  } catch {
    error.value = '팀 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }

  playersLoading.value = true
  try {
    players.value = await fetchTeamPlayers(route.params.id)
  } finally {
    playersLoading.value = false
  }
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: white;
  padding-top: 60px;
}

.loading, .error {
  text-align: center;
  color: #a8d8ea;
  padding: 100px 20px;
}

/* ── 히어로 ── */
.hero-banner {
  padding: 40px 20px 0;
}

.hero-inner {
  max-width: 800px;
  margin: 0 auto;
}

.back-link {
  color: rgba(255,255,255,0.7);
  text-decoration: none;
  font-size: 0.9rem;
  display: inline-block;
  margin-bottom: 20px;
}
.back-link:hover { color: white; }

.hero-content {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.mascot-icon { font-size: 4rem; }

.team-name {
  font-size: 2.4rem;
  font-weight: 800;
  margin-bottom: 8px;
}

.team-sub {
  color: rgba(255,255,255,0.75);
  font-size: 0.95rem;
}

/* ── 퀵 네비 ── */
.quick-nav {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding-bottom: 20px;
}

.qnav-item {
  padding: 5px 14px;
  border-radius: 20px;
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.75);
  text-decoration: none;
  font-size: 0.78rem;
  font-weight: 500;
  transition: background 0.2s, color 0.2s;
}

.qnav-item:hover {
  background: rgba(255,255,255,0.2);
  color: white;
}

/* ── 콘텐츠 ── */
.content-area {
  max-width: 800px;
  margin: 0 auto;
  padding: 30px 20px 60px;
}

/* ── 지표 ── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-box {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-num {
  font-size: 1.6rem;
  font-weight: 800;
}

.stat-label {
  font-size: 0.75rem;
  color: #888;
}

/* ── 섹션 공통 ── */
.section { margin-bottom: 32px; }

.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: #a8d8ea;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.description {
  line-height: 1.8;
  color: #ddd;
  font-size: 0.95rem;
}

/* ── 특징 태그 ── */
.char-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.char-tag {
  padding: 6px 14px;
  border: 1px solid;
  border-radius: 20px;
  font-size: 0.85rem;
}

/* ── 좋아하게 되는 이유 ── */
.why-love-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.why-love-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid;
  border-radius: 14px;
  padding: 16px;
}

.why-love-icon {
  display: block;
  font-size: 1.5rem;
  margin-bottom: 8px;
}

.why-love-card p {
  font-size: 0.85rem;
  line-height: 1.65;
  color: #ccc;
  margin: 0;
}

/* ── 선수 카드 ── */
.players-loading {
  color: #a8d8ea;
  font-size: 0.9rem;
}

.players-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.player-card {
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px;
  overflow: hidden;
  transition: border-color 0.2s;
}

.player-card:hover { border-color: var(--team-color); }

.player-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid;
}

.jersey-num { font-size: 1.4rem; font-weight: 800; }

.position-badge {
  font-size: 0.75rem;
  padding: 3px 10px;
  background: rgba(255,255,255,0.12);
  border-radius: 10px;
  color: #ddd;
}

.player-body { padding: 12px 14px; }

.player-name {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 6px;
}

.player-desc {
  font-size: 0.8rem;
  color: #aaa;
  line-height: 1.6;
}

/* ── 감독 ── */
.manager { font-size: 1rem; color: #ddd; }

/* ── 타임라인 ── */
.timeline {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: grid;
  grid-template-columns: 60px 24px 1fr;
  gap: 0 12px;
  align-items: flex-start;
}

.timeline-left {
  padding-top: 2px;
  text-align: right;
}

.timeline-year {
  font-size: 0.82rem;
  font-weight: 700;
}

.timeline-center {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 4px;
}

.timeline-line {
  width: 2px;
  flex: 1;
  min-height: 24px;
  background: rgba(255,255,255,0.12);
  margin: 4px 0;
}

.timeline-right {
  padding-bottom: 20px;
}

.timeline-event {
  font-size: 0.88rem;
  line-height: 1.55;
  color: #ccc;
  padding-top: 3px;
}

.timeline-event.is-highlight {
  color: white;
  font-weight: 600;
}

/* ── 응원 문화 ── */
.cheer-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cheer-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  background: rgba(255,255,255,0.04);
  border-radius: 12px;
  padding: 14px;
}

.cheer-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}

.cheer-text strong {
  display: block;
  font-size: 0.93rem;
  font-weight: 700;
  margin-bottom: 4px;
}

.cheer-text p {
  font-size: 0.83rem;
  color: #aaa;
  line-height: 1.6;
  margin: 0;
}

/* ── 라이벌 ── */
.rival-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid;
  border-radius: 16px;
  padding: 22px;
}

.rival-vs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 14px;
}

.rival-team {
  font-size: 1.1rem;
  font-weight: 800;
}

.rival-vs-label {
  font-size: 0.75rem;
  color: #555;
  font-weight: 400;
}

.rival-desc {
  font-size: 0.88rem;
  line-height: 1.7;
  color: #ccc;
  text-align: center;
  margin-bottom: 14px;
}

.rival-tags {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.rival-tag {
  padding: 4px 12px;
  border-radius: 20px;
  background: rgba(255,255,255,0.08);
  font-size: 0.75rem;
  color: #aaa;
}

/* ── 경기 추천 ── */
.watch-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.watch-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  background: rgba(255,255,255,0.04);
  border-radius: 12px;
  padding: 14px;
}

.watch-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.watch-text strong {
  display: block;
  font-size: 0.93rem;
  font-weight: 700;
  margin-bottom: 4px;
}

.watch-text p {
  font-size: 0.83rem;
  color: #aaa;
  line-height: 1.6;
  margin: 0;
}

/* ── 팬 스타일 ── */
.fan-style-card {
  background: rgba(255,255,255,0.04);
  border: 1px solid;
  border-radius: 16px;
  padding: 22px;
}

.fan-style-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.fan-style-emoji { font-size: 2rem; }

.fan-style-title {
  font-size: 1.05rem;
  font-weight: 700;
}

.fan-style-desc {
  font-size: 0.88rem;
  line-height: 1.75;
  color: #ccc;
  margin-bottom: 14px;
}

.fan-style-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.fan-tag {
  padding: 4px 12px;
  border: 1px solid;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 500;
}

/* ── 역대 기록 ── */
.records {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255,255,255,0.04);
  border-radius: 10px;
  padding: 14px 16px;
}

.record-icon { font-size: 1.5rem; }

.record-title {
  font-size: 0.8rem;
  color: #888;
  margin-bottom: 2px;
}

.record-value {
  font-size: 0.95rem;
  font-weight: 600;
}

/* ── 입문 가이드 ── */
.guide-box {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  background: rgba(255,255,255,0.04);
  border-left: 4px solid;
  border-radius: 0 12px 12px 0;
  padding: 16px 20px;
}

.guide-icon { font-size: 1.3rem; flex-shrink: 0; }

.guide-text {
  font-size: 0.92rem;
  line-height: 1.8;
  color: #ddd;
}

/* ── CTA ── */
.cta-area {
  margin-top: 40px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.cta-btn {
  flex: 1;
  min-width: 180px;
  display: inline-block;
  color: white;
  text-decoration: none;
  padding: 14px 24px;
  border-radius: 30px;
  font-size: 0.95rem;
  font-weight: 600;
  text-align: center;
  transition: opacity 0.2s, transform 0.2s;
}

.compare-btn {
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.2);
}

.cta-btn:hover {
  opacity: 0.85;
  transform: translateY(-2px);
}

@media (max-width: 600px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .team-name { font-size: 1.8rem; }
  .players-grid { grid-template-columns: 1fr; }
  .why-love-grid { grid-template-columns: 1fr; }
  .cta-area { flex-direction: column; }
}
</style>
