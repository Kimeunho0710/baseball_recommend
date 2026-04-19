<template>
  <div class="survey-page">
    <div class="survey-container">
      <header class="survey-header">
        <RouterLink to="/" class="back-link">← 홈으로</RouterLink>
        <h1>⚾ 성향 설문</h1>
        <p>{{ currentIndex + 1 }} / {{ questions.length }}</p>
      </header>

      <div v-if="store.loading" class="loading">
        <p>{{ isSubmitting ? 'AI가 팀을 분석 중입니다...' : '질문을 불러오는 중...' }}</p>
      </div>

      <div v-else-if="store.error" class="error-box">
        <p>{{ store.error }}</p>
        <button @click="store.loadQuestions()">다시 시도</button>
      </div>

      <template v-else-if="questions.length">
        <!-- 진행 바 -->
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>

        <!-- 질문 카드 -->
        <transition name="slide" mode="out-in">
          <div class="question-card" :key="currentIndex">
            <p class="question-text">{{ currentQuestion.question }}</p>
            <div v-if="currentQuestion.hint" class="hint-box">
              💡 {{ currentQuestion.hint }}
            </div>
            <div class="options">
              <button
                v-for="option in currentQuestion.options"
                :key="option.value"
                class="option-btn"
                :class="{ selected: selectedAnswer === option.value }"
                @click="selectAnswer(option.value)"
              >
                <span class="option-label">{{ option.value }}</span>
                <span class="option-text">{{ option.text }}</span>
              </button>
            </div>
          </div>
        </transition>

        <!-- 네비게이션 -->
        <div class="nav-btns">
          <button v-if="currentIndex > 0" class="nav-btn prev" @click="prevQuestion">
            ← 이전
          </button>
          <button
            v-if="currentIndex < questions.length - 1"
            class="nav-btn next"
            :disabled="!selectedAnswer"
            @click="nextQuestion"
          >
            다음 →
          </button>
          <button
            v-else
            class="nav-btn submit"
            :disabled="!selectedAnswer"
            @click="handleSubmit"
          >
            결과 보기 🎯
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useSurveyStore } from '../stores/surveyStore'

const store = useSurveyStore()
const router = useRouter()
const currentIndex = ref(0)
const isSubmitting = ref(false)

const questions = computed(() => store.questions)
const currentQuestion = computed(() => questions.value[currentIndex.value])
const selectedAnswer = computed(() => store.answers[currentQuestion.value?.id])
const progressPercent = computed(() =>
  ((currentIndex.value + 1) / questions.value.length) * 100
)

onMounted(() => {
  store.reset()
  store.loadQuestions()
})

function selectAnswer(value) {
  store.setAnswer(currentQuestion.value.id, value)
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
  }
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

async function handleSubmit() {
  isSubmitting.value = true
  try {
    const result = await store.submit()
    router.push(`/result/${result.recommendId}`)
  } catch {
    // error는 store에서 관리
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.survey-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.survey-container {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 40px;
  width: 100%;
  max-width: 580px;
  color: white;
}

.survey-header {
  text-align: center;
  margin-bottom: 30px;
}

.survey-header h1 {
  font-size: 1.8rem;
  margin: 10px 0 4px;
}

.survey-header p {
  color: #a8d8ea;
}

.back-link {
  color: #a8d8ea;
  text-decoration: none;
  font-size: 0.9rem;
}

.progress-bar {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  height: 6px;
  margin-bottom: 30px;
  overflow: hidden;
}

.progress-fill {
  background: #e94560;
  height: 100%;
  border-radius: 10px;
  transition: width 0.4s ease;
}

.question-card {
  margin-bottom: 30px;
}

.question-text {
  font-size: 1.2rem;
  font-weight: 600;
  line-height: 1.6;
  margin-bottom: 24px;
  text-align: center;
}

.hint-box {
  background: rgba(168, 216, 234, 0.08);
  border: 1px solid rgba(168, 216, 234, 0.25);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.83rem;
  color: #a8d8ea;
  margin-bottom: 18px;
  line-height: 1.5;
}

.options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.option-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 2px solid rgba(255, 255, 255, 0.15);
  color: white;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.option-label {
  font-size: 1rem;
  font-weight: 700;
  color: #e94560;
  line-height: 1;
}

.option-text {
  font-size: 0.88rem;
  line-height: 1.4;
}

.option-btn:hover {
  border-color: #e94560;
  background: rgba(233, 69, 96, 0.1);
}

.option-btn.selected {
  border-color: #e94560;
  background: rgba(233, 69, 96, 0.2);
}

.option-btn.selected .option-label {
  color: white;
}

@media (max-width: 480px) {
  .options {
    grid-template-columns: 1fr;
  }
}

.nav-btns {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.nav-btn {
  flex: 1;
  padding: 14px;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.nav-btn.prev {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-btn.next, .nav-btn.submit {
  background: #e94560;
  color: white;
}

.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.nav-btn:not(:disabled):hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.loading, .error-box {
  text-align: center;
  padding: 40px 0;
  color: #a8d8ea;
}

.error-box button {
  margin-top: 12px;
  background: #e94560;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  cursor: pointer;
}

.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}
.slide-enter-from { opacity: 0; transform: translateX(30px); }
.slide-leave-to { opacity: 0; transform: translateX(-30px); }
</style>
