import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, signup as apiSignup } from '../api/authApi'

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

  function _setAuth(data) {
    token.value = data.token
    user.value = { memberId: data.memberId, email: data.email, nickname: data.nickname }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  return { token, user, isLoggedIn, login, signup, logout }
})
