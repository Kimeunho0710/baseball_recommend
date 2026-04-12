<template>
  <div class="home">
    <div class="hero">
      <div class="hero-content">
        <h1 class="title">⚾ KBO 팀 추천</h1>
        <p class="subtitle">나와 딱 맞는 KBO 야구팀을 찾아보세요!</p>
        <p class="description">
          간단한 성향 설문을 통해 AI가 KBO 10개 팀 중<br />
          당신에게 가장 잘 맞는 팀을 추천해 드립니다.
        </p>
        <RouterLink to="/survey" class="start-btn">
          설문 시작하기 →
        </RouterLink>
      </div>
    </div>

    <div class="teams-preview">
      <h2>KBO 10개 팀</h2>
      <div class="team-list">
        <div
          v-for="team in teams"
          :key="team.name"
          class="team-chip"
          :style="{ borderColor: team.color }"
        >
          {{ team.name }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { RouterLink } from 'vue-router'

const teams = [
  { name: 'KIA 타이거즈', color: '#EA0029' },
  { name: '삼성 라이온즈', color: '#074CA1' },
  { name: 'LG 트윈스', color: '#C30452' },
  { name: '두산 베어스', color: '#131230' },
  { name: 'KT 위즈', color: '#000000' },
  { name: 'SSG 랜더스', color: '#CE0E2D' },
  { name: '롯데 자이언츠', color: '#041E42' },
  { name: '한화 이글스', color: '#FF6600' },
  { name: 'NC 다이노스', color: '#071D49' },
  { name: '키움 히어로즈', color: '#820024' },
]
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: white;
  padding-top: 60px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.title {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 16px;
}

.subtitle {
  font-size: 1.4rem;
  color: #a8d8ea;
  margin-bottom: 20px;
}

.description {
  font-size: 1rem;
  color: #ccc;
  line-height: 1.8;
  margin-bottom: 40px;
}

.start-btn {
  display: inline-block;
  background: #e94560;
  color: white;
  padding: 16px 40px;
  border-radius: 50px;
  font-size: 1.1rem;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.2s, background 0.2s;
}

.start-btn:hover {
  background: #c73652;
  transform: translateY(-2px);
}

.teams-preview {
  padding: 40px 20px 60px;
  text-align: center;
}

.teams-preview h2 {
  font-size: 1.5rem;
  margin-bottom: 24px;
  color: #a8d8ea;
}

.team-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  max-width: 700px;
  margin: 0 auto;
}

.team-chip {
  padding: 8px 18px;
  border: 2px solid;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 500;
}
</style>
