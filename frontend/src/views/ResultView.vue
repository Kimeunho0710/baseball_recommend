<template>
  <div class="result-page">
    <div class="result-container">

      <!-- 로딩 -->
      <div v-if="loading" class="state-box">
        <p>결과를 불러오는 중...</p>
      </div>

      <!-- 에러 -->
      <div v-else-if="error" class="state-box">
        <p>{{ error }}</p>
        <RouterLink to="/survey" class="retry-btn">설문 다시 하기</RouterLink>
      </div>

      <!-- 결과 -->
      <template v-else-if="result">

        <!-- 팬 성향 프로필 -->
        <div v-if="result.fanProfile" class="profile-section">
          <span class="profile-badge" :style="{ background: profileBadgeColor }">
            {{ profileEmoji }} {{ result.fanProfile }}
          </span>
          <p class="profile-desc">{{ result.fanProfileDescription }}</p>
        </div>

        <div class="section-divider"></div>

        <!-- 1위 팀 헤더 -->
        <div class="result-header">
          <p class="label">당신과 가장 잘 맞는 팀</p>
          <h1 class="team-name" :style="{ color: teamColor }">
            ⚾ {{ result.teamName }}
          </h1>
          <div class="team-info">
            <span>📍 {{ result.teamCity }}</span>
            <span>🏟️ {{ result.teamStadium }}</span>
          </div>
        </div>

        <div class="color-divider" :style="{ background: teamColor }"></div>

        <!-- TOP 3 적합도 분석 -->
        <div v-if="result.top3?.length" class="top3-section">
          <h2 class="section-title">팀 적합도 분석</h2>
          <div
            v-for="item in result.top3"
            :key="item.rank"
            class="team-bar-item"
            :class="{ 'is-top': item.rank === 1 }"
          >
            <div class="bar-header">
              <div class="bar-left">
                <span class="rank-badge" :style="item.rank === 1 ? { background: item.primaryColor } : {}">
                  {{ item.rank }}위
                </span>
                <span class="bar-team-name">{{ item.teamName }}</span>
              </div>
              <span class="percentage-label">{{ item.percentage }}%</span>
            </div>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{ width: item.percentage + '%', background: item.primaryColor }"
              ></div>
            </div>
            <p class="bar-reason">{{ item.shortReason }}</p>
          </div>
        </div>

        <!-- 추천 이유 -->
        <div class="reason-box">
          <h2>왜 이 팀이 당신에게 맞을까요?</h2>
          <p class="reason-text">{{ result.reason }}</p>
        </div>

        <!-- 팀 특징 태그 -->
        <div v-if="characteristics.length" class="characteristics">
          <span
            v-for="char in characteristics"
            :key="char"
            class="char-tag"
            :style="{ borderColor: teamColor, color: teamColor }"
          >
            {{ char }}
          </span>
        </div>

        <!-- 링크 복사 -->
        <button class="share-btn" @click="copyLink">
          {{ copied ? '✅ 링크 복사됨!' : '🔗 결과 링크 복사' }}
        </button>

        <div class="action-btns">
          <RouterLink to="/survey" class="btn retry">다시 해보기</RouterLink>
          <RouterLink to="/" class="btn home">홈으로</RouterLink>
        </div>

      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { useSurveyStore } from '../stores/surveyStore'
import { fetchRecommend } from '../api/surveyApi'

const route = useRoute()
const store = useSurveyStore()

const result = ref(null)
const loading = ref(false)
const error = ref(null)
const copied = ref(false)

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

const profileEmojiMap = {
  '감성 몰입형 팬': '🎭',
  '전략 분석형 팬': '📊',
  '열정 응원형 팬': '🔥',
  '전통 중시형 팬': '🏆',
  '도전 선호형 팬': '⚡',
  '차분 관조형 팬': '🎯',
}

const teamColor = computed(() =>
  result.value ? (teamColorMap[result.value.teamName] || '#e94560') : '#e94560'
)

const profileEmoji = computed(() =>
  result.value?.fanProfile ? (profileEmojiMap[result.value.fanProfile] || '⚾') : '⚾'
)

// 프로필 배지 배경색: 팀색의 20% 투명도
const profileBadgeColor = computed(() => teamColor.value + '33')

const characteristics = computed(() =>
  result.value?.teamCharacteristics
    ? result.value.teamCharacteristics.split(',').map(c => c.trim())
    : []
)

onMounted(async () => {
  const id = route.params.id

  if (store.result && String(store.result.recommendId) === String(id)) {
    result.value = store.result
    return
  }

  loading.value = true
  try {
    result.value = await fetchRecommend(id)
  } catch {
    error.value = '결과를 찾을 수 없습니다.'
  } finally {
    loading.value = false
  }
})

async function copyLink() {
  await navigator.clipboard.writeText(window.location.href)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}
</script>

<style scoped>
.result-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 20px;
}

.result-container {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 36px;
  width: 100%;
  max-width: 600px;
  color: white;
}

/* ── 상태 박스 ── */
.state-box {
  padding: 60px 0;
  color: #a8d8ea;
  text-align: center;
}

/* ── 팬 성향 프로필 ── */
.profile-section {
  text-align: center;
  margin-bottom: 20px;
}

.profile-badge {
  display: inline-block;
  padding: 8px 20px;
  border-radius: 30px;
  font-size: 1rem;
  font-weight: 700;
  color: white;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.profile-desc {
  font-size: 0.88rem;
  color: #b0c4d8;
  line-height: 1.7;
  max-width: 480px;
  margin: 0 auto;
}

/* ── 구분선 ── */
.section-divider {
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  margin: 20px 0;
}

/* ── 1위 팀 헤더 ── */
.result-header {
  text-align: center;
  margin-bottom: 16px;
}

.label {
  font-size: 0.8rem;
  color: #a8d8ea;
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.team-name {
  font-size: 2.2rem;
  font-weight: 800;
  margin-bottom: 12px;
}

.team-info {
  display: flex;
  justify-content: center;
  gap: 20px;
  color: #bbb;
  font-size: 0.88rem;
}

.color-divider {
  height: 4px;
  border-radius: 2px;
  width: 60px;
  margin: 0 auto 24px;
}

/* ── TOP 3 적합도 분석 ── */
.top3-section {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 0.85rem;
  color: #a8d8ea;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-bottom: 18px;
  font-weight: 600;
}

.team-bar-item {
  margin-bottom: 18px;
}

.team-bar-item:last-child {
  margin-bottom: 0;
}

.team-bar-item.is-top .bar-team-name {
  font-weight: 700;
  color: white;
}

.bar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rank-badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  min-width: 30px;
  text-align: center;
}

.bar-team-name {
  font-size: 0.92rem;
  color: #ddd;
}

.percentage-label {
  font-size: 0.88rem;
  font-weight: 700;
  color: #eee;
}

.bar-track {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  height: 10px;
  overflow: hidden;
  margin-bottom: 5px;
}

.bar-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.8s ease;
}

.bar-reason {
  font-size: 0.78rem;
  color: #888;
  padding-left: 2px;
}

/* ── 추천 이유 ── */
.reason-box {
  background: rgba(255, 255, 255, 0.04);
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
}

.reason-box h2 {
  font-size: 0.82rem;
  color: #a8d8ea;
  letter-spacing: 1px;
  margin-bottom: 10px;
  font-weight: 600;
}

.reason-text {
  font-size: 0.95rem;
  line-height: 1.85;
  color: #e0e0e0;
}

/* ── 팀 특징 태그 ── */
.characteristics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 20px;
}

.char-tag {
  padding: 4px 14px;
  border: 1px solid;
  border-radius: 20px;
  font-size: 0.78rem;
  font-weight: 500;
}

/* ── 공유 / 버튼 ── */
.share-btn {
  width: 100%;
  padding: 13px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.07);
  color: white;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 10px;
  transition: background 0.2s;
}

.share-btn:hover {
  background: rgba(255, 255, 255, 0.13);
}

.action-btns {
  display: flex;
  gap: 10px;
}

.btn {
  flex: 1;
  padding: 13px;
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 600;
  text-decoration: none;
  text-align: center;
  transition: opacity 0.2s, transform 0.2s;
}

.btn.retry {
  background: #e94560;
  color: white;
}

.btn.home {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.btn:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.retry-btn {
  display: inline-block;
  margin-top: 16px;
  background: #e94560;
  color: white;
  padding: 12px 30px;
  border-radius: 12px;
  text-decoration: none;
}
</style>
