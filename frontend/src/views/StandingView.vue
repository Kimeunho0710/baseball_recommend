<template>
  <div class="standing-page">
    <div class="standing-container">
      <h1 class="page-title">🏆 KBO 순위</h1>
      <p class="page-subtitle">{{ currentYear }}시즌 팀 순위</p>

      <div v-if="loading" class="loading">순위를 불러오는 중...</div>
      <div v-else-if="error" class="error">{{ error }}</div>

      <template v-else>
        <div class="table-wrap">
          <table class="standing-table">
            <thead>
              <tr>
                <th>순위</th>
                <th>팀</th>
                <th>경기</th>
                <th>승</th>
                <th>패</th>
                <th>무</th>
                <th>승률</th>
                <th>게임차</th>
                <th>연속</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="s in standings"
                :key="s.rank"
                :class="{ 'top3': s.rank <= 3, 'playoff': s.rank <= 5 }"
              >
                <td class="rank-cell">
                  <span class="rank-badge" :class="`rank-${s.rank}`">{{ s.rank }}</span>
                </td>
                <td class="team-cell">
                  <span class="team-color-dot" :style="{ background: teamColor(s.teamName) }"></span>
                  {{ s.teamName }}
                </td>
                <td>{{ s.games }}</td>
                <td class="wins">{{ s.wins }}</td>
                <td class="losses">{{ s.losses }}</td>
                <td>{{ s.draws }}</td>
                <td class="winrate">{{ s.winRate.toFixed(3) }}</td>
                <td>{{ s.gamesBehind }}</td>
                <td>{{ s.streak }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="legend">
          <span class="legend-item playoff-badge">5위 이내 포스트시즌</span>
          <p class="data-note">* 데이터는 KBO 공식 홈페이지 기준 (실시간 스크래핑)</p>
        </div>

        <div v-if="isAllZero" class="scrape-warning">
          ⚠️ 현재 실시간 순위를 불러오지 못했습니다. KBO 공식 사이트에서 직접 확인해 주세요.
          <a href="https://www.koreabaseball.com" target="_blank" rel="noopener">KBO 공식 홈페이지 →</a>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchStandings } from '../api/surveyApi'

const standings = ref([])
const loading = ref(false)
const error = ref(null)
const currentYear = new Date().getFullYear()
const isAllZero = computed(() => standings.value.length > 0 && standings.value.every(s => s.games === 0))

const teamColorMap = {
  'KIA 타이거즈': '#EA0029',
  '삼성 라이온즈': '#074CA1',
  'LG 트윈스': '#C30452',
  '두산 베어스': '#131230',
  'KT 위즈': '#555555',
  'SSG 랜더스': '#CE0E2D',
  '롯데 자이언츠': '#041E42',
  '한화 이글스': '#FF6600',
  'NC 다이노스': '#071D49',
  '키움 히어로즈': '#820024',
}

function teamColor(name) {
  return teamColorMap[name] || '#888'
}

onMounted(async () => {
  loading.value = true
  try {
    standings.value = await fetchStandings()
  } catch {
    error.value = '순위 데이터를 불러오는 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.standing-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 90px 20px 60px;
  color: white;
}

.standing-container {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 2.2rem;
  font-weight: 800;
  text-align: center;
  margin-bottom: 8px;
}

.page-subtitle {
  text-align: center;
  color: #a8d8ea;
  margin-bottom: 32px;
}

.loading, .error {
  text-align: center;
  color: #a8d8ea;
  padding: 60px;
}

.table-wrap {
  overflow-x: auto;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.1);
}

.standing-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

.standing-table thead tr {
  background: rgba(255,255,255,0.08);
}

.standing-table th {
  padding: 14px 12px;
  text-align: center;
  font-size: 0.8rem;
  color: #a8d8ea;
  font-weight: 600;
  white-space: nowrap;
}

.standing-table td {
  padding: 13px 12px;
  text-align: center;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.standing-table tbody tr:hover {
  background: rgba(255,255,255,0.04);
}

.standing-table tbody tr.playoff {
  border-left: 3px solid #e94560;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.85rem;
  background: rgba(255,255,255,0.08);
}

.rank-1 { background: #FFD700; color: #000; }
.rank-2 { background: #C0C0C0; color: #000; }
.rank-3 { background: #CD7F32; color: #fff; }

.team-cell {
  text-align: left;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  font-weight: 600;
}

.team-color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.wins { color: #5bc8f5; font-weight: 600; }
.losses { color: #f57c7c; font-weight: 600; }
.winrate { font-weight: 600; }

.legend {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.playoff-badge {
  font-size: 0.8rem;
  color: #e94560;
  padding: 4px 12px;
  border: 1px solid #e94560;
  border-radius: 12px;
}

.data-note {
  font-size: 0.75rem;
  color: #666;
}

.scrape-warning {
  margin-top: 20px;
  padding: 14px 18px;
  background: rgba(255, 165, 0, 0.1);
  border: 1px solid rgba(255, 165, 0, 0.3);
  border-radius: 10px;
  font-size: 0.9rem;
  color: #ffb347;
  text-align: center;
}

.scrape-warning a {
  color: #ffd580;
  margin-left: 8px;
}
</style>
