<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { calculateCompatibility } from '@/api/match'
import waitingCharacter from '@/assets/images/characters/character-waiting.png'

const router = useRouter()

const progress = ref(0)
const calculating = ref(false)
const errorMessage = ref('')

let progressTimer
let navigationTimer
let destroyed = false

const clearTimers = () => {
  window.clearInterval(progressTimer)
  window.clearTimeout(navigationTimer)
}

const wait = (milliseconds) =>
  new Promise((resolve) => {
    window.setTimeout(resolve, milliseconds)
  })

const startCalculation = async () => {
  if (calculating.value) {
    return
  }

  clearTimers()

  progress.value = 0
  calculating.value = true
  errorMessage.value = ''

  // API 응답을 기다리는 동안 진행률은 최대 90%까지만 표시
  progressTimer = window.setInterval(() => {
    if (progress.value < 90) {
      progress.value += 5
    }
  }, 150)

  try {
    // 실제 궁합도 계산 API 호출
    // 화면이 너무 빠르게 사라지지 않도록 최소 2.5초 동안 표시
    await Promise.all([calculateCompatibility(), wait(2500)])

    if (destroyed) {
      return
    }

    window.clearInterval(progressTimer)
    progress.value = 100

    navigationTimer = window.setTimeout(() => {
      router.replace('/dashboard')
    }, 500)
  } catch (error) {
    if (destroyed) {
      return
    }

    window.clearInterval(progressTimer)

    errorMessage.value = error?.message || '금융 궁합도 계산 중 오류가 발생했습니다.'
  } finally {
    calculating.value = false
  }
}

onMounted(() => {
  startCalculation()
})

onUnmounted(() => {
  destroyed = true
  clearTimers()
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

    <!-- 오류 메시지 및 재시도 -->
    <div v-if="errorMessage" class="mt-6 text-center">
      <p class="text-[14px] font-medium text-red-600">
        {{ errorMessage }}
      </p>

      <button
        type="button"
        class="mt-4 rounded-full bg-brand-ink px-6 py-3 text-[14px] font-bold text-white disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="calculating"
        @click="startCalculation"
      >
        다시 시도하기
      </button>
    </div>

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
