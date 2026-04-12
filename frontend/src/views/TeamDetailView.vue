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
        </div>
      </div>

      <div class="content-area">

        <!-- 핵심 지표 -->
        <div class="stats-row">
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
        <div class="section">
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

        <!-- 주요 선수 -->
        <div class="section">
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
        <div v-if="team.beginnerGuide" class="section">
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

const mascotIconMap = {
  '호랑이': '🐯', '사자': '🦁', '쌍둥이': '👯', '곰': '🐻',
  '마법사': '🧙', '랜더': '🚀', '갈매기': '🐦', '독수리': '🦅',
  '공룡': '🦕', '영웅': '⚡'
}

function mascotIcon(mascot) {
  return mascotIconMap[mascot] || '⚾'
}

const characteristics = computed(() =>
  team.value?.characteristics
    ? team.value.characteristics.split(',').map(c => c.trim())
    : []
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

.hero-banner {
  padding: 40px 20px;
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
.section { margin-bottom: 28px; }

.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: #a8d8ea;
  margin-bottom: 12px;
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

.player-card:hover {
  border-color: var(--team-color);
}

.player-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid;
}

.jersey-num {
  font-size: 1.4rem;
  font-weight: 800;
}

.position-badge {
  font-size: 0.75rem;
  padding: 3px 10px;
  background: rgba(255,255,255,0.12);
  border-radius: 10px;
  color: #ddd;
}

.player-body {
  padding: 12px 14px;
}

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
.manager {
  font-size: 1rem;
  color: #ddd;
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
  .cta-area { flex-direction: column; }
}
</style>
