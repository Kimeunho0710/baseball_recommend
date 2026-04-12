import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import SurveyView from '../views/SurveyView.vue'
import ResultView from '../views/ResultView.vue'
import TeamsView from '../views/TeamsView.vue'
import TeamDetailView from '../views/TeamDetailView.vue'
import StandingView from '../views/StandingView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/survey', name: 'survey', component: SurveyView },
    { path: '/result', name: 'result', component: ResultView },
    { path: '/teams', name: 'teams', component: TeamsView },
    { path: '/teams/:id', name: 'team-detail', component: TeamDetailView },
    { path: '/standings', name: 'standings', component: StandingView }
  ]
})

export default router
