<template>
  <div class="onboarding-page">
    <div v-if="loading" class="state-box">
      <p>팬 입문 가이드를 준비하는 중...</p>
    </div>

    <div v-else-if="error" class="state-box">
      <p>{{ error }}</p>
      <RouterLink to="/" class="back-link">홈으로</RouterLink>
    </div>

    <template v-else-if="result">

      <!-- ── 상단 헤더 ── -->
      <div class="hero" :style="{ background: `linear-gradient(135deg, ${teamColor}22 0%, #0f3460 100%)` }">
        <div class="hero-inner">
          <span class="onboarding-badge">팬 입문 가이드</span>
          <h1 class="hero-team" :style="{ color: teamColor }">{{ result.teamName }}</h1>
          <p class="hero-sub">지금부터 진짜 팬이 되는 여정을 시작해볼게요</p>
          <div class="hero-meta">
            <span>📍 {{ result.teamCity }}</span>
            <span>🏟️ {{ result.teamStadium }}</span>
          </div>
        </div>
        <div class="accent-bar" :style="{ background: teamColor }"></div>
      </div>

      <div class="content">

        <!-- ── 1. 왜 이 팀인가요? ── -->
        <section class="card">
          <h2 class="card-title">왜 {{ result.teamName }}가 당신에게 맞을까요?</h2>
          <p class="card-body">{{ result.reason }}</p>
        </section>

        <!-- ── 2. 꼭 알아야 할 팀 이야기 ── -->
        <section class="card">
          <h2 class="card-title">꼭 알아야 할 팀 이야기 3가지</h2>
          <div class="stories">
            <div
              v-for="(story, i) in teamStories"
              :key="i"
              class="story-item"
            >
              <div class="story-icon" :style="{ background: teamColor + '22', color: teamColor }">
                {{ story.icon }}
              </div>
              <div class="story-text">
                <strong>{{ story.title }}</strong>
                <p>{{ story.body }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- ── 3. 주목할 선수 3명 ── -->
        <section class="card">
          <h2 class="card-title">입문자가 먼저 주목할 선수 3명</h2>
          <div v-if="playersLoading" class="mini-loading">선수 정보 불러오는 중...</div>
          <div v-else class="players-grid">
            <div
              v-for="player in featuredPlayers"
              :key="player.id"
              class="player-card"
              :style="{ borderColor: teamColor + '55' }"
            >
              <div class="player-number" :style="{ color: teamColor }">#{{ player.jerseyNumber }}</div>
              <div class="player-info">
                <span class="player-name">{{ player.name }}</span>
                <span class="player-pos">{{ player.position }}</span>
              </div>
              <p class="player-desc">{{ player.description }}</p>
            </div>
          </div>
        </section>

        <!-- ── 4. 오늘의 경기 ── -->
        <section class="card">
          <h2 class="card-title">오늘 볼 수 있는 경기</h2>
          <div v-if="todayGame" class="today-game" :style="{ borderColor: teamColor }">
            <div class="game-teams">
              <span :class="{ 'is-my-team': todayGame.awayTeam === result.teamName }">
                {{ todayGame.awayTeam }}
              </span>
              <span class="vs-label">VS</span>
              <span :class="{ 'is-my-team': todayGame.homeTeam === result.teamName }">
                {{ todayGame.homeTeam }}
              </span>
            </div>
            <div class="game-meta">
              <span>{{ todayGame.gameDate }}</span>
              <span v-if="todayGame.gameTime">{{ todayGame.gameTime }}</span>
              <span v-if="todayGame.stadium">{{ todayGame.stadium }}</span>
            </div>
            <div v-if="todayGame.awayScore !== null && todayGame.homeScore !== null" class="game-score">
              {{ todayGame.awayScore }} : {{ todayGame.homeScore }}
              <span class="score-status">{{ todayGame.status }}</span>
            </div>
            <div v-else class="game-upcoming">경기 예정</div>
          </div>
          <div v-else class="no-game">
            <p>오늘은 {{ result.teamName }}의 경기가 없어요.</p>
            <RouterLink to="/standings" class="standings-link" :style="{ color: teamColor }">
              순위표 보러 가기 →
            </RouterLink>
          </div>
        </section>

        <!-- ── 5. 팬 입문 가이드 ── -->
        <section v-if="result.beginnerGuide" class="card">
          <h2 class="card-title">처음 팬이 된다면 이렇게 시작해보세요</h2>
          <div class="guide-steps">
            <div class="guide-step" v-for="(step, i) in guideSteps" :key="i">
              <div class="step-num" :style="{ background: teamColor }">{{ i + 1 }}</div>
              <p>{{ step }}</p>
            </div>
          </div>
          <div class="guide-full">
            <p>{{ result.beginnerGuide }}</p>
          </div>
        </section>

        <!-- ── 액션 버튼 ── -->
        <div class="action-area">
          <RouterLink
            :to="`/teams/${result.teamId}`"
            class="action-btn primary"
            :style="{ background: teamColor }"
          >
            팀 상세 정보 보기
          </RouterLink>
          <RouterLink to="/standings" class="action-btn secondary">
            KBO 순위 보기
          </RouterLink>
          <RouterLink :to="`/result/${result.recommendId}`" class="action-btn ghost">
            추천 결과로 돌아가기
          </RouterLink>
        </div>

      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useSurveyStore } from '../stores/surveyStore'
import { fetchRecommend, fetchTeamPlayers, fetchTodayGames } from '../api/surveyApi'

const route = useRoute()
const store = useSurveyStore()

const result = ref(null)
const loading = ref(false)
const error = ref(null)
const players = ref([])
const playersLoading = ref(false)
const todayGame = ref(null)

// ── 팀별 스토리 데이터 ──────────────────────────────────
const TEAM_STORIES = {
  'KIA 타이거즈': [
    { icon: '🏆', title: 'KBO 역대 최다 11회 우승', body: '2024년 한국시리즈 우승으로 KBO 역사상 최다 우승 기록을 세웠어요. KBO의 명실상부 1위 왕조예요.' },
    { icon: '🐯', title: '해태 타이거즈의 전설을 잇다', body: '1982년 해태 타이거즈로 출발해 40년 이상 이어온 역사. 광주 팬들에게 야구는 삶 그 자체예요.' },
    { icon: '⚡', title: '2024 MVP 김도영의 30-30', body: '2024시즌 30홈런-30도루를 달성한 역대급 시즌. 초등학교 때부터 KIA 팬이었던 김도영이 팀의 스타로 거듭났어요.' },
  ],
  '삼성 라이온즈': [
    { icon: '👑', title: '2010년대 4년 연속 우승 왕조', body: '2011~2014년 전례 없는 4연패로 KBO 최강 왕조를 구축했어요. 현존하는 KBO 최고의 연속 우승 기록이에요.' },
    { icon: '🏭', title: '탄탄한 팜 시스템', body: '원태인, 이재현 등 자체 육성 선수들이 팀의 핵심이 되는 체계적인 구단 운영이 삼성의 자랑이에요.' },
    { icon: '🦁', title: '삼성 라이온즈 파크의 품격', body: '2016년 개장한 대구 삼성 라이온즈 파크는 최신식 설비와 아름다운 전망을 자랑하는 명품 야구장이에요.' },
  ],
  'LG 트윈스': [
    { icon: '😭', title: '29년의 한을 풀다', body: '1994년 이후 무려 29년 만에 2023 한국시리즈 우승을 차지하며 팬들의 눈물을 이끌어냈어요.' },
    { icon: '⚔️', title: '잠실 더비, 영원한 라이벌', body: '같은 잠실구장을 홈으로 쓰는 두산과의 라이벌 매치는 KBO 최고의 맞대결로 꼽혀요.' },
    { icon: '❤️', title: '오지환의 LG 사랑', body: '15년 이상 LG 한 팀만을 위해 뛰며 팬들의 절대적인 지지를 받는 진정한 프랜차이즈 스타예요.' },
  ],
  '두산 베어스': [
    { icon: '📜', title: 'OB 베어스의 전설', body: '1982년 OB 베어스로 시작한 역사적 명문. 두산의 역사를 안다는 건 한국 야구의 역사를 아는 것과 같아요.' },
    { icon: '🔄', title: '역전의 명수', body: '절대 포기하지 않는 뚝심 야구. 9회 역전 드라마가 가장 많은 팀 중 하나로 끝까지 눈을 뗄 수 없어요.' },
    { icon: '🌟', title: '이승엽 감독의 귀환', body: 'KBO 레전드 홈런왕 이승엽이 감독으로 돌아와 두산의 부활을 이끌고 있어요. 역사적인 순간을 함께하세요.' },
  ],
  'KT 위즈': [
    { icon: '🚀', title: '창단 7년 만의 기적', body: '2015년 창단해 단 7년 만인 2021년에 한국시리즈 우승을 달성한 KBO 최속 성장 신화예요.' },
    { icon: '⭐', title: '막내의 반란', body: 'KBO 10개 팀 중 가장 젊은 팀이지만 강백호, 고영표 등 KBO 최고의 스타들이 포진한 강팀이에요.' },
    { icon: '🏙️', title: '수원의 새 바람', body: '수원·경기도 팬들의 뜨거운 사랑을 받으며 빠르게 뿌리를 내린 수도권 팀이에요.' },
  ],
  'SSG 랜더스': [
    { icon: '🧬', title: 'SK의 DNA를 이어받다', body: 'SK 와이번스 시절 4번의 우승 경력을 바탕으로 SSG로 새롭게 탄생해 강팀 전통을 이어가고 있어요.' },
    { icon: '💣', title: '홈런왕 최정의 KBO 신기록', body: 'KBO 역대 홈런 신기록을 보유한 최정. 그의 홈런 장면은 팬이라면 반드시 직접 봐야 할 장면이에요.' },
    { icon: '✈️', title: 'MLB에서 돌아온 김광현', body: 'MLB에서 활약 후 귀국한 김광현이 SSG 마운드를 이끌며 팬들에게 최고의 피칭을 선보이고 있어요.' },
  ],
  '롯데 자이언츠': [
    { icon: '🐦', title: '갈매기 응원단의 사직 전설', body: '갈매기 응원단과 함께하는 사직구장의 응원 문화는 KBO 최고예요. 직접 가보면 평생 잊지 못할 거예요.' },
    { icon: '🌊', title: '부산 전체가 롯데 팬', body: '부산 시민이라면 롯데 팬이 기본값이라 해도 과언이 아닐 만큼 지역 밀착형 스포츠 문화가 독보적이에요.' },
    { icon: '💔', title: '기다림이 만든 끈끈함', body: '1992년 이후 우승이 없지만 그 기다림과 열정이 롯데 팬덤을 더욱 끈끈하고 특별하게 만들어요.' },
  ],
  '한화 이글스': [
    { icon: '🌏', title: 'MLB 투수 류현진의 귀환', body: 'MLB에서 세계 수준의 투구를 선보인 류현진이 한화로 돌아왔어요. 지금이 한화 팬이 되기 딱 좋은 시기예요.' },
    { icon: '🔥', title: '대전 팬들의 독보적 열정', body: '대전 팬들의 끈끈한 응원과 팀에 대한 헌신은 KBO에서도 독보적이에요. 패배 후에도 팀을 지키는 진짜 팬십이에요.' },
    { icon: '💪', title: '재건의 아이콘 노시환', body: '한화 재건의 핵심 노시환의 폭발적인 장타력은 매 경기 팬들을 열광시키는 볼거리예요.' },
  ],
  'NC 다이노스': [
    { icon: '📊', title: '데이터 야구의 선구자', body: '2012년 창단 때부터 데이터와 분석을 중심으로 한 스마트한 야구를 추구해온 KBO의 이단아예요.' },
    { icon: '🦕', title: '창단 9년 만의 우승', body: '2020년 창단 9년 만에 한국시리즈 우승을 차지하며 KBO의 새 강자로 떠올랐어요.' },
    { icon: '🏟️', title: '창원 NC 파크의 매력', body: '2019년 개장한 창원 NC 파크는 KBO에서 가장 아름다운 구장 중 하나로 꼽혀요.' },
  ],
  '키움 히어로즈': [
    { icon: '🏛️', title: '국내 유일의 돔구장', body: '고척 스카이돔은 국내 유일의 돔구장이에요. 날씨 걱정 없이 사계절 언제든 야구를 즐길 수 있어요.' },
    { icon: '💡', title: '저비용 고효율의 도전', body: '작은 예산으로도 꾸준히 포스트시즌에 진출하는 효율적인 구단 운영이 키움만의 매력이에요.' },
    { icon: '🌱', title: '스타 배출의 산실', body: '류현진, 이정후 등 KBO 최고의 스타들을 배출한 선수 육성의 명가예요. 다음 스타를 먼저 발견해보세요.' },
  ],
}

// ── 팬 입문 가이드 3단계 (static) ──────────────────────
const GUIDE_STEPS = {
  'KIA 타이거즈': ['김도영 선수의 타석을 유심히 봐요. 그의 주루 플레이와 타격이 KIA 야구의 정수예요.', '광주 홈경기 응원가를 미리 들어봐요. 경기장에서 따라 부를 수 있으면 팬의 절반은 된 거예요.', '2024 한국시리즈 하이라이트 영상을 찾아보세요. KIA의 우승 DNA를 느낄 수 있어요.'],
  '삼성 라이온즈': ['원태인의 투구 폼을 분석해봐요. 제구력의 교과서 같은 피칭을 감상할 수 있어요.', '삼성의 수비 시프트를 주목해봐요. 데이터 기반 수비 배치가 얼마나 치밀한지 보여요.', '구자욱의 타격 메커니즘을 공부해봐요. KBO 최고의 순수 타자 중 한 명이에요.'],
  'LG 트윈스': ['오지환의 경기 전 루틴과 팬들과의 소통을 눈여겨봐요. 진정한 팬심이 무엇인지 알게 돼요.', '잠실 구장 좌석을 예매해봐요. LG 홈경기의 응원 열기를 직접 느껴봐요.', '2023 한국시리즈 우승 순간 영상을 찾아봐요. 29년 만의 기쁨이 무엇인지 느껴져요.'],
  '두산 베어스': ['두산의 9회 역전 영상을 찾아봐요. 이 팀이 왜 끝까지 봐야 하는 팀인지 알게 돼요.', '이승엽 감독의 선수 시절 홈런 영상도 찾아보세요. 레전드의 귀환을 더 실감할 수 있어요.', '잠실 더비(두산 vs LG) 경기를 꼭 봐요. KBO 최고의 라이벌전 분위기를 경험할 수 있어요.'],
  'KT 위즈': ['강백호의 스윙을 유심히 봐요. 파워와 기술이 결합된 KBO 최정상 타자의 타격이에요.', '2021년 한국시리즈 우승 하이라이트를 찾아봐요. 창단 7년 만의 기적이 얼마나 드라마틱했는지 알 수 있어요.', '수원 KT 위즈 파크 좌석을 예매해봐요. 가성비 좋은 야구 관람의 성지예요.'],
  'SSG 랜더스': ['최정의 홈런 영상을 찾아봐요. KBO 역대 최다 홈런 기록의 의미를 느껴볼 수 있어요.', '김광현의 피칭을 분석해봐요. MLB에서 검증된 세계 수준의 투구를 KBO에서 볼 수 있어요.', '인천 SSG 랜더스 필드 응원석에 앉아봐요. 인천 팬들의 열기가 얼마나 뜨거운지 느낄 수 있어요.'],
  '롯데 자이언츠': ['사직구장 외야석 응원단 좌석을 예매해봐요. KBO 최고의 응원 문화를 직접 체험해봐요.', '갈매기 응원가를 미리 외워봐요. 한 곡만 알아도 사직 외야에서 완전한 일원이 될 수 있어요.', '롯데의 인생 역전 응원 영상을 찾아봐요. 팬심이 얼마나 강한지 보면 저절로 팬이 돼요.'],
  '한화 이글스': ['류현진의 경기 예고가 뜨면 꼭 챙겨봐요. KBO에서 MLB 수준의 투구를 볼 수 있는 기회예요.', '노시환의 타격 영상을 봐요. 터질 듯한 장타 에너지가 한화 팬이 되고 싶게 만들어요.', '대전 이글스 파크 응원 문화를 유튜브에서 미리 경험해봐요. 입장 전부터 설레게 해줘요.'],
  'NC 다이노스': ['NC 경기의 수비 시프트와 작전 플레이를 주목해봐요. 데이터 야구의 정수를 볼 수 있어요.', '손아섭의 타격을 봐요. 정교한 배트 컨트롤의 교과서예요.', '창원 NC 파크 구장을 사진으로 먼저 찾아봐요. 직접 가고 싶어질 정도로 아름다운 구장이에요.'],
  '키움 히어로즈': ['고척 스카이돔 관람을 예약해봐요. 실내 돔의 음향 효과와 분위기는 처음엔 신세계예요.', '안우진의 투구를 봐요. KBO 최고 수준의 강속구를 직접 느낄 수 있어요.', '키움의 젊은 선수들 이름을 외워봐요. 다음 스타를 먼저 발견하는 즐거움이 키움 팬의 특권이에요.'],
}

const teamColorMap = {
  'KIA 타이거즈':  '#EA0029',
  '삼성 라이온즈': '#074CA1',
  'LG 트윈스':    '#C30452',
  '두산 베어스':   '#4a4a8a',
  'KT 위즈':      '#aaaaaa',
  'SSG 랜더스':   '#CE0E2D',
  '롯데 자이언츠': '#4a6fa5',
  '한화 이글스':   '#FF6600',
  'NC 다이노스':   '#2a5ca8',
  '키움 히어로즈': '#820024',
}

const teamColor = computed(() =>
  result.value ? (teamColorMap[result.value.teamName] || '#e94560') : '#e94560'
)

const teamStories = computed(() =>
  result.value ? (TEAM_STORIES[result.value.teamName] || []) : []
)

const guideSteps = computed(() =>
  result.value ? (GUIDE_STEPS[result.value.teamName] || []) : []
)

const featuredPlayers = computed(() => players.value.slice(0, 3))

onMounted(async () => {
  const id = route.params.id

  // 결과 데이터 로드
  if (store.result && String(store.result.recommendId) === String(id)) {
    result.value = store.result
  } else {
    loading.value = true
    try {
      result.value = await fetchRecommend(id)
    } catch {
      error.value = '결과를 찾을 수 없습니다.'
      loading.value = false
      return
    } finally {
      loading.value = false
    }
  }

  // 선수 + 오늘 경기 병렬 로드
  playersLoading.value = true
  const [, games] = await Promise.allSettled([
    fetchTeamPlayers(result.value.teamId).then(data => { players.value = data }),
    fetchTodayGames(),
  ])

  playersLoading.value = false

  if (games.status === 'fulfilled') {
    const all = games.value
    todayGame.value = all.find(g =>
      g.awayTeam === result.value.teamName || g.homeTeam === result.value.teamName
    ) || null
  }
})
</script>

<style scoped>
.onboarding-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: white;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  color: #a8d8ea;
  gap: 16px;
}

.back-link {
  color: #e94560;
  text-decoration: none;
  font-weight: 600;
}

/* ── 히어로 ── */
.hero {
  padding: 60px 20px 0;
  text-align: center;
}

.hero-inner {
  max-width: 600px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.onboarding-badge {
  display: inline-block;
  padding: 5px 16px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 0.75rem;
  letter-spacing: 2px;
  color: #a8d8ea;
  margin-bottom: 16px;
  text-transform: uppercase;
}

.hero-team {
  font-size: 2.6rem;
  font-weight: 800;
  margin-bottom: 10px;
}

.hero-sub {
  font-size: 1rem;
  color: #b0c4d8;
  margin-bottom: 16px;
  line-height: 1.6;
}

.hero-meta {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 0.85rem;
  color: #aaa;
}

.accent-bar {
  height: 5px;
  border-radius: 5px 5px 0 0;
  max-width: 600px;
  margin: 0 auto;
}

/* ── 콘텐츠 영역 ── */
.content {
  max-width: 640px;
  margin: 0 auto;
  padding: 28px 20px 60px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── 카드 공통 ── */
.card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 18px;
  padding: 24px;
}

.card-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: #a8d8ea;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-bottom: 16px;
}

.card-body {
  font-size: 0.95rem;
  line-height: 1.85;
  color: #ddd;
}

/* ── 팀 스토리 ── */
.stories {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.story-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.story-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}

.story-text strong {
  display: block;
  font-size: 0.95rem;
  font-weight: 700;
  color: #eee;
  margin-bottom: 4px;
}

.story-text p {
  font-size: 0.85rem;
  color: #aaa;
  line-height: 1.65;
  margin: 0;
}

/* ── 선수 그리드 ── */
.mini-loading {
  color: #888;
  font-size: 0.85rem;
}

.players-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.player-card {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid;
  border-radius: 12px;
  padding: 14px 16px;
}

.player-number {
  font-size: 0.8rem;
  font-weight: 700;
  margin-bottom: 4px;
}

.player-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.player-name {
  font-size: 1rem;
  font-weight: 700;
  color: #eee;
}

.player-pos {
  font-size: 0.75rem;
  color: #888;
  background: rgba(255, 255, 255, 0.08);
  padding: 2px 8px;
  border-radius: 8px;
}

.player-desc {
  font-size: 0.82rem;
  color: #aaa;
  line-height: 1.6;
  margin: 0;
}

/* ── 오늘의 경기 ── */
.today-game {
  border: 1px solid;
  border-radius: 14px;
  padding: 20px;
  text-align: center;
}

.game-teams {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  font-size: 1.05rem;
  font-weight: 600;
  color: #ccc;
  margin-bottom: 10px;
}

.game-teams .is-my-team {
  color: white;
  font-weight: 800;
  font-size: 1.15rem;
}

.vs-label {
  font-size: 0.75rem;
  color: #666;
  font-weight: 400;
}

.game-meta {
  display: flex;
  justify-content: center;
  gap: 12px;
  font-size: 0.8rem;
  color: #888;
  margin-bottom: 10px;
}

.game-score {
  font-size: 1.6rem;
  font-weight: 800;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.score-status {
  font-size: 0.75rem;
  font-weight: 400;
  color: #888;
}

.game-upcoming {
  font-size: 0.85rem;
  color: #888;
}

.no-game {
  text-align: center;
  color: #888;
  font-size: 0.9rem;
}

.no-game p {
  margin-bottom: 10px;
}

.standings-link {
  font-size: 0.88rem;
  font-weight: 600;
  text-decoration: none;
}

/* ── 입문 가이드 ── */
.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.guide-step {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.step-num {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
  margin-top: 1px;
}

.guide-step p {
  font-size: 0.88rem;
  line-height: 1.65;
  color: #ccc;
  margin: 0;
}

.guide-full {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  padding: 14px;
  margin-top: 4px;
}

.guide-full p {
  font-size: 0.85rem;
  line-height: 1.8;
  color: #aaa;
  margin: 0;
}

/* ── 액션 버튼 ── */
.action-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-btn {
  display: block;
  width: 100%;
  padding: 15px;
  border-radius: 14px;
  font-size: 0.97rem;
  font-weight: 700;
  text-align: center;
  text-decoration: none;
  transition: opacity 0.2s, transform 0.2s;
}

.action-btn:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.action-btn.primary {
  color: white;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.action-btn.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.action-btn.ghost {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #aaa;
  font-size: 0.85rem;
}
</style>
