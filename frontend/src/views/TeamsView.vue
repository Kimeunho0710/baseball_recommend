<template>
  <div class="teams-page">
    <div class="teams-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">KBO 10개 팀</h1>
          <p class="page-subtitle">각 팀을 클릭해 자세한 정보를 확인하세요</p>
        </div>
        <RouterLink to="/compare" class="compare-link">⚖️ 팀 비교하기</RouterLink>
      </div>

      <div v-if="loading" class="loading">불러오는 중...</div>
      <div v-else-if="error" class="error">{{ error }}</div>

      <div v-else class="team-grid">
        <RouterLink
          v-for="team in teams"
          :key="team.id"
          :to="`/teams/${team.id}`"
          class="team-card"
          :style="{ '--team-color': team.primaryColor }"
        >
          <div class="card-top" :style="{ background: team.primaryColor }">
            <span class="mascot-icon">{{ mascotIcon(team.mascot) }}</span>
          </div>
          <div class="card-body">
            <h2 class="team-name">{{ team.name }}</h2>
            <p class="team-city">📍 {{ team.city }}</p>
            <div class="team-stats">
              <div class="stat">
                <span class="stat-label">창단</span>
                <span class="stat-value">{{ team.foundedYear }}년</span>
              </div>
              <div class="stat">
                <span class="stat-label">우승</span>
                <span class="stat-value">{{ team.championships }}회</span>
              </div>
            </div>
            <div class="char-tags">
              <span
                v-for="c in characteristics(team)"
                :key="c"
                class="char-tag"
              >{{ c }}</span>
            </div>
          </div>
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchTeams } from '../api/surveyApi'

const teams = ref([])
const loading = ref(false)
const error = ref(null)

const mascotIconMap = {
  '호랑이': '🐯', '사자': '🦁', '쌍둥이': '👯', '곰': '🐻',
  '마법사': '🧙', '랜더': '🚀', '갈매기': '🐦', '독수리': '🦅',
  '공룡': '🦕', '영웅': '⚡'
}

function mascotIcon(mascot) {
  return mascotIconMap[mascot] || '⚾'
}

function characteristics(team) {
  return team.characteristics ? team.characteristics.split(',').map(c => c.trim()).slice(0, 3) : []
}

onMounted(async () => {
  loading.value = true
  try {
    teams.value = await fetchTeams()
  } catch {
    error.value = '팀 정보를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.teams-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 90px 20px 60px;
  color: white;
}

.teams-container {
  max-width: 1100px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 40px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  font-size: 2.2rem;
  font-weight: 800;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #a8d8ea;
}

.compare-link {
  display: inline-block;
  padding: 10px 20px;
  border: 1px solid rgba(255,255,255,0.25);
  border-radius: 24px;
  color: white;
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 600;
  background: rgba(255,255,255,0.07);
  transition: background 0.2s;
  white-space: nowrap;
}

.compare-link:hover { background: rgba(255,255,255,0.14); }

.loading, .error {
  text-align: center;
  color: #a8d8ea;
  padding: 60px;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.team-card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px;
  overflow: hidden;
  text-decoration: none;
  color: white;
  transition: transform 0.2s, border-color 0.2s;
}

.team-card:hover {
  transform: translateY(-4px);
  border-color: var(--team-color);
}

.card-top {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mascot-icon {
  font-size: 2.5rem;
}

.card-body {
  padding: 16px;
}

.team-name {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 4px;
}

.team-city {
  font-size: 0.8rem;
  color: #aaa;
  margin-bottom: 12px;
}

.team-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: 0.7rem;
  color: #888;
}

.stat-value {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--team-color);
}

.char-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.char-tag {
  font-size: 0.7rem;
  padding: 2px 8px;
  background: rgba(255,255,255,0.08);
  border-radius: 10px;
  color: #ccc;
}
</style>
