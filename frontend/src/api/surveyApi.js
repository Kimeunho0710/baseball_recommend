import http from './http'

const api = http

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

export async function fetchRecommend(id) {
  const { data } = await api.get(`/recommend/${id}`)
  return data
}

export async function fetchTeamPlayers(teamId) {
  const { data } = await api.get(`/teams/${teamId}/players`)
  return data
}

export async function fetchTodayGames() {
  const { data } = await api.get('/games/today')
  return data
}

export async function fetchRecentGames() {
  const { data } = await api.get('/games/recent')
  return data
}

export async function fetchTeamForm(teamName) {
  const { data } = await api.get(`/games/form/${encodeURIComponent(teamName)}`)
  return data
}

export async function fetchPopularTeams() {
  const { data } = await api.get('/recommend/popular-teams')
  return data
}
