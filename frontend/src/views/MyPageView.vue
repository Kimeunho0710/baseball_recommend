<template>
  <div class="my-page">
    <div class="my-container">
      <div class="my-header">
        <h1 class="my-title">내 페이지</h1>
        <div v-if="authStore.user" class="user-info">
          <span class="nickname">{{ authStore.user.nickname }}</span>
          <span class="email">{{ authStore.user.email }}</span>
        </div>
      </div>

      <div class="section">
        <h2 class="section-title">내 추천 기록</h2>

        <div v-if="loading" class="state-box">불러오는 중...</div>

        <div v-else-if="fetchError" class="state-box error-box">
          <p>{{ fetchError }}</p>
        </div>

        <div v-else-if="recommendations.length === 0" class="state-box">
          <p>아직 추천 기록이 없습니다.</p>
          <RouterLink to="/survey" class="survey-btn">설문 시작하기 →</RouterLink>
        </div>

        <div v-else class="recommend-list">
          <div
            v-for="item in recommendations"
            :key="item.recommendId"
            class="recommend-card"
            :style="{ borderColor: teamColorMap[item.teamName] || '#e94560' }"
          >
            <div class="card-left">
              <span class="card-team" :style="{ color: teamColorMap[item.teamName] || '#e94560' }">
                ⚾ {{ item.teamName }}
              </span>
              <span v-if="item.fanProfile" class="card-profile">{{ item.fanProfile }}</span>
              <span class="card-date">{{ formatDate(item.createdAt) }}</span>
            </div>
            <div class="card-btns">
              <RouterLink
                :to="`/onboarding/${item.recommendId}`"
                class="card-btn primary"
                :style="{ background: teamColorMap[item.teamName] || '#e94560' }"
              >
                팬 되기
              </RouterLink>
              <RouterLink :to="`/result/${item.recommendId}`" class="card-btn secondary">
                결과 보기
              </RouterLink>
            </div>
          </div>
        </div>
      </div>

      <button class="logout-btn" @click="handleLogout">로그아웃</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { fetchMyRecommendations } from '../api/authApi'

const router = useRouter()
const authStore = useAuthStore()

const recommendations = ref([])
const loading = ref(false)
const fetchError = ref(null)

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
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  loading.value = true
  fetchError.value = null
  try {
    recommendations.value = await fetchMyRecommendations()
  } catch (e) {
    if (e.response?.status === 401) {
      authStore.logout()
      router.push('/login')
      return
    }
    fetchError.value = '추천 기록을 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
})

function handleLogout() {
  authStore.logout()
  router.push('/')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  // LocalDateTime 배열([2024,1,1,...]) 또는 ISO 문자열 모두 처리
  const d = Array.isArray(dateStr)
    ? new Date(dateStr[0], dateStr[1] - 1, dateStr[2])
    : new Date(dateStr)
  if (isNaN(d.getTime())) return ''
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.my-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  padding: 80px 20px 60px;
  color: white;
}

.my-container {
  max-width: 600px;
  margin: 0 auto;
}

.my-header {
  margin-bottom: 36px;
}

.my-title {
  font-size: 1.8rem;
  font-weight: 800;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nickname {
  font-size: 1.1rem;
  font-weight: 600;
}

.email {
  font-size: 0.85rem;
  color: #888;
}

.section {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 0.85rem;
  color: #a8d8ea;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  margin-bottom: 20px;
  font-weight: 600;
}

.state-box {
  text-align: center;
  padding: 40px 0;
  color: #888;
  font-size: 0.9rem;
}

.error-box {
  color: #ff8a8a;
  font-size: 0.88rem;
}

.survey-btn {
  display: inline-block;
  margin-top: 16px;
  background: #e94560;
  color: white;
  padding: 10px 24px;
  border-radius: 12px;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommend-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: rgba(255, 255, 255, 0.04);
  border: 1.5px solid;
  border-radius: 12px;
  padding: 14px 16px;
  flex-wrap: wrap;
}

.card-left {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.card-team {
  font-size: 0.98rem;
  font-weight: 700;
}

.card-profile {
  font-size: 0.78rem;
  color: #aaa;
}

.card-date {
  font-size: 0.75rem;
  color: #666;
}

.card-btns {
  display: flex;
  gap: 8px;
}

.card-btn {
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 0.82rem;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.card-btn.primary {
  color: white;
}

.card-btn.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #ccc;
}

.card-btn:hover {
  opacity: 0.85;
}

.logout-btn {
  width: 100%;
  padding: 13px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: transparent;
  color: #888;
  font-size: 0.92rem;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.logout-btn:hover {
  color: #ff6b6b;
  border-color: #ff6b6b;
}
</style>
