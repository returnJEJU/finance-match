<script setup>
// TODO(담당자): 이 화면을 구현하세요. (기준: 찰떡궁합_UI.pdf)
// 화면: 궁합도 계산 중 · 레이아웃: BlankLayout
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import waitingCharacter from '@/assets/images/characters/character-waiting.png'

const router = useRouter()
//진행률 0에서 시작해서 100%까지
const progress = ref(0)

let progressTimer
let navigationTimer

onMounted(() => {
  progressTimer = window.setInterval(() => {
    if (progress.value < 100) {
      progress.value += 20
    }
  }, 1000)

  navigationTimer = window.setTimeout(() => {
    router.replace('/dashboard')
  }, 5200)
})

onUnmounted(() => {
  window.clearInterval(progressTimer)
  window.clearTimeout(navigationTimer)
})
</script>

<template>
  <main class="flex min-h-screen flex-col items-center bg-brand px-6">
    <!-- 캐릭터 -->
    <img
      :src="waitingCharacter"
      alt="금융 궁합도를 계산하는 중"
      class="mt-[210px] h-[190px] w-[220px] object-contain"
    />

    <!-- 제목 -->
    <h1 class="mt-8 text-center text-[24px] leading-[1.35] font-bold text-brand-ink">
      우리 부부의 금융 궁합도를<br />
      계산하고 있어요...
    </h1>

    <!-- 설명 -->
    <p class="mt-3 text-center text-[16px] leading-6 text-brand-ink">
      두 분의 성향을 찰떡같이 분석 중입니다.<br />
      조금만 기다려주세요!!
    </p>

    <!-- 진행률 -->
    <div class="mt-10 w-full max-w-[300px]">
      <!-- 진행 바 -->
      <div class="h-[10px] overflow-hidden rounded-full bg-white/60">
        <div
          class="h-full rounded-full bg-brand-ink transition-[width] duration-500 ease-out"
          :style="{ width: `${progress}%` }"
        />
      </div>

      <!-- 진행률 숫자 -->
      <p class="mt-2 text-center text-[13px] font-semibold text-brand-ink">{{ progress }}% 완료</p>
    </div>
  </main>
</template>
