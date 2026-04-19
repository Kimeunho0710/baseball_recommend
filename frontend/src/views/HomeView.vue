<template>
  <div class="home">
    <div class="hero">
      <div class="hero-content">
        <h1 class="title">⚾ KBO 팀 추천</h1>
        <p class="subtitle">나와 딱 맞는 KBO 야구팀을 찾아보세요!</p>
        <p class="description">
          간단한 성향 설문을 통해 AI가 KBO 10개 팀 중<br />
          당신에게 가장 잘 맞는 팀을 추천해 드립니다.
        </p>
        <RouterLink to="/survey" class="start-btn">
          설문 시작하기 →
        </RouterLink>

        <!-- 이전 추천 이력 -->
        <div v-if="lastRecommend" class="last-recommend">
          <p class="last-label">지난번 추천 결과</p>
          <div class="last-card" :style="{ borderColor: teamColorMap[lastRecommend.teamName] }">
            <div class="last-card-left">
              <span class="last-team-name" :style="{ color: teamColorMap[lastRecommend.teamName] }">
                ⚾ {{ lastRecommend.teamName }}
              </span>
              <span v-if="lastRecommend.fanProfile" class="last-profile">{{ lastRecommend.fanProfile }}</span>
            </div>
            <div class="last-card-btns">
              <RouterLink
                :to="`/onboarding/${lastRecommend.recommendId}`"
                class="last-btn primary"
                :style="{ background: teamColorMap[lastRecommend.teamName] }"
              >
                팬 되기 시작하기
              </RouterLink>
              <RouterLink
                :to="`/result/${lastRecommend.recommendId}`"
                class="last-btn secondary"
              >
                결과 보기
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 인기 팀 통계 -->
    <div class="popular-section">
      <h2>사람들이 가장 많이 추천받은 팀</h2>
      <p class="popular-subtitle">지금까지 설문을 완료한 모든 사용자 기준</p>
      <div v-if="popularTeams.length" class="popular-list">
        <div
          v-for="(team, index) in popularTeams"
          :key="team.teamName"
          class="popular-item"
        >
          <div class="popular-rank" :style="{ color: team.primaryColor }">{{ index + 1 }}</div>
          <div class="popular-info">
            <div class="popular-name">{{ team.teamName }}</div>
            <div class="popular-bar-wrap">
              <div
                class="popular-bar"
                :style="{ width: team.percentage + '%', background: team.primaryColor }"
              ></div>
            </div>
          </div>
          <div class="popular-pct" :style="{ color: team.primaryColor }">{{ team.percentage }}%</div>
        </div>
      </div>
      <div v-else class="popular-empty">아직 추천 데이터가 없습니다.</div>
    </div>

    <div class="teams-preview">
      <h2>KBO 10개 팀</h2>
      <div class="team-list">
        <div
          v-for="team in teams"
          :key="team.name"
          class="team-chip"
          :style="{ borderColor: team.color }"
        >
          {{ team.name }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchPopularTeams } from '@/api/surveyApi'

const lastRecommend = ref(null)
const popularTeams = ref([])

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

onMounted(async () => {
  const saved = localStorage.getItem('lastRecommend')
  if (saved) {
    try {
      lastRecommend.value = JSON.parse(saved)
    } catch {
      localStorage.removeItem('lastRecommend')
    }
  }

  try {
    popularTeams.value = await fetchPopularTeams()
  } catch {
    // 통계 실패는 무시
  }
})

const teams = [
  { name: 'KIA 타이거즈', color: '#EA0029' },
  { name: '삼성 라이온즈', color: '#074CA1' },
  { name: 'LG 트윈스', color: '#C30452' },
  { name: '두산 베어스', color: '#131230' },
  { name: 'KT 위즈', color: '#000000' },
  { name: 'SSG 랜더스', color: '#CE0E2D' },
  { name: '롯데 자이언츠', color: '#041E42' },
  { name: '한화 이글스', color: '#FF6600' },
  { name: 'NC 다이노스', color: '#071D49' },
  { name: '키움 히어로즈', color: '#820024' },
]
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: white;
  padding-top: 60px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.title {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 16px;
}

.subtitle {
  font-size: 1.4rem;
  color: #a8d8ea;
  margin-bottom: 20px;
}

.description {
  font-size: 1rem;
  color: #ccc;
  line-height: 1.8;
  margin-bottom: 40px;
}

.start-btn {
  display: inline-block;
  background: #e94560;
  color: white;
  padding: 16px 40px;
  border-radius: 50px;
  font-size: 1.1rem;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.2s, background 0.2s;
}

.start-btn:hover {
  background: #c73652;
  transform: translateY(-2px);
}

.last-recommend {
  margin-top: 32px;
}

.last-label {
  font-size: 0.75rem;
  color: #a8d8ea;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.last-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: rgba(255, 255, 255, 0.06);
  border: 1.5px solid;
  border-radius: 14px;
  padding: 14px 20px;
  flex-wrap: wrap;
}

.last-card-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.last-team-name {
  font-size: 1.05rem;
  font-weight: 700;
}

.last-profile {
  font-size: 0.78rem;
  color: #aaa;
}

.last-card-btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.last-btn {
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 600;
  text-decoration: none;
  transition: opacity 0.2s, transform 0.2s;
  white-space: nowrap;
}

.last-btn.primary {
  color: white;
}

.last-btn.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #ccc;
}

.last-btn:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.popular-section {
  padding: 40px 20px;
  max-width: 600px;
  margin: 0 auto;
  text-align: center;
}

.popular-section h2 {
  font-size: 1.4rem;
  margin-bottom: 8px;
  color: #a8d8ea;
}

.popular-subtitle {
  font-size: 0.8rem;
  color: #777;
  margin-bottom: 28px;
}

.popular-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.popular-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 12px 16px;
}

.popular-rank {
  font-size: 1.3rem;
  font-weight: 800;
  width: 28px;
  text-align: center;
  flex-shrink: 0;
}

.popular-info {
  flex: 1;
  text-align: left;
}

.popular-name {
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 6px;
}

.popular-bar-wrap {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.popular-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.popular-pct {
  font-size: 0.85rem;
  font-weight: 700;
  width: 38px;
  text-align: right;
  flex-shrink: 0;
}

.popular-empty {
  color: #666;
  font-size: 0.9rem;
  padding: 20px;
}

.teams-preview {
  padding: 40px 20px 60px;
  text-align: center;
}

.teams-preview h2 {
  font-size: 1.5rem;
  margin-bottom: 24px;
  color: #a8d8ea;
}

.team-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  max-width: 700px;
  margin: 0 auto;
}

.team-chip {
  padding: 8px 18px;
  border: 2px solid;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 500;
}
</style>
