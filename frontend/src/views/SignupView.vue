<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-title">⚾ 회원가입</h1>
      <p class="auth-sub">가입하면 내 추천 기록이 영구 저장됩니다.</p>

      <form @submit.prevent="handleSignup" class="auth-form">
        <div class="field">
          <label>이메일</label>
          <input v-model="email" type="email" placeholder="example@email.com" required />
        </div>
        <div class="field">
          <label>닉네임</label>
          <input v-model="nickname" type="text" placeholder="2~20자" required minlength="2" maxlength="20" />
        </div>
        <div class="field">
          <label>비밀번호</label>
          <input v-model="password" type="password" placeholder="8자 이상" required minlength="8" />
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '가입 중...' : '회원가입' }}
        </button>
      </form>

      <p class="auth-footer">
        이미 계정이 있으신가요?
        <RouterLink to="/login">로그인</RouterLink>
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
const nickname = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function handleSignup() {
  loading.value = true
  error.value = ''
  try {
    await authStore.signup(email.value, password.value, nickname.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '회원가입 중 오류가 발생했습니다.'
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
