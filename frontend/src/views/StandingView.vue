<template>
  <div class="league-page">
    <div class="league-container">

      <h1 class="page-title">⚾ 리그 현황</h1>
      <p class="page-subtitle">{{ currentYear }}시즌 · 실시간 업데이트</p>

      <!-- ── 탭 ── -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >{{ tab.label }}</button>
      </div>

      <!-- ════════════════════════════════════════
           탭 1: 오늘의 경기
      ════════════════════════════════════════ -->
      <div v-if="activeTab === 'today'">

        <div v-if="gamesLoading" class="loading">경기 정보 불러오는 중...</div>

        <template v-else>
          <!-- 오늘 경기 있음 -->
          <div v-if="todayGames.length" class="games-grid">
            <div
              v-for="game in todayGames"
              :key="`${game.awayTeam}-${game.homeTeam}`"
              class="game-card"
              :class="{ completed: game.completed }"
            >
              <div class="game-date-row">
                <span class="game-badge" :class="game.completed ? 'badge-done' : 'badge-scheduled'">
                  {{ game.completed ? '종료' : game.gameTime || '예정' }}
                </span>
                <span class="game-stadium">{{ game.stadium || '' }}</span>
              </div>

              <div class="matchup">
                <!-- 어웨이 -->
                <div class="team-side away">
                  <span class="team-dot" :style="{ background: teamColor(game.awayTeam) }"></span>
                  <span class="team-abbr">{{ abbr(game.awayTeam) }}</span>
                </div>

                <!-- 스코어 -->
                <div class="score-center">
                  <template v-if="game.completed">
                    <span class="score" :class="{ winner: game.awayScore > game.homeScore }">{{ game.awayScore }}</span>
                    <span class="score-sep">:</span>
                    <span class="score" :class="{ winner: game.homeScore > game.awayScore }">{{ game.homeScore }}</span>
                  </template>
                  <span v-else class="vs-text">VS</span>
                </div>

                <!-- 홈 -->
                <div class="team-side home">
                  <span class="team-abbr">{{ abbr(game.homeTeam) }}</span>
                  <span class="team-dot" :style="{ background: teamColor(game.homeTeam) }"></span>
                </div>
              </div>

              <div class="team-names-row">
                <span class="team-name-small">{{ game.awayTeam }}</span>
                <span class="team-name-small">{{ game.homeTeam }}</span>
              </div>
            </div>
          </div>

          <!-- 오늘 경기 없음 -->
          <div v-else class="empty-box">
            <p>오늘 예정된 경기가 없거나 정보를 불러오지 못했습니다.</p>
            <a href="https://www.koreabaseball.com" target="_blank" rel="noopener" class="kbo-link">
              KBO 공식 홈페이지에서 확인하기 →
            </a>
          </div>
        </template>
      </div>

      <!-- ════════════════════════════════════════
           탭 2: 팀 순위
      ════════════════════════════════════════ -->
      <div v-if="activeTab === 'standing'">

        <div v-if="standingLoading" class="loading">순위를 불러오는 중...</div>
        <div v-else-if="standingError" class="error">{{ standingError }}</div>

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
                  <th>최근 폼</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="s in standings"
                  :key="s.rank"
                  :class="{ playoff: s.rank <= 5 }"
                >
                  <td>
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
                  <td class="winrate">{{ s.winRate?.toFixed(3) }}</td>
                  <td>{{ s.gamesBehind }}</td>
                  <td class="streak-cell">{{ s.streak }}</td>
                  <td class="form-cell">
                    <div class="form-dots">
                      <template v-if="teamForms[s.teamName]?.length">
                        <span
                          v-for="(r, i) in teamForms[s.teamName]"
                          :key="i"
                          class="form-dot"
                          :class="formClass(r)"
                          :title="r === 'W' ? '승' : r === 'L' ? '패' : r === 'D' ? '무' : '?'"
                        >{{ r }}</span>
                      </template>
                      <span v-else class="form-empty">-</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="legend">
            <span class="legend-item playoff-badge">5위 이내 포스트시즌</span>
            <p class="data-note">* KBO 공식 홈페이지 기준 (30분마다 갱신)</p>
          </div>

          <div v-if="isAllZero" class="scrape-warning">
            ⚠️ 실시간 순위를 불러오지 못했습니다.
            <a href="https://www.koreabaseball.com" target="_blank" rel="noopener">KBO 공식 홈페이지 →</a>
          </div>
        </template>
      </div>

      <!-- ════════════════════════════════════════
           탭 3: 최근 경기 결과
      ════════════════════════════════════════ -->
      <div v-if="activeTab === 'recent'">

        <div v-if="gamesLoading" class="loading">결과를 불러오는 중...</div>

        <template v-else>
          <div v-if="recentGames.length" class="recent-list">
            <div
              v-for="game in recentGames"
              :key="`${game.gameDate}-${game.awayTeam}-${game.homeTeam}`"
              class="recent-item"
            >
              <span class="recent-date">{{ formatDate(game.gameDate) }}</span>
              <div class="recent-matchup">
                <span class="recent-team" :style="{ color: game.awayScore > game.homeScore ? teamColor(game.awayTeam) : '#888' }">
                  {{ abbr(game.awayTeam) }}
                </span>
                <span class="recent-score">{{ game.awayScore }} : {{ game.homeScore }}</span>
                <span class="recent-team" :style="{ color: game.homeScore > game.awayScore ? teamColor(game.homeTeam) : '#888' }">
                  {{ abbr(game.homeTeam) }}
                </span>
              </div>
              <span class="recent-stadium">{{ game.stadium || '' }}</span>
            </div>
          </div>

          <div v-else class="empty-box">
            <p>최근 경기 결과를 불러오지 못했습니다.</p>
            <a href="https://www.koreabaseball.com" target="_blank" rel="noopener" class="kbo-link">
              KBO 공식 홈페이지에서 확인하기 →
            </a>
          </div>
        </template>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchStandings, fetchTodayGames, fetchRecentGames, fetchTeamForm } from '../api/surveyApi'

const currentYear = new Date().getFullYear()
const activeTab = ref('today')

const tabs = [
  { id: 'today',    label: '오늘의 경기' },
  { id: 'standing', label: '팀 순위' },
  { id: 'recent',   label: '최근 결과' },
]

// ── 데이터 ──
const standings = ref([])
const standingLoading = ref(false)
const standingError = ref(null)

const todayGames = ref([])
const recentGames = ref([])
const gamesLoading = ref(false)

const teamForms = ref({}) // { "KIA 타이거즈": ["W","L","W","W","L"] }

// ── 색상 ──
const teamColorMap = {
  'KIA 타이거즈':  '#EA0029',
  '삼성 라이온즈': '#074CA1',
  'LG 트윈스':    '#C30452',
  '두산 베어스':   '#131230',
  'KT 위즈':      '#555555',
  'SSG 랜더스':   '#CE0E2D',
  '롯데 자이언츠': '#041E42',
  '한화 이글스':   '#FF6600',
  'NC 다이노스':   '#071D49',
  '키움 히어로즈': '#820024',
}

const teamAbbrMap = {
  'KIA 타이거즈':  'KIA',
  '삼성 라이온즈': '삼성',
  'LG 트윈스':    'LG',
  '두산 베어스':   '두산',
  'KT 위즈':      'KT',
  'SSG 랜더스':   'SSG',
  '롯데 자이언츠': '롯데',
  '한화 이글스':   '한화',
  'NC 다이노스':   'NC',
  '키움 히어로즈': '키움',
}

function teamColor(name) { return teamColorMap[name] || '#888' }
function abbr(name) { return teamAbbrMap[name] || name?.slice(0, 2) || '' }

const isAllZero = computed(() =>
  standings.value.length > 0 && standings.value.every(s => s.games === 0)
)

function formClass(r) {
  if (r === 'W') return 'form-win'
  if (r === 'L') return 'form-lose'
  if (r === 'D') return 'form-draw'
  return 'form-unknown'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const [, m, d] = dateStr.split('-')
  return `${parseInt(m)}/${parseInt(d)}`
}

// ── 마운트 ──
onMounted(async () => {
  // 순위 로드
  standingLoading.value = true
  try {
    standings.value = await fetchStandings()
    // 각 팀 폼 비동기 로드
    loadAllForms()
  } catch {
    standingError.value = '순위 데이터를 불러오는 중 오류가 발생했습니다.'
  } finally {
    standingLoading.value = false
  }

  // 경기 일정/결과 로드
  gamesLoading.value = true
  try {
    const [today, recent] = await Promise.all([fetchTodayGames(), fetchRecentGames()])
    todayGames.value = today
    recentGames.value = recent
  } catch {
    // 실패해도 빈 배열 유지, UI에서 처리
  } finally {
    gamesLoading.value = false
  }
})

async function loadAllForms() {
  const TEAM_NAMES = [
    'KIA 타이거즈', '삼성 라이온즈', 'LG 트윈스', '두산 베어스', 'KT 위즈',
    'SSG 랜더스', '롯데 자이언츠', '한화 이글스', 'NC 다이노스', '키움 히어로즈'
  ]
  const results = await Promise.allSettled(TEAM_NAMES.map(name => fetchTeamForm(name)))
  results.forEach((res, i) => {
    if (res.status === 'fulfilled' && res.value?.form) {
      teamForms.value[TEAM_NAMES[i]] = res.value.form
    }
  })
}
</script>

<style scoped>
.league-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 90px 20px 60px;
  color: white;
}

.league-container {
  max-width: 960px;
  margin: 0 auto;
}

.page-title {
  font-size: 2.2rem;
  font-weight: 800;
  text-align: center;
  margin-bottom: 6px;
}

.page-subtitle {
  text-align: center;
  color: #a8d8ea;
  margin-bottom: 28px;
  font-size: 0.9rem;
}

.loading, .error {
  text-align: center;
  color: #a8d8ea;
  padding: 60px;
}

/* ── 탭 ── */
.tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 28px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 14px;
  padding: 6px;
}

.tab-btn {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #aaa;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: rgba(255,255,255,0.12);
  color: white;
}

/* ── 오늘의 경기 카드 ── */
.games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.game-card {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 14px;
  padding: 16px;
  transition: border-color 0.2s;
}

.game-card:hover { border-color: rgba(255,255,255,0.2); }
.game-card.completed { background: rgba(255,255,255,0.07); }

.game-date-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.game-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 10px;
}

.badge-done     { background: rgba(255,255,255,0.15); color: #ddd; }
.badge-scheduled { background: rgba(233,69,96,0.2); color: #e94560; }

.game-stadium {
  font-size: 0.72rem;
  color: #777;
  max-width: 120px;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.matchup {
  display: grid;
  grid-template-columns: 1fr 80px 1fr;
  align-items: center;
  margin-bottom: 8px;
}

.team-side {
  display: flex;
  align-items: center;
  gap: 6px;
}

.team-side.home { justify-content: flex-end; }

.team-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.team-abbr {
  font-size: 1.1rem;
  font-weight: 800;
}

.score-center {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.score {
  font-size: 1.6rem;
  font-weight: 800;
  color: #888;
  min-width: 28px;
  text-align: center;
}

.score.winner { color: white; }
.score-sep { font-size: 1.2rem; color: #555; }
.vs-text { font-size: 0.9rem; font-weight: 700; color: #555; }

.team-names-row {
  display: flex;
  justify-content: space-between;
  padding: 0 2px;
}

.team-name-small {
  font-size: 0.72rem;
  color: #888;
}

/* ── 빈 상태 ── */
.empty-box {
  text-align: center;
  padding: 60px 20px;
  color: #a8d8ea;
}

.empty-box p { margin-bottom: 16px; }

.kbo-link {
  color: #5bc8f5;
  text-decoration: none;
  font-size: 0.9rem;
}

.kbo-link:hover { text-decoration: underline; }

/* ── 순위표 ── */
.table-wrap {
  overflow-x: auto;
  border-radius: 16px;
  border: 1px solid rgba(255,255,255,0.1);
}

.standing-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.standing-table thead tr {
  background: rgba(255,255,255,0.08);
}

.standing-table th {
  padding: 13px 10px;
  text-align: center;
  font-size: 0.78rem;
  color: #a8d8ea;
  font-weight: 600;
  white-space: nowrap;
}

.standing-table td {
  padding: 12px 10px;
  text-align: center;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.standing-table tbody tr:hover { background: rgba(255,255,255,0.04); }
.standing-table tbody tr.playoff { border-left: 3px solid #e94560; }

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.82rem;
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
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.wins   { color: #5bc8f5; font-weight: 600; }
.losses { color: #f57c7c; font-weight: 600; }
.winrate { font-weight: 600; }
.streak-cell { font-size: 0.82rem; color: #aaa; }

/* 최근 폼 */
.form-cell { min-width: 90px; }

.form-dots {
  display: flex;
  gap: 3px;
  justify-content: center;
  align-items: center;
}

.form-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 0.6rem;
  font-weight: 700;
}

.form-win     { background: #3b82f6; color: white; }
.form-lose    { background: #ef4444; color: white; }
.form-draw    { background: #6b7280; color: white; }
.form-unknown { background: rgba(255,255,255,0.1); color: #666; }

.form-empty { font-size: 0.8rem; color: #555; }

.legend {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.playoff-badge {
  font-size: 0.78rem;
  color: #e94560;
  padding: 3px 10px;
  border: 1px solid #e94560;
  border-radius: 10px;
}

.data-note { font-size: 0.73rem; color: #555; }

.scrape-warning {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(255,165,0,0.1);
  border: 1px solid rgba(255,165,0,0.3);
  border-radius: 10px;
  font-size: 0.88rem;
  color: #ffb347;
  text-align: center;
}

.scrape-warning a { color: #ffd580; margin-left: 8px; }

/* ── 최근 결과 ── */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.recent-item {
  display: grid;
  grid-template-columns: 56px 1fr 100px;
  align-items: center;
  gap: 12px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.07);
  border-radius: 10px;
  padding: 12px 16px;
}

.recent-date {
  font-size: 0.8rem;
  color: #888;
  text-align: center;
}

.recent-matchup {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
}

.recent-team {
  font-size: 0.9rem;
  font-weight: 700;
  min-width: 30px;
  text-align: center;
}

.recent-score {
  font-size: 1rem;
  font-weight: 800;
  color: white;
  letter-spacing: 1px;
}

.recent-stadium {
  font-size: 0.72rem;
  color: #666;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 600px) {
  .games-grid { grid-template-columns: 1fr; }
  .recent-item { grid-template-columns: 44px 1fr; }
  .recent-stadium { display: none; }
}
</style>
