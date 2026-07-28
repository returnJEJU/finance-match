<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import BottomNav from '@/components/layout/BottomNav.vue'

// 상단바 제목·뒤로가기는 라우터 meta에서 읽는다 (화면마다 다름).
//   예: meta: { title: '리포트', showBack: true }
//   title 없으면 로고 표시, showBack 없으면 뒤로가기 숨김.
const route = useRoute()
const title = computed(() => route.meta.title ?? '')
const showBack = computed(() => route.meta.showBack ?? false)
</script>

<template>
  <!-- 배경색은 main.css 의 body 에서 정한다. 여기서 덮지 않는다. -->
  <div class="mx-auto min-h-screen max-w-[393px]">
    <AppHeader :title="title" :show-back="showBack" />

    <!-- 상단바(50px)·하단탭(58px)에 가리지 않게 여백 -->
    <main class="min-h-screen pt-[50px] pb-[58px]">
      <RouterView />
    </main>

    <BottomNav />
  </div>
</template>
