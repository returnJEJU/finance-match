<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { ArrowRight } from 'lucide-vue-next'
import { getPersonalSurveyResult } from '@/api/personalSurvey'
import { getOnboardingStatus } from '@/api/onboarding'
import BaseButton from '@/components/ui/BaseButton.vue'
import { investmentTypeMeta } from '@/constants/investmentTypeMeta'

const {
  data: result,
  isError,
  refetch,
} = useQuery({
  queryKey: ['personal-survey-result'],
  queryFn: getPersonalSurveyResult,
})

const resultMeta = computed(() => {
  if (!result.value) {
    return null
  }

  return investmentTypeMeta[result.value.investmentType]
})

// 버튼 클릭 함수
const router = useRouter()

async function handleNext() {
  const status = await getOnboardingStatus()

  if (status.partnerPersonalSurveyCompleted === true) {
    router.push({ name: 'match-calculating' })
    return
  }

  router.push({ name: 'dashboard-waiting' })
}
</script>

<template>
  <main class="flex min-h-screen flex-col px-5 pt-3 pb-6">
    <header class="flex h-8 flex-none items-center justify-center">
      <h1 class="text-center text-[20px] font-semibold text-ink">나의 금융 스타일 결과</h1>
    </header>

    <!-- 오류 UI -->
    <div v-if="isError" class="flex flex-1 flex-col items-center justify-center px-5 text-center">
      <p class="text-warn text-[16px] leading-7 font-medium">
        금융 스타일 결과를 불러오지 못했어요.<br />
        잠시 후 다시 시도해 주세요.
      </p>

      <button
        type="button"
        class="border-line-field mt-5 rounded-full border bg-white px-6 py-3 text-[18px] font-semibold"
        @click="refetch"
      >
        다시 시도하기
      </button>
    </div>

    <section v-else-if="result && resultMeta" class="flex flex-1 flex-col">
      <!-- 결과 문구 -->
      <div class="mt-10 text-center">
        <p class="text-[18px] font-medium tracking-[-0.2px]">{{ result.name }}님의 금융 스타일은</p>

        <p class="mt-2 flex items-baseline justify-center gap-2 tracking-[-1px]">
          <strong class="text-[48px] leading-[1.2] font-extrabold" :class="resultMeta.accentClass">
            {{ result.investmentType }}
          </strong>

          <!-- <span class="text-[28px] font-medium">입니다.</span> -->
        </p>
      </div>

      <!-- 캐릭터 -->
      <div class="relative mt-0 flex h-[250px] items-center justify-center">
        <div
          class="absolute h-[250px] w-[330px] rounded-full bg-[radial-gradient(circle,_rgba(255,244,79,0.32)_0%,_rgba(255,244,79,0.14)_50%,_transparent_74%)]"
          aria-hidden="true"
        ></div>

        <img
          :src="resultMeta.character"
          :alt="`${result.investmentType} 캐릭터`"
          class="relative h-[210px] w-[210px] object-contain"
        />
      </div>

      <!-- 설명 카드 -->
      <article
        class="border-line-card rounded-card mt-auto flex h-[230px] flex-none flex-col overflow-hidden border bg-white px-6 py-6"
      >
        <div class="flex flex-none items-start gap-2">
          <component
            :is="resultMeta.icon"
            class="mt-0.5 h-5 w-5 flex-none text-[#777000]"
            :stroke-width="2"
            aria-hidden="true"
          />

          <h2 class="text-[20px] leading-[1.4] font-bold">
            {{ result.headline }}
          </h2>
        </div>

        <div
          class="text-ink-sub mt-4 min-h-0 flex-1 overflow-y-auto pr-2 text-[18px] leading-[1.7]"
        >
          <p>{{ result.description }}</p>
        </div>
      </article>
    </section>

    <!-- 하단 CTA -->
    <div v-if="result && resultMeta" class="mt-5 flex-none">
      <BaseButton class="w-full text-[20px]" @click="handleNext">
        금융 궁합도 확인하러 가기
        <ArrowRight class="h-[19px] w-[19px]" :stroke-width="2.2" aria-hidden="true" />
      </BaseButton>
    </div>
  </main>
</template>
