import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, signup as apiSignup, logout as apiLogout } from '../api/authApi'

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

  async function logout() {
    try {
      await apiLogout()  // 서버: DB에서 Refresh Token 삭제 + 쿠키 만료
    } catch {
      // 서버 요청 실패(네트워크 오류 등)해도 로컬 상태는 반드시 정리
    } finally {
      _clearAuth()
    }
  }

  function _setAuth(data) {
    token.value = data.token
    user.value = { memberId: data.memberId, email: data.email, nickname: data.nickname }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function _clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, login, signup, logout }
})
