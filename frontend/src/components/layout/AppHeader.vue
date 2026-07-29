<script setup>
// 진행바 없는 상단바. 뒤로가기 + 가운데 제목(또는 로고).
//
//   <AppHeader title="리포트" show-back />                    허브 화면 — 위에 고정
//   <AppHeader variant="plain" title="로그인" :fallback-to="{ name: 'onboarding' }" />
//
// 진행바가 있는 회원가입 퍼널은 생김새가 달라 FunnelHeader 를 따로 쓴다.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'
import logoUrl from '@/assets/images/logo/logo-wordmark.png'

const props = defineProps({
  // 가운데 제목. 비어 있으면 로고를 표시(홈 화면).
  title: { type: String, default: '' },
  // 왼쪽 뒤로가기(<) 표시 여부.
  showBack: { type: Boolean, default: true },
  // fixed 허브 화면용(위에 고정 · 흰 배경 · 아래 테두리)
  // plain  몰입 화면용(그냥 맨 위에 놓임 · 배경/테두리 없음 · 좌우 여백 px-7)
  variant: {
    type: String,
    default: 'fixed',
    validator: (v) => ['fixed', 'plain'].includes(v),
  },
  // 앱 안에 돌아갈 기록이 없을 때(주소 직접 입력·새로고침) 대신 갈 화면
  fallbackTo: { type: [String, Object], default: null },
})

const router = useRouter()

const VARIANT_CLASS = {
  fixed:
    'border-line-soft fixed inset-x-0 top-0 z-40 mx-auto h-[50px] max-w-[393px] border-b bg-white px-4',
  plain: 'px-7 pt-4 pb-2.5',
}

const layoutClass = computed(() => VARIANT_CLASS[props.variant])

// 주소를 직접 열거나 새로고침하면 앱 안에 기록이 없어 router.back() 이 앱 밖으로 나간다.
function goBack() {
  if (window.history.state?.back) router.back()
  else if (props.fallbackTo) router.replace(props.fallbackTo)
}
</script>

<template>
  <header class="flex items-center justify-between" :class="layoutClass">
    <!-- 왼쪽: 뒤로가기 -->
    <button
      v-if="showBack"
      type="button"
      class="text-ink flex h-7 w-7 cursor-pointer items-center justify-center"
      aria-label="뒤로 가기"
      @click="goBack"
    >
      <ChevronLeft :size="24" />
    </button>
    <span v-else class="h-7 w-7" />

    <!-- 가운데: 제목 없으면 로고, 있으면 제목 -->
    <img v-if="!title" :src="logoUrl" alt="찰떡귱합" class="h-6 w-auto" />
    <h1 v-else class="text-ink text-[15px] font-bold">{{ title }}</h1>

    <!-- 오른쪽: 알림 없음 → 제목 가운데 정렬용 빈 자리 -->
    <span class="h-7 w-7" />
  </header>
</template>
