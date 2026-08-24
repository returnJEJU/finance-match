<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { LoaderCircle } from 'lucide-vue-next'

import { savePersonalSurvey } from '@/api/personalSurvey'
import BaseButton from '@/components/ui/BaseButton.vue'
import FunnelHeader from '@/components/layout/FunnelHeader.vue'
import characterExcited from '@/assets/images/characters/character-excited.png'

const router = useRouter()
const queryClient = useQueryClient()

const financialAssetRatioOptions = [
  { value: 'UNDER_10', label: '10% 이내' },
  { value: 'UNDER_30', label: '30% 이내' },
  { value: 'UNDER_50', label: '50% 이내' },
  { value: 'UNDER_80', label: '80% 이내' },
  { value: 'OVER_80', label: '80% 초과' },
]

const investmentExperienceOptions = [
  {
    value: 'LOW_RISK',
    label: '은행 예·적금, 국채, 지방채',
  },
  {
    value: 'MODERATE_LOW_RISK',
    label: '채권형 펀드, 금융채, 우량 회사채',
  },
  {
    value: 'MODERATE_RISK',
    label: '혼합형 펀드, 일반 회사채',
  },
  {
    value: 'MODERATE_HIGH_RISK',
    label: '인덱스 펀드, 저신용 회사채',
  },
  {
    value: 'HIGH_RISK',
    label: '주식형 펀드, 파생상품 펀드, 주식, 선물·옵션',
  },
]

const financialKnowledgeOptions = [
  {
    value: 'VERY_LOW',
    label: '예·적금 외 금융상품은 아직 익숙하지 않음',
  },
  {
    value: 'LOW',
    label: '주식·채권·펀드의 차이를 구분할 수 있음',
  },
  {
    value: 'MEDIUM',
    label: '주식·채권·펀드의 손실 가능성을 이해함',
  },
  {
    value: 'HIGH',
    label: '주식·채권·펀드의 구조를 구체적으로 이해함',
  },
  {
    value: 'VERY_HIGH',
    label: '대부분의 금융투자상품을 깊이 있게 이해함',
  },
]

const capitalPreservationOptions = [
  { value: 'ZERO', label: '원금 보존 추구' },
  { value: 'UNDER_10', label: '10% 이내 손실 감내 가능' },
  { value: 'UNDER_20', label: '20% 이내 손실 감내 가능' },
  { value: 'UNDER_50', label: '50% 이내 손실 감내 가능' },
  { value: 'UNDER_70', label: '70% 이내 손실 감내 가능' },
  { value: 'FULL', label: '전액손실 감내 가능' },
]

// 빈 문자열과 빈 배열로 시작하므로 처음에는 아무 선택도 되지 않는다.
const annualIncome = ref('')
const monthlyAvailableAmount = ref('')
const financialAssetRatio = ref('')
const investmentExperiences = ref([])
const financialKnowledge = ref('')
const capitalPreservationAttitude = ref('')
const formError = ref('')

function formatNumber(value) {
  if (value === '') return ''
  return Number(value).toLocaleString('ko-KR')
}

function formatKoreanAmount(value) {
  if (value === '') {
    return '금액 입력'
  }

  const amount = Number(value)

  if (!Number.isFinite(amount) || amount < 0) {
    return '금액 입력'
  }

  if (amount === 0) {
    return '0원'
  }

  const jo = Math.floor(amount / 1_000_000_000_000)
  const eok = Math.floor((amount % 1_000_000_000_000) / 100_000_000)
  const man = Math.floor((amount % 100_000_000) / 10_000)
  const won = amount % 10_000
  const parts = []

  if (jo > 0) {
    parts.push(`${jo}조`)
  }

  if (eok > 0) {
    parts.push(`${eok}억`)
  }

  if (man > 0) {
    parts.push(`${man}만`)
  }

  if (won > 0) {
    parts.push(`${won}원`)
  }

  if (won === 0) {
    return `${parts.join(' ')} 원`
  }

  return parts.join(' ')
}

const formattedAnnualIncome = computed(() => formatNumber(annualIncome.value))
const formattedMonthlyAmount = computed(() => formatNumber(monthlyAvailableAmount.value))

const annualIncomeSummary = computed(() => formatKoreanAmount(annualIncome.value))
const monthlyAmountSummary = computed(() => formatKoreanAmount(monthlyAvailableAmount.value))

const questionCompletion = computed(() => [
  annualIncome.value !== '',
  monthlyAvailableAmount.value !== '',
  Boolean(financialAssetRatio.value),
  investmentExperiences.value.length > 0,
  Boolean(financialKnowledge.value),
  Boolean(capitalPreservationAttitude.value),
])

const completedQuestionCount = computed(
  () => questionCompletion.value.filter((isCompleted) => isCompleted).length,
)

const isFormValid = computed(() => completedQuestionCount.value === questionCompletion.value.length)

const saveSurveyMutation = useMutation({
  mutationFn: savePersonalSurvey,
  onSuccess: async () => {
    formError.value = ''

    // 현재는 첫 설문 뒤 결과 페이지에 처음 진입하지만, 개발 중 남은 캐시나 향후 설문 수정 기능에서도
    // 이전 투자성향이 보이지 않도록 결과 쿼리를 무효화해 서버의 최신 계산 결과를 다시 조회하게 한다.
    await queryClient.invalidateQueries({
      queryKey: ['personal-survey-result'],
    })

    router.push({ name: 'survey-result' })
  },
  // TODO(개인설문): ApiError의 error.code에 따라 안내 문구와 후속 화면 이동을 분기한다.
  onError: (error) => {
    formError.value = error.message || '개인 설문 저장에 실패했어요.'
  },
})

const isSubmitting = computed(() => saveSurveyMutation.isPending.value)

function updateMoney(targetRef, event) {
  const limitedValue = event.target.value.replace(/\D/g, '').slice(0, 15)

  targetRef.value = limitedValue

  // 16번째 이후 숫자가 실제 입력창에 남지 않도록 표시값도 즉시 갱신한다.
  event.target.value = limitedValue ? Number(limitedValue).toLocaleString('ko-KR') : ''

  formError.value = ''
}

function updateAnnualIncome(event) {
  updateMoney(annualIncome, event)
}

function updateMonthlyAmount(event) {
  updateMoney(monthlyAvailableAmount, event)
}

function selectFinancialAssetRatio(value) {
  financialAssetRatio.value = value
  formError.value = ''
}

function selectFinancialKnowledge(value) {
  financialKnowledge.value = value
  formError.value = ''
}

function selectCapitalPreservationAttitude(value) {
  capitalPreservationAttitude.value = value
  formError.value = ''
}

function toggleInvestmentExperience(value) {
  formError.value = ''

  if (investmentExperiences.value.includes(value)) {
    investmentExperiences.value = investmentExperiences.value.filter(
      (selectedValue) => selectedValue !== value,
    )
    return
  }

  investmentExperiences.value = [...investmentExperiences.value, value]
}

function submitSurvey() {
  if (isSubmitting.value) return

  if (!isFormValid.value) {
    formError.value = '개인 설문 문항에 모두 답변해 주세요.'
    return
  }

  saveSurveyMutation.mutate({
    annualIncome: Number(annualIncome.value),
    monthlyAvailableAmount: Number(monthlyAvailableAmount.value),
    financialAssetRatio: financialAssetRatio.value,
    investmentExperiences: investmentExperiences.value,
    financialKnowledge: financialKnowledge.value,
    capitalPreservationAttitude: capitalPreservationAttitude.value,
  })
}
</script>

<template>
  <section class="mx-auto flex min-h-screen flex-col bg-canvas px-5 pb-7">
    <FunnelHeader
      :step="completedQuestionCount"
      :total="questionCompletion.length"
      title="개인 설문"
      padding-class="px-5"
      class="-mx-5"
    />

    <main class="mt-1 flex-1">
      <div class="rounded-field flex items-center gap-4 bg-brand px-4 py-4">
        <img :src="characterExcited" alt="" class="h-16 w-16 flex-none object-contain" />

        <p class="text-[14px] leading-[1.45] font-bold text-brand-ink">
          찰떡같은 금융 궁합 진단을 위해 개인 질문에 답해 주세요. 두 분의 금융 상황과 투자 가치관을
          바탕으로 금융 궁합을 분석해요.
        </p>
      </div>

      <form class="mt-7 space-y-7" @submit.prevent="submitSurvey">
        <!-- Q1 -->
        <fieldset class="min-w-0">
          <label for="annual-income" class="text-[17.5px] font-extrabold text-ink">
            <span class="text-[20px]">Q1.</span> 최근 <span class="text-good">1년</span>간 세전
            <span class="text-good">총소득</span>은 얼마인가요?
          </label>

          <div class="rounded-field mt-4 border border-ink bg-white px-5 py-5">
            <div class="flex h-12 items-center border border-line-field bg-white px-3">
              <input
                id="annual-income"
                :value="formattedAnnualIncome"
                type="text"
                inputmode="numeric"
                autocomplete="off"
                class="min-w-0 flex-1 bg-transparent text-right text-[22px] font-semibold text-ink outline-none"
                @input="updateAnnualIncome"
              />
              <span class="ml-5 text-[16px] font-medium text-ink">원</span>
            </div>

            <div class="mt-2 flex justify-end">
              <span class="rounded-[4px] bg-mint px-2 py-1 text-[16px] font-extrabold text-good">
                {{ annualIncomeSummary }}
              </span>
            </div>
          </div>
        </fieldset>

        <!-- Q2 -->
        <fieldset class="min-w-0">
          <label for="monthly-amount" class="text-[17.5px] font-extrabold text-ink">
            <span class="text-[20px]">Q2.</span> <span class="text-good">매달</span> 무리 없이
            <span class="text-good">저축</span>하거나 <span class="text-good">투자</span>할 수 있는
            금액은 얼마인가요?
          </label>

          <div class="rounded-field mt-4 border border-ink bg-white px-5 py-5">
            <div class="flex h-12 items-center border border-line-field bg-white px-3">
              <input
                id="monthly-amount"
                :value="formattedMonthlyAmount"
                type="text"
                inputmode="numeric"
                autocomplete="off"
                class="min-w-0 flex-1 bg-transparent text-right text-[22px] font-semibold text-ink outline-none"
                @input="updateMonthlyAmount"
              />
              <span class="ml-5 text-[16px] font-medium text-ink">원</span>
            </div>

            <div class="mt-2 flex justify-end">
              <span class="rounded-[4px] bg-mint px-2 py-1 text-[16px] font-extrabold text-good">
                {{ monthlyAmountSummary }}
              </span>
            </div>
          </div>
        </fieldset>

        <!-- Q3 -->
        <fieldset>
          <legend class="text-[17.5px] leading-[1.5] font-extrabold text-ink">
            <span class="text-[20px]">Q3.</span> 총 자산(부동산 등을 포함) 중
            <span class="text-good">금융자산</span>이 차지하는 <span class="text-good">비중</span>은
            어느 정도인가요?
          </legend>

          <div class="mt-3 space-y-2.5">
            <button
              v-for="option in financialAssetRatioOptions"
              :key="option.value"
              type="button"
              class="rounded-chip flex min-h-12 w-full items-center justify-center border px-4 py-3 text-center text-[16px] font-semibold transition"
              :class="
                financialAssetRatio === option.value
                  ? 'border-good bg-mint text-ink'
                  : 'border-line-card bg-white text-ink'
              "
              @click="selectFinancialAssetRatio(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </fieldset>

        <!-- Q4: 복수 선택 -->
        <fieldset>
          <legend class="text-[17.5px] leading-[1.5] font-extrabold text-ink">
            <span class="text-[20px]">Q4.</span> 지금까지 거래하거나 가입해 본
            <span class="text-good">금융상품</span>을 <span class="text-good">모두</span> 선택해
            주세요.
          </legend>
          <div class="mt-3 space-y-2.5">
            <button
              v-for="option in investmentExperienceOptions"
              :key="option.value"
              type="button"
              class="rounded-chip flex min-h-12 w-full items-center justify-center border px-4 py-3 text-center text-[16px] leading-[1.45] font-semibold transition"
              :class="
                investmentExperiences.includes(option.value)
                  ? 'border-good bg-mint text-ink'
                  : 'border-line-card bg-white text-ink'
              "
              :aria-pressed="investmentExperiences.includes(option.value)"
              @click="toggleInvestmentExperience(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </fieldset>

        <!-- Q5 -->
        <fieldset>
          <legend class="text-[17.5px] leading-[1.5] font-extrabold text-ink">
            <span class="text-[20px]">Q5.</span> 금융투자상품에 대한
            <span class="text-good">이해도</span>는 어느 정도인가요?
          </legend>

          <div class="mt-3 space-y-2.5">
            <button
              v-for="option in financialKnowledgeOptions"
              :key="option.value"
              type="button"
              class="rounded-chip flex min-h-12 w-full items-center justify-center border px-4 py-3 text-center text-[16px] leading-[1.45] font-semibold transition"
              :class="
                financialKnowledge === option.value
                  ? 'border-good bg-mint text-ink'
                  : 'border-line-card bg-white text-ink'
              "
              @click="selectFinancialKnowledge(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </fieldset>

        <!-- Q6 -->
        <fieldset>
          <legend class="text-[17.5px] leading-[1.5] font-extrabold text-ink">
            <span class="text-[20px]">Q6.</span> 현재 금융 목표를 위해 투자한다고 가정했을 때,
            <span class="text-good">원금 보존 태도</span>는 무엇인가요?
          </legend>

          <div class="mt-3 space-y-2.5">
            <button
              v-for="option in capitalPreservationOptions"
              :key="option.value"
              type="button"
              class="rounded-chip flex min-h-12 w-full items-center justify-center border px-4 py-3 text-center text-[16px] font-semibold transition"
              :class="
                capitalPreservationAttitude === option.value
                  ? 'border-good bg-mint text-ink'
                  : 'border-line-card bg-white text-ink'
              "
              @click="selectCapitalPreservationAttitude(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </fieldset>

        <div class="pt-1">
          <p v-if="formError" class="mb-3 min-h-4 text-center text-[16px] font-semibold text-warn">
            {{ formError }}
          </p>

          <BaseButton
            class="w-full text-[20px] shadow-lg"
            :class="isFormValid && !isSubmitting ? 'shadow-brand-deep/30' : 'shadow-gray-300/60'"
            :variant="isFormValid && !isSubmitting ? 'primary' : 'disabled'"
            @click="submitSurvey"
          >
            <LoaderCircle v-if="isSubmitting" :size="18" class="animate-spin" />
            <span>
              {{ isSubmitting ? '저장 중' : '결과 확인하기' }}
            </span>
          </BaseButton>
        </div>
      </form>
    </main>
  </section>
</template>
