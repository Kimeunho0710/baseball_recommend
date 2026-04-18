import http from './http'

export async function signup(email, password, nickname) {
  const { data } = await http.post('/auth/signup', { email, password, nickname })
  return data
}

export async function login(email, password) {
  const { data } = await http.post('/auth/login', { email, password })
  return data
}

export async function fetchMe() {
  const { data } = await http.get('/auth/me')
  return data
}

export async function fetchMyRecommendations() {
  const { data } = await http.get('/auth/me/recommendations')
  return data
}

export async function logout() {
  await http.post('/auth/logout')
}
