<script setup>
import { useRouter } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'
import logoUrl from '@/assets/images/logo/logo-wordmark.png'

// title: 가운데 제목. 비어 있으면 로고를 표시(홈 화면).
// showBack: 왼쪽 뒤로가기(<) 표시 여부.
defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: true },
})

const router = useRouter()
</script>

<template>
  <header
    class="fixed inset-x-0 top-0 z-40 mx-auto flex h-[50px] max-w-[393px] items-center justify-between border-b border-gray-100 bg-white px-4"
  >
    <!-- 왼쪽: 뒤로가기 -->
    <button
      v-if="showBack"
      type="button"
      class="flex h-7 w-7 items-center justify-center text-gray-900"
      aria-label="뒤로가기"
      @click="router.back()"
    >
      <ChevronLeft :size="24" />
    </button>
    <span v-else class="h-7 w-7" />

    <!-- 가운데: 제목 없으면 로고, 있으면 제목 -->
    <img v-if="!title" :src="logoUrl" alt="찰떡궁합" class="h-6 w-auto" />
    <h1 v-else class="text-[15px] font-bold text-gray-900">{{ title }}</h1>

    <!-- 오른쪽: 알림 없음 → 제목 가운데 정렬용 빈 자리 -->
    <span class="h-7 w-7" />
  </header>
</template>
