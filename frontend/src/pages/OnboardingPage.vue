<script setup>
// 온보딩 화면 (앱 진입) · 레이아웃: BlankLayout
// 카카오 로그인은 아직 붙이지 않는다. 버튼만 두고 인증 도메인이 준비되면 클릭 핸들러를 채운다.
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterMarried from '@/assets/images/characters/character-married.png'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'

const route = useRoute()
const router = useRouter()
const toastMessage = ref('')
let toastTimer = null

onMounted(() => {
  const query = { ...route.query }

  if (route.query.withdraw === 'success') {
    toastMessage.value = '탈퇴가 완료되었습니다.'
    delete query.withdraw
  }

  if (!toastMessage.value) return

  router.replace({ name: 'onboarding', query })

  toastTimer = window.setTimeout(() => {
    toastMessage.value = ''
  }, 2400)
})

onBeforeUnmount(() => {
  if (toastTimer) {
    window.clearTimeout(toastTimer)
  }
})
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <div
      v-if="toastMessage"
      class="fixed left-1/2 top-5 z-[120] w-[calc(100%-40px)] max-w-[360px] -translate-x-1/2 rounded-xl bg-gray-900 px-4 py-3 text-center text-[13px] font-semibold text-white shadow-lg"
    >
      {{ toastMessage }}
    </div>

    <!-- pt-* 는 가운데 정렬을 유지한 채 덩어리를 내리는 손잡이다. 위쪽에만 여백이 생기므로
         남는 공간이 위아래로 나뉘는 지점이 내려가고, 실제로는 <b>준 값의 절반</b>만큼 내려온다.
         (pt-16 = 64px → 약 32px 아래로) -->
    <div class="flex flex-1 flex-col items-center justify-center px-5 pt-16 text-center">
      <img :src="logoWordmark" alt="찰떡귱합" class="mb-2 w-[260px]" />
      <p class="text-muted mb-2.5 text-[12.5px]">"우리의 금융 궁합도 계산기"</p>
      <img :src="characterMarried" alt="" class="-mt-5 -mb-7 w-[210px]" />

      <PageTitle class="mt-2.5">예비 부부를 위한<br />달콤한 금융 궁합</PageTitle>
      <p class="text-muted mt-2.5 text-[13px] leading-[1.5]">
        서로 다른 두 사람의 경제력을 하나로,<br />찰떡같이 붙는 자산 관리를 시작해보세요.
      </p>
    </div>

    <!-- 하단 영역
         pb-*   : 로그인 링크를 화면 맨 아래에서 띄우는 거리 -->
    <div class="px-7 pb-[26px]">
      <!-- 버튼 두 개
           gap-*  : 두 버튼 사이 간격
           mb-*   : 로그인 링크와의 거리 → 키우면 버튼 두 개가 위로 올라간다 -->
      <div class="mb-2.5 flex flex-col gap-5">
        <!-- TODO(인증): 카카오 로그인 연동 후 클릭 시 카카오 동의 화면으로 이동 -->
        <BaseButton variant="kakao">카카오로 계속하기</BaseButton>

        <BaseButton :to="{ name: 'signup' }">시작하기</BaseButton>
      </div>

      <!-- '로그인' 만 링크. 앞 문구는 일반 텍스트라 눌러도 흐려지지 않는다. -->
      <p class="text-muted-soft text-center text-[13px]">
        이미 계정이 있나요?
        <RouterLink
          :to="{ name: 'login' }"
          class="text-ink-sub ml-3 font-bold transition-opacity active:opacity-60"
        >
          로그인
        </RouterLink>
      </p>
    </div>
  </div>
</template>
