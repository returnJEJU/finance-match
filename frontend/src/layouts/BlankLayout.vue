<script setup>
import { RouterView } from 'vue-router'

// 상단바·하단탭 없이 화면만 꽉 채우는 레이아웃.
// 로그인·회원가입·설문·로딩 등 몰입 화면에 사용.
</script>

<template>
  <!-- 배경색은 main.css 의 body 에서 정한다. 여기서 덮지 않는다. -->
  <div class="mx-auto min-h-screen max-w-[393px]">
    <RouterView />
  </div>
</template>
