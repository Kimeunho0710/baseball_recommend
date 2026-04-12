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
        <div class="result-header">
          <p class="label">AI 추천 결과</p>
          <h1 class="team-name" :style="{ color: teamColor }">
            ⚾ {{ result.teamName }}
          </h1>
          <div class="team-info">
            <span>📍 {{ result.teamCity }}</span>
            <span>🏟️ {{ result.teamStadium }}</span>
          </div>
        </div>

        <div class="divider" :style="{ background: teamColor }"></div>

        <div class="reason-box">
          <h2>왜 이 팀이 당신에게 맞을까요?</h2>
          <p class="reason-text">{{ result.reason }}</p>
        </div>

        <div class="description-box">
          <p>{{ result.teamDescription }}</p>
          <div class="characteristics">
            <span
              v-for="char in characteristics"
              :key="char"
              class="char-tag"
              :style="{ borderColor: teamColor }"
            >
              {{ char }}
            </span>
          </div>
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
  'KIA 타이거즈': '#EA0029',
  '삼성 라이온즈': '#074CA1',
  'LG 트윈스': '#C30452',
  '두산 베어스': '#4a4a8a',
  'KT 위즈': '#aaaaaa',
  'SSG 랜더스': '#CE0E2D',
  '롯데 자이언츠': '#4a6fa5',
  '한화 이글스': '#FF6600',
  'NC 다이노스': '#2a5ca8',
  '키움 히어로즈': '#820024',
}

const teamColor = computed(() =>
  result.value ? (teamColorMap[result.value.teamName] || '#e94560') : '#e94560'
)

const characteristics = computed(() =>
  result.value?.teamCharacteristics
    ? result.value.teamCharacteristics.split(',').map(c => c.trim())
    : []
)

onMounted(async () => {
  const id = route.params.id

  // 설문 직후 이동 시 store에 결과가 있으면 그대로 사용
  if (store.result && String(store.result.recommendId) === String(id)) {
    result.value = store.result
    return
  }

  // 새로고침 or 공유 링크 접근 시 API로 조회
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
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.result-container {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 40px;
  width: 100%;
  max-width: 580px;
  color: white;
  text-align: center;
}

.state-box {
  padding: 40px 0;
  color: #a8d8ea;
}

.label {
  font-size: 0.9rem;
  color: #a8d8ea;
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-bottom: 12px;
}

.team-name {
  font-size: 2.4rem;
  font-weight: 800;
  margin-bottom: 16px;
}

.team-info {
  display: flex;
  justify-content: center;
  gap: 20px;
  color: #ccc;
  font-size: 0.9rem;
  margin-bottom: 24px;
}

.divider {
  height: 4px;
  border-radius: 2px;
  margin: 0 auto 30px;
  width: 60px;
}

.reason-box {
  background: rgba(255, 255, 255, 0.04);
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 20px;
  text-align: left;
}

.reason-box h2 {
  font-size: 1rem;
  color: #a8d8ea;
  margin-bottom: 12px;
}

.reason-text {
  font-size: 1rem;
  line-height: 1.8;
  color: #eee;
}

.description-box {
  margin-bottom: 24px;
  color: #bbb;
  font-size: 0.9rem;
  line-height: 1.7;
}

.characteristics {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
}

.char-tag {
  padding: 4px 12px;
  border: 1px solid;
  border-radius: 20px;
  font-size: 0.8rem;
  color: white;
}

.share-btn {
  width: 100%;
  padding: 13px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.07);
  color: white;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 12px;
  transition: all 0.2s;
}

.share-btn:hover {
  background: rgba(255, 255, 255, 0.13);
}

.action-btns {
  display: flex;
  gap: 12px;
}

.btn {
  flex: 1;
  padding: 14px;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s;
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
