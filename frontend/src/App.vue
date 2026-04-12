<template>
  <div id="app">
    <nav class="global-nav">
      <div class="nav-inner">
        <RouterLink to="/" class="nav-logo">⚾ KBO 추천</RouterLink>
        <div class="nav-links">
          <RouterLink to="/teams">팀 정보</RouterLink>
          <RouterLink to="/standings">순위</RouterLink>
          <RouterLink to="/survey" class="nav-cta">팀 추천받기</RouterLink>
        </div>
      </div>
    </nav>
    <RouterView />
  </div>
</template>

<script setup>
import { RouterView, RouterLink } from 'vue-router'
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }

.global-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(15, 15, 30, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.nav-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-logo {
  color: white;
  text-decoration: none;
  font-size: 1.1rem;
  font-weight: 700;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.nav-links a {
  color: #ccc;
  text-decoration: none;
  font-size: 0.95rem;
  transition: color 0.2s;
}

.nav-links a:hover,
.nav-links a.router-link-active {
  color: white;
}

.nav-cta {
  background: #e94560;
  color: white !important;
  padding: 8px 18px;
  border-radius: 20px;
  font-weight: 600;
}

.nav-cta:hover {
  background: #c73652;
}
</style>
