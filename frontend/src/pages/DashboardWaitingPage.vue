<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { CreditCard, Heart, LockKeyhole, RefreshCw } from 'lucide-vue-next'

import { getOnboardingStatus } from '@/api/onboarding'

const router = useRouter()

const refreshing = ref(false)
const refreshMessage = ref('')

const handleRefresh = async () => {
  if (refreshing.value) {
    return
  }

  refreshing.value = true
  refreshMessage.value = ''

  try {
    const status = await getOnboardingStatus()

    if (!status.coupleConnected) {
      refreshMessage.value = '아직 상대방과 연결되지 않았습니다.'
      return
    }

    if (!status.personalSurveyCompleted) {
      refreshMessage.value = '아직 내 개인설문이 완료되지 않았습니다.'
      return
    }

    if (status.partnerPersonalSurveyCompleted !== true) {
      refreshMessage.value = '상대방의 개인설문이 완료되지 않았습니다.'
      return
    }

    router.push('/match/calculating')
  } catch (error) {
    refreshMessage.value =
      error?.message || '상태를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    refreshing.value = false
  }
}
</script>

<template>
  <section class="flex min-h-[calc(100vh-108px)] flex-col px-5 py-4">
    <!-- 상단 안내 문구 -->
    <p class="text-center text-[15px] font-semibold text-brand-ink">
      함께 만드는 안정적인 우리의 금융 미래 💛
    </p>

    <!-- 흰색 메인 카드 -->
    <div
      class="mt-5 flex flex-1 flex-col items-center rounded-[30px] bg-white px-6 pt-12 pb-10 shadow-[0_4px_20px_rgba(0,0,0,0.04)]"
    >
      <!-- 자물쇠 영역 -->
      <div class="relative flex h-[230px] w-[230px] items-center justify-center">
        <!-- 연한 노란색 원 -->
        <div class="absolute inset-0 rounded-full bg-brand-soft" />

        <!-- 원 안쪽 장식 -->
        <div class="absolute inset-[18px] rounded-full border-[10px] border-white/50" />

        <!-- 왼쪽 하트 -->
        <Heart class="absolute top-1 left-0 text-[#ebe9dc]" :size="43" :stroke-width="1.8" />

        <!-- 오른쪽 카드 장식 -->
        <CreditCard
          class="absolute right-0 bottom-8 rotate-12 text-[#d7d3c0]"
          :size="36"
          :stroke-width="1.5"
        />

        <!-- 가운데 흰색 원 -->
        <div
          class="relative flex h-[112px] w-[112px] items-center justify-center rounded-full bg-white shadow-[0_8px_20px_rgba(90,85,30,0.16)]"
        >
          <LockKeyhole class="text-brand-ink" :size="54" :stroke-width="2.4" />
        </div>
      </div>

      <!-- 안내 문구 -->
      <div class="mt-10 text-center">
        <h1 class="text-[18px] font-bold">아직 궁합을 계산할 수 없습니다</h1>

        <p class="mt-3 text-[15px] leading-6 text-ink-sub">
          상대방이 개인 설문을 완료하면<br />
          새로고침 버튼을 눌러 확인해주세요.
        </p>
      </div>

      <!-- 새로고침 버튼 -->
      <button
        type="button"
        class="mt-12 flex h-12 w-[200px] items-center justify-center gap-2 rounded-full border-2 border-brand bg-white text-[15px] font-semibold transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="refreshing"
        @click="handleRefresh"
      >
        <RefreshCw :size="17" :stroke-width="2.2" :class="{ 'animate-spin': refreshing }" />

        {{ refreshing ? '확인 중...' : '새로고침' }}
      </button>

      <!-- 상태 안내 문구 -->
      <p
        v-if="refreshMessage"
        role="alert"
        class="mt-4 text-center text-[12px] leading-5 font-semibold text-red-500"
      >
        {{ refreshMessage }}<br />
        잠시만 기다려주세요.
      </p>
    </div>
  </section>
</template>
