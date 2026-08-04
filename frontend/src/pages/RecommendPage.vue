<script setup>
import { computed, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { LoaderCircle, RotateCcw } from 'lucide-vue-next'
import { createRecommendation, getRecommendation } from '@/api/recommendation'
import { getCompatibility } from '@/api/match'
import HighInterestDebtNotice from '@/components/recommendation/HighInterestDebtNotice.vue'
import PersonalRecommendation from '@/components/recommendation/PersonalRecommendation.vue'
import RecommendationPackage from '@/components/recommendation/RecommendationPackage.vue'

const fetchRecommendation = async () => {
  try {
    return await getRecommendation()
  } catch (error) {
    if (error.code !== 'RECOMMENDATION_NOT_FOUND') {
      throw error
    }

    await createRecommendation()
    return getRecommendation()
  }
}

const {
  data: recommendation,
  error,
  isPending,
  refetch,
} = useQuery({
  queryKey: ['recommendation'],
  queryFn: fetchRecommendation,
  retry: false,
})
const debtNoticeConfirmed = ref(false)

const { data: compatibility } = useQuery({
  queryKey: ['compatibility'],
  queryFn: getCompatibility,
  retry: false,
})

const errorCode = computed(() => error.value?.code)
const isNotReady = computed(() => errorCode.value === 'RECOMMENDATION_NOT_READY')
const visibleSlots = computed(() =>
  (recommendation.value?.packageSlots ?? []).filter(({ products }) => products.length > 0),
)
const recommendationsDimmed = computed(
  () => recommendation.value?.hasHighInterestDebt && !debtNoticeConfirmed.value,
)
const compatibilityScore = computed(() =>
  compatibility.value ? Math.round(compatibility.value.totalScore) : null,
)
</script>

<template>
  <section class="min-h-[calc(100vh-108px)] bg-white px-4 pt-5 pb-10">
    <div v-if="isPending" class="flex min-h-[460px] flex-col items-center justify-center">
      <LoaderCircle class="h-7 w-7 animate-spin text-brand-deep" />
      <p class="mt-3 text-[14px] text-muted">맞춤 상품을 준비하고 있어요</p>
    </div>

    <div
      v-else-if="error"
      class="flex min-h-[460px] flex-col items-center justify-center text-center"
    >
      <div class="flex h-14 w-14 items-center justify-center rounded-full bg-brand-soft text-2xl">
        {{ isNotReady ? '📝' : '😥' }}
      </div>

      <h1 class="mt-4 text-[20px] font-bold">
        {{ isNotReady ? '추천에 필요한 정보가 없습니다' : '추천을 불러오지 못했습니다' }}
      </h1>

      <p class="mt-2 text-[14px] leading-6 text-muted">
        {{
          isNotReady
            ? '맞춤 추천을 받으려면 설문과 금융정보 입력을 완료해 주세요.'
            : '잠시 후 다시 시도해 주세요.'
        }}
      </p>

      <button
        v-if="!isNotReady"
        type="button"
        class="mt-5 inline-flex h-12 items-center gap-2 rounded-xl bg-ink px-6 text-[14px] font-semibold text-white"
        @click="refetch()"
      >
        <RotateCcw class="h-4 w-4" />
        다시 시도
      </button>
    </div>

    <template v-else-if="recommendation">
      <header>
        <h1 class="text-[22px] font-extrabold tracking-[-0.03em]">우리를 위한 추천 패키지</h1>

        <p class="mt-1.5 text-[14px] leading-5 text-muted">
          <template v-if="compatibilityScore !== null">
            우리 궁합 {{ compatibilityScore }}점 · 두 사람의 목표와 자산 흐름을 반영했어요
          </template>
          <template v-else>두 사람의 목표와 자산 흐름을 반영했어요</template>
        </p>
      </header>

      <HighInterestDebtNotice
        v-if="recommendation.hasHighInterestDebt && !debtNoticeConfirmed"
        class="mt-5"
        @confirm="debtNoticeConfirmed = true"
      />

      <div
        class="mt-5 transition duration-300"
        :class="recommendationsDimmed ? 'pointer-events-none opacity-20 blur-[0.5px]' : ''"
        :aria-hidden="recommendationsDimmed"
      >
        <RecommendationPackage v-if="visibleSlots.length" :slots="visibleSlots" />

        <PersonalRecommendation
          :tax-saving="recommendation.personalTaxSavingRecommendation"
          :investment="recommendation.personalInvestmentRecommendation"
        />
      </div>

      <p class="mt-10 text-center text-[12px] leading-5 text-muted-soft">
        추천 결과는 현재 입력된 정보를 기준으로 제공되며,<br />
        실제 가입 조건과 상품 정보는 금융사 안내를 확인해 주세요.
      </p>
    </template>
  </section>
</template>
