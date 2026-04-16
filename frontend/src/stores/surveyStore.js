import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchQuestions, submitSurvey } from '../api/surveyApi'

export const useSurveyStore = defineStore('survey', () => {
  const questions = ref([])
  const answers = ref({})
  const result = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function loadQuestions() {
    loading.value = true
    error.value = null
    try {
      questions.value = await fetchQuestions()
    } catch (e) {
      error.value = '질문을 불러오는 중 오류가 발생했습니다.'
    } finally {
      loading.value = false
    }
  }

  function setAnswer(questionId, value) {
    answers.value[questionId] = value
  }

  async function submit() {
    loading.value = true
    error.value = null
    try {
      result.value = await submitSurvey(answers.value)
      // 홈에서 이력 표시를 위해 핵심 정보 저장
      localStorage.setItem('lastRecommend', JSON.stringify({
        recommendId: result.value.recommendId,
        teamName: result.value.teamName,
        teamId: result.value.teamId,
        fanProfile: result.value.fanProfile,
      }))
      return result.value
    } catch (e) {
      error.value = 'AI 추천 중 오류가 발생했습니다. 다시 시도해주세요.'
      throw e
    } finally {
      loading.value = false
    }
  }

  function reset() {
    answers.value = {}
    result.value = null
    error.value = null
  }

  return { questions, answers, result, loading, error, loadQuestions, setAnswer, submit, reset }
})
