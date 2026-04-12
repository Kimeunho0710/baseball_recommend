import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

export async function fetchQuestions() {
  const { data } = await api.get('/survey/questions')
  return data
}

export async function submitSurvey(answers) {
  const { data } = await api.post('/survey/submit', { answers })
  return data
}

export async function fetchTeams() {
  const { data } = await api.get('/teams')
  return data
}

export async function fetchTeam(id) {
  const { data } = await api.get(`/teams/${id}`)
  return data
}

export async function fetchStandings() {
  const { data } = await api.get('/standings')
  return data
}
