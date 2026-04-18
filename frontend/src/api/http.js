import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true  // HttpOnly Refresh Token 쿠키 자동 전송/수신
})

// 요청 인터셉터: Access Token 자동 첨부
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── 자동 토큰 재발급 (Refresh Token Rotation) ──────────────────────────────
let isRefreshing = false
let failedQueue = []  // refresh 중 실패한 요청들의 대기열

function processQueue(error, token = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token)
  })
  failedQueue = []
}

// 응답 인터셉터: 401 감지 → 자동 refresh → 원래 요청 재시도
http.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config

    // 조건: 401이고 / 재시도 아니고 / /refresh 엔드포인트 자체가 아닌 경우
    const isRefreshEndpoint = originalRequest.url?.includes('/auth/refresh')
    if (error.response?.status !== 401 || originalRequest._retry || isRefreshEndpoint) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      // 이미 refresh 진행 중 → 완료 후 재시도하도록 큐에 등록
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      }).then(token => {
        originalRequest.headers['Authorization'] = `Bearer ${token}`
        return http(originalRequest)
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      // Refresh Token(쿠키)으로 새 Access Token 요청
      const { data } = await http.post('/auth/refresh')
      const newToken = data.token

      localStorage.setItem('token', newToken)
      http.defaults.headers.common['Authorization'] = `Bearer ${newToken}`

      // 대기 중이던 요청들 재시도
      processQueue(null, newToken)

      // 원래 요청 재시도
      originalRequest.headers['Authorization'] = `Bearer ${newToken}`
      return http(originalRequest)
    } catch (refreshError) {
      // Refresh Token도 만료 → 완전 로그아웃
      processQueue(refreshError, null)
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = '/login'
      }
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }
)

export default http
