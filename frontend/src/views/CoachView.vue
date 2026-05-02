<template>
  <div class="coach-page" :style="bgStyle">

    <!-- 헤더 -->
    <header class="coach-header">
      <RouterLink :to="backRoute" class="back-btn">← 돌아가기</RouterLink>
      <div class="header-info">
        <span class="header-tag" :style="{ background: teamColor }">AI 입문 코치</span>
        <span class="header-team">{{ teamName }}</span>
      </div>
    </header>

    <!-- 메시지 영역 -->
    <main class="chat-area" ref="chatArea">

      <!-- 인트로 (첫 메시지 없을 때) -->
      <div v-if="messages.length === 0" class="intro-section">
        <div class="intro-avatar" :style="{ borderColor: teamColor }">🤖</div>
        <p class="intro-text">
          안녕하세요! 저는 <strong :style="{ color: teamColor }">{{ teamName }}</strong> 입문자를 위한 AI 코치예요.<br>
          야구 규칙, 팀 정보, 경기 보는 법 등 뭐든 물어보세요!
        </p>

        <!-- 추천 질문 -->
        <div class="suggest-title">이런 질문 어떠세요?</div>
        <div class="suggest-chips">
          <button
            v-for="q in suggestQuestions"
            :key="q"
            class="chip"
            :style="{ borderColor: teamColor }"
            @click="sendSuggest(q)"
            :disabled="loading"
          >{{ q }}</button>
        </div>
      </div>

      <!-- 채팅 메시지 목록 -->
      <div v-for="(msg, i) in messages" :key="i" class="message-row" :class="msg.role">
        <div v-if="msg.role === 'model'" class="avatar" :style="{ borderColor: teamColor }">🤖</div>
        <div class="bubble" :class="msg.role" :style="msg.role === 'model' ? {} : { background: teamColor }">
          <span class="bubble-text">{{ msg.content }}</span>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="loading" class="message-row model">
        <div class="avatar" :style="{ borderColor: teamColor }">🤖</div>
        <div class="bubble model typing">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        </div>
      </div>

    </main>

    <!-- 추천 질문 (메시지 있을 때 하단 위) -->
    <div v-if="messages.length > 0 && !loading" class="suggest-bar">
      <button
        v-for="q in suggestQuestions"
        :key="q"
        class="chip small"
        :style="{ borderColor: teamColor }"
        @click="sendSuggest(q)"
        :disabled="loading"
      >{{ q }}</button>
    </div>

    <!-- 입력 영역 -->
    <footer class="input-area">
      <input
        ref="inputRef"
        v-model="inputText"
        class="msg-input"
        placeholder="야구에 대해 뭐든 물어보세요..."
        maxlength="200"
        @keydown.enter.prevent="send"
        :disabled="loading"
      />
      <button
        class="send-btn"
        :style="{ background: teamColor }"
        @click="send"
        :disabled="loading || !inputText.trim()"
      >전송</button>
    </footer>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { fetchRecommend, sendCoachMessage } from '@/api/surveyApi'

const route = useRoute()
const recommendId = Number(route.params.id)

const teamName = ref('')
const teamColor = ref('#888')
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const chatArea = ref(null)
const inputRef = ref(null)

const bgStyle = computed(() => ({
  background: `linear-gradient(160deg, #0d0d0d 0%, ${teamColor.value}18 100%)`
}))

const backRoute = computed(() => `/onboarding/${recommendId}`)

const suggestQuestions = computed(() => [
  `${teamName.value || '이 팀'}의 주요 선수가 궁금해요`,
  '스트라이크랑 볼이 뭐예요?',
  '야구장 처음인데 뭘 챙겨야 해요?',
  '응원가 어떻게 따라해요?',
])

async function loadTeam() {
  try {
    const result = await fetchRecommend(recommendId)
    teamName.value = result.teamName || ''
    teamColor.value = result.primaryColor || '#EA0029'
  } catch {
    // 팀 정보 없어도 채팅은 동작
  }
}

async function send() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const history = messages.value
      .slice(0, -1)
      .map(m => ({ role: m.role, content: m.content }))

    const res = await sendCoachMessage(recommendId, text, history)
    messages.value.push({ role: 'model', content: res.reply })
  } catch {
    messages.value.push({ role: 'model', content: '답변을 불러오는 데 실패했어요. 다시 시도해주세요.' })
  } finally {
    loading.value = false
    await scrollToBottom()
    inputRef.value?.focus()
  }
}

async function sendSuggest(q) {
  inputText.value = q
  await send()
}

async function scrollToBottom() {
  await nextTick()
  if (chatArea.value) {
    chatArea.value.scrollTop = chatArea.value.scrollHeight
  }
}

onMounted(() => {
  loadTeam()
  inputRef.value?.focus()
})
</script>

<style scoped>
.coach-page {
  display: flex;
  flex-direction: column;
  height: 100dvh;
  max-width: 680px;
  margin: 0 auto;
  color: #fff;
  font-family: inherit;
  overflow: hidden;
}

/* 헤더 */
.coach-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}

.back-btn {
  color: #aaa;
  text-decoration: none;
  font-size: 0.85rem;
  white-space: nowrap;
}

.back-btn:hover { color: #fff; }

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-tag {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 20px;
  color: #fff;
}

.header-team {
  font-size: 0.9rem;
  font-weight: 600;
  color: #eee;
}

/* 채팅 영역 */
.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 인트로 */
.intro-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 20px 8px;
  gap: 14px;
}

.intro-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  background: rgba(255,255,255,0.05);
}

.intro-text {
  font-size: 0.92rem;
  line-height: 1.7;
  color: #ccc;
  margin: 0;
}

.suggest-title {
  font-size: 0.78rem;
  color: #888;
  margin-top: 4px;
}

.suggest-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

/* 메시지 */
.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1.5px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
  background: rgba(255,255,255,0.05);
}

.bubble {
  max-width: 72%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 0.9rem;
  line-height: 1.65;
}

.bubble.model {
  background: rgba(255,255,255,0.07);
  border-radius: 4px 16px 16px 16px;
  color: #e8e8e8;
}

.bubble.user {
  border-radius: 16px 4px 16px 16px;
  color: #fff;
}

.bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}

/* 타이핑 인디케이터 */
.typing {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 12px 16px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #888;
  animation: bounce 1.2s infinite ease-in-out;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.5; }
  40% { transform: translateY(-6px); opacity: 1; }
}

/* 추천 질문 바 */
.suggest-bar {
  padding: 8px 16px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  flex-shrink: 0;
}

.suggest-bar::-webkit-scrollbar { display: none; }

/* 칩 공통 */
.chip {
  background: transparent;
  border: 1px solid;
  border-radius: 20px;
  padding: 7px 13px;
  font-size: 0.83rem;
  color: #ddd;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s, color 0.15s;
}

.chip:hover:not(:disabled) {
  background: rgba(255,255,255,0.08);
  color: #fff;
}

.chip:disabled { opacity: 0.4; cursor: not-allowed; }

.chip.small {
  padding: 5px 11px;
  font-size: 0.78rem;
}

/* 입력 영역 */
.input-area {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
}

.msg-input {
  flex: 1;
  background: rgba(255,255,255,0.07);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  padding: 11px 14px;
  font-size: 0.9rem;
  color: #fff;
  outline: none;
  transition: border-color 0.2s;
}

.msg-input::placeholder { color: #666; }
.msg-input:focus { border-color: rgba(255,255,255,0.3); }
.msg-input:disabled { opacity: 0.5; }

.send-btn {
  padding: 11px 18px;
  border: none;
  border-radius: 12px;
  font-size: 0.88rem;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  transition: opacity 0.2s;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) { opacity: 0.85; }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
