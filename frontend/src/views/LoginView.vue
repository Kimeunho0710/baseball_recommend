<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-title">⚾ 로그인</h1>
      <p class="auth-sub">내 추천 기록을 저장하고 언제든 다시 확인하세요.</p>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="field">
          <label>이메일</label>
          <input v-model="email" type="email" placeholder="example@email.com" required />
        </div>
        <div class="field">
          <label>비밀번호</label>
          <input v-model="password" type="password" placeholder="비밀번호 입력" required />
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <p class="auth-footer">
        아직 계정이 없으신가요?
        <RouterLink to="/signup">회원가입</RouterLink>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  loading.value = true
  error.value = ''
  try {
    await authStore.login(email.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '이메일 또는 비밀번호가 올바르지 않습니다.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px 40px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 40px 36px;
  width: 100%;
  max-width: 420px;
  color: white;
}

.auth-title {
  font-size: 1.8rem;
  font-weight: 800;
  text-align: center;
  margin-bottom: 8px;
}

.auth-sub {
  font-size: 0.88rem;
  color: #a8d8ea;
  text-align: center;
  margin-bottom: 32px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 0.82rem;
  color: #aaa;
  font-weight: 500;
}

.field input {
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  padding: 12px 14px;
  color: white;
  font-size: 0.95rem;
  outline: none;
  transition: border-color 0.2s;
}

.field input:focus {
  border-color: #e94560;
}

.field input::placeholder {
  color: #555;
}

.error-msg {
  font-size: 0.83rem;
  color: #ff6b6b;
  text-align: center;
}

.submit-btn {
  background: #e94560;
  color: white;
  border: none;
  border-radius: 12px;
  padding: 14px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  margin-top: 4px;
  transition: opacity 0.2s;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-btn:not(:disabled):hover {
  opacity: 0.88;
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 0.85rem;
  color: #888;
}

.auth-footer a {
  color: #a8d8ea;
  text-decoration: none;
  font-weight: 600;
}
</style>
