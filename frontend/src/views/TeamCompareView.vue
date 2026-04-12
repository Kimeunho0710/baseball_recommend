<template>
  <div class="compare-page">
    <div class="compare-container">
      <RouterLink to="/teams" class="back-link">← 팀 목록</RouterLink>
      <h1 class="page-title">팀 비교</h1>
      <p class="page-subtitle">두 팀을 선택해서 나란히 비교해보세요</p>

      <!-- 팀 선택 -->
      <div class="selectors">
        <div class="selector-wrap">
          <label>첫 번째 팀</label>
          <select v-model="leftId" class="team-select">
            <option value="">팀 선택</option>
            <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
        </div>
        <div class="vs-badge">VS</div>
        <div class="selector-wrap">
          <label>두 번째 팀</label>
          <select v-model="rightId" class="team-select">
            <option value="">팀 선택</option>
            <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
        </div>
      </div>

      <!-- 비교 결과 -->
      <div v-if="leftTeam && rightTeam" class="compare-table">

        <!-- 팀 헤더 -->
        <div class="compare-header">
          <div class="team-header" :style="{ borderColor: leftTeam.primaryColor }">
            <span class="header-mascot">{{ mascotIcon(leftTeam.mascot) }}</span>
            <span class="header-name" :style="{ color: leftTeam.primaryColor }">{{ leftTeam.name }}</span>
            <span class="header-city">📍 {{ leftTeam.city }}</span>
          </div>
          <div class="header-mid">항목</div>
          <div class="team-header" :style="{ borderColor: rightTeam.primaryColor }">
            <span class="header-mascot">{{ mascotIcon(rightTeam.mascot) }}</span>
            <span class="header-name" :style="{ color: rightTeam.primaryColor }">{{ rightTeam.name }}</span>
            <span class="header-city">📍 {{ rightTeam.city }}</span>
          </div>
        </div>

        <!-- 비교 행 -->
        <div v-for="row in compareRows" :key="row.label" class="compare-row">
          <div class="cell left" :class="{ highlight: row.winner === 'left' }">
            <span :style="row.winner === 'left' ? { color: leftTeam.primaryColor, fontWeight: 700 } : {}">
              {{ row.left }}
            </span>
            <span v-if="row.winner === 'left'" class="win-badge" :style="{ background: leftTeam.primaryColor }">↑</span>
          </div>
          <div class="cell mid">{{ row.label }}</div>
          <div class="cell right" :class="{ highlight: row.winner === 'right' }">
            <span v-if="row.winner === 'right'" class="win-badge" :style="{ background: rightTeam.primaryColor }">↑</span>
            <span :style="row.winner === 'right' ? { color: rightTeam.primaryColor, fontWeight: 700 } : {}">
              {{ row.right }}
            </span>
          </div>
        </div>

        <!-- 팀 특징 비교 -->
        <div class="char-compare">
          <div class="char-side">
            <span
              v-for="c in leftChars"
              :key="c"
              class="char-tag"
              :style="{ borderColor: leftTeam.primaryColor, color: leftTeam.primaryColor }"
            >{{ c }}</span>
          </div>
          <div class="char-mid">특징</div>
          <div class="char-side right">
            <span
              v-for="c in rightChars"
              :key="c"
              class="char-tag"
              :style="{ borderColor: rightTeam.primaryColor, color: rightTeam.primaryColor }"
            >{{ c }}</span>
          </div>
        </div>

        <!-- 입문 가이드 비교 -->
        <div v-if="leftTeam.beginnerGuide || rightTeam.beginnerGuide" class="guide-compare">
          <div class="guide-item" :style="{ borderLeftColor: leftTeam.primaryColor }">
            <p class="guide-team-name">{{ leftTeam.name }}</p>
            <p class="guide-text">{{ leftTeam.beginnerGuide }}</p>
          </div>
          <div class="guide-item" :style="{ borderLeftColor: rightTeam.primaryColor }">
            <p class="guide-team-name">{{ rightTeam.name }}</p>
            <p class="guide-text">{{ rightTeam.beginnerGuide }}</p>
          </div>
        </div>

        <!-- 설문 CTA -->
        <div class="cta-wrap">
          <p class="cta-hint">어느 팀이 나와 잘 맞을지 아직 모르겠다면?</p>
          <RouterLink to="/survey" class="cta-btn">성향 설문으로 내 팀 찾기 →</RouterLink>
        </div>
      </div>

      <!-- 미선택 상태 -->
      <div v-else class="empty-state">
        <p>위에서 두 팀을 선택하면 비교 결과가 나타납니다</p>
        <div class="team-shortcut">
          <RouterLink
            v-for="t in teams"
            :key="t.id"
            :to="`/teams/${t.id}`"
            class="shortcut-chip"
            :style="{ '--c': t.primaryColor }"
          >{{ t.name }}</RouterLink>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchTeams } from '../api/surveyApi'

const teams = ref([])
const leftId = ref('')
const rightId = ref('')

const leftTeam = computed(() => teams.value.find(t => t.id == leftId.value) || null)
const rightTeam = computed(() => teams.value.find(t => t.id == rightId.value) || null)

const mascotIconMap = {
  '호랑이': '🐯', '사자': '🦁', '쌍둥이': '👯', '곰': '🐻',
  '마법사': '🧙', '랜더': '🚀', '갈매기': '🐦', '독수리': '🦅',
  '공룡': '🦕', '영웅': '⚡'
}

function mascotIcon(m) { return mascotIconMap[m] || '⚾' }

const leftChars = computed(() =>
  leftTeam.value?.characteristics ? leftTeam.value.characteristics.split(',').map(c => c.trim()) : []
)
const rightChars = computed(() =>
  rightTeam.value?.characteristics ? rightTeam.value.characteristics.split(',').map(c => c.trim()) : []
)

const compareRows = computed(() => {
  if (!leftTeam.value || !rightTeam.value) return []
  const L = leftTeam.value
  const R = rightTeam.value
  return [
    {
      label: '창단연도',
      left: `${L.foundedYear}년`,
      right: `${R.foundedYear}년`,
      winner: L.foundedYear < R.foundedYear ? 'left' : R.foundedYear < L.foundedYear ? 'right' : null,
    },
    {
      label: '구단 역사',
      left: `${2026 - L.foundedYear}년`,
      right: `${2026 - R.foundedYear}년`,
      winner: L.foundedYear < R.foundedYear ? 'left' : R.foundedYear < L.foundedYear ? 'right' : null,
    },
    {
      label: '우승 횟수',
      left: `${L.championships}회`,
      right: `${R.championships}회`,
      winner: L.championships > R.championships ? 'left' : R.championships > L.championships ? 'right' : null,
    },
    {
      label: '마스코트',
      left: `${mascotIcon(L.mascot)} ${L.mascot}`,
      right: `${mascotIcon(R.mascot)} ${R.mascot}`,
      winner: null,
    },
    {
      label: '홈구장',
      left: L.stadium,
      right: R.stadium,
      winner: null,
    },
    {
      label: '감독',
      left: L.manager,
      right: R.manager,
      winner: null,
    },
  ]
})

onMounted(async () => {
  teams.value = await fetchTeams()
})
</script>

<style scoped>
.compare-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 80px 20px 60px;
  color: white;
}

.compare-container {
  max-width: 900px;
  margin: 0 auto;
}

.back-link {
  color: #a8d8ea;
  text-decoration: none;
  font-size: 0.9rem;
  display: inline-block;
  margin-bottom: 20px;
}
.back-link:hover { color: white; }

.page-title {
  font-size: 2rem;
  font-weight: 800;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #a8d8ea;
  margin-bottom: 32px;
  font-size: 0.95rem;
}

/* ── 선택기 ── */
.selectors {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.selector-wrap {
  flex: 1;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.selector-wrap label {
  font-size: 0.82rem;
  color: #a8d8ea;
  font-weight: 600;
}

.team-select {
  background: rgba(255,255,255,0.07);
  border: 1px solid rgba(255,255,255,0.2);
  color: white;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 0.95rem;
  cursor: pointer;
  appearance: none;
}

.team-select option { background: #1a1a2e; }

.vs-badge {
  font-size: 1.2rem;
  font-weight: 900;
  color: #e94560;
  padding-top: 24px;
}

/* ── 비교 테이블 ── */
.compare-table {
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px;
  overflow: hidden;
}

/* 헤더 */
.compare-header {
  display: grid;
  grid-template-columns: 1fr 100px 1fr;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.team-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 16px;
  gap: 6px;
  border-top: 4px solid;
}

.header-mascot { font-size: 2rem; }
.header-name { font-size: 1rem; font-weight: 700; text-align: center; }
.header-city { font-size: 0.78rem; color: #999; }
.header-mid {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  color: #666;
  font-weight: 600;
}

/* 비교 행 */
.compare-row {
  display: grid;
  grid-template-columns: 1fr 100px 1fr;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.compare-row:last-of-type { border-bottom: none; }

.cell {
  padding: 14px 20px;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

.cell.left { justify-content: flex-end; text-align: right; }
.cell.right { justify-content: flex-start; text-align: left; }
.cell.mid {
  justify-content: center;
  text-align: center;
  font-size: 0.78rem;
  color: #888;
  font-weight: 600;
  background: rgba(255,255,255,0.03);
}

.cell.highlight { background: rgba(255,255,255,0.04); }

.win-badge {
  display: inline-block;
  padding: 2px 7px;
  border-radius: 8px;
  font-size: 0.72rem;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

/* 특징 비교 */
.char-compare {
  display: grid;
  grid-template-columns: 1fr 100px 1fr;
  border-top: 1px solid rgba(255,255,255,0.08);
  padding: 16px 0;
}

.char-side {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 20px;
  justify-content: flex-end;
}

.char-side.right { justify-content: flex-start; }

.char-mid {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 2px;
  font-size: 0.78rem;
  color: #888;
  font-weight: 600;
}

.char-tag {
  font-size: 0.75rem;
  padding: 4px 10px;
  border: 1px solid;
  border-radius: 14px;
}

/* 입문 가이드 비교 */
.guide-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 20px;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.guide-item {
  border-left: 4px solid;
  padding: 12px 16px;
  background: rgba(255,255,255,0.03);
  border-radius: 0 8px 8px 0;
}

.guide-team-name {
  font-size: 0.82rem;
  font-weight: 700;
  color: #a8d8ea;
  margin-bottom: 8px;
}

.guide-text {
  font-size: 0.82rem;
  color: #bbb;
  line-height: 1.7;
}

/* CTA */
.cta-wrap {
  padding: 24px 20px;
  text-align: center;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.cta-hint {
  font-size: 0.88rem;
  color: #a8d8ea;
  margin-bottom: 14px;
}

.cta-btn {
  display: inline-block;
  background: #e94560;
  color: white;
  text-decoration: none;
  padding: 13px 32px;
  border-radius: 30px;
  font-size: 0.95rem;
  font-weight: 600;
  transition: opacity 0.2s, transform 0.2s;
}

.cta-btn:hover {
  opacity: 0.85;
  transform: translateY(-2px);
}

/* 미선택 상태 */
.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #a8d8ea;
}

.empty-state p { margin-bottom: 24px; font-size: 0.95rem; }

.team-shortcut {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.shortcut-chip {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.82rem;
  text-decoration: none;
  color: white;
  background: rgba(255,255,255,0.07);
  border: 1px solid rgba(255,255,255,0.15);
  transition: border-color 0.2s, background 0.2s;
}

.shortcut-chip:hover {
  border-color: var(--c);
  background: color-mix(in srgb, var(--c) 20%, transparent);
}

@media (max-width: 600px) {
  .compare-header,
  .compare-row,
  .char-compare { grid-template-columns: 1fr 60px 1fr; }
  .header-mid, .char-mid, .cell.mid { font-size: 0.7rem; }
  .guide-compare { grid-template-columns: 1fr; }
}
</style>
