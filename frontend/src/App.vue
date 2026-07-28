<script setup>
import { RouterView } from 'vue-router'

// 레이아웃 틀. 네비바·상단바 등 고정 요소는 여기(RouterView 위/아래)에 배치한다.
// 예: <AppHeader /> ... <RouterView /> ... <AppNavbar />
// 지금은 페이지 영역(RouterView)만 둔다.
</script>

<template>
  <RouterView />
</template>
