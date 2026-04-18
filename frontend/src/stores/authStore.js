import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, signup as apiSignup, fetchMe } from '../api/authApi'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  async function login(email, password) {
    const data = await apiLogin(email, password)
    _setAuth(data)
    return data
  }

  async function signup(email, password, nickname) {
    const data = await apiSignup(email, password, nickname)
    _setAuth(data)
    return data
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // 앱 시작 시 localStorage의 토큰이 유효한지 서버에서 검증
  async function verifySession() {
    if (!token.value) return
    try {
      const data = await fetchMe()
      user.value = { memberId: data.memberId, email: data.email, nickname: data.nickname }
    } catch {
      // 토큰이 만료되었거나 유효하지 않으면 로그아웃
      logout()
    }
  }

  function _setAuth(data) {
    token.value = data.token
    user.value = { memberId: data.memberId, email: data.email, nickname: data.nickname }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  return { token, user, isLoggedIn, login, signup, logout, verifySession }
})
