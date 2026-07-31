<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation } from '@tanstack/vue-query'
import { ChevronLeft, LoaderCircle } from 'lucide-vue-next'
import { createInvitation } from '@/api/invitation'
import BaseButton from '@/components/ui/BaseButton.vue'
import characterExcited from '@/assets/images/characters/character-excited.png'

const router = useRouter()

const goalOptions = [
  { value: 'INVESTMENT', label: '여유 자금 투자' },
  { value: 'RETIREMENT', label: '노후 자금 마련' },
  { value: 'MARRIAGE', label: '결혼 자금 마련' },
  { value: 'HOUSING', label: '부동산 자금 마련' },
  { value: 'SHORT_TERM', label: '사용예정자금 단기운용' },
]

const loanPurposeOptions = [
  { value: 'NONE', label: '대출 계획은 없음' },
  { value: 'JEONSE', label: '전세자금 마련 목적' },
  { value: 'HOUSING', label: '주택 구입 자금 마련 목적' },
  { value: 'CAR', label: '자동차 구입 자금 마련 목적' },
  { value: 'BUSINESS', label: '사업이나 창업 자금 마련 목적' },
]

const firstGoal = ref('')
const secondGoal = ref('')
const targetAmount = ref('')
const targetPeriodMonths = ref('')
const loanPurpose = ref('')
const hasLoanWithinOneMonth = ref(null)
const formError = ref('')

const formattedTargetAmount = computed(() => {
  if (!targetAmount.value) return ''
  return Number(targetAmount.value).toLocaleString('ko-KR')
})

const periodSummary = computed(() => {
  const months = Number(targetPeriodMonths.value)
  if (!months) return ''

  const years = Math.floor(months / 12)
  const restMonths = months % 12

  if (years === 0) return `${restMonths}개월`
  if (restMonths === 0) return `${years}년`
  return `${years}년 ${restMonths}개월`
})

const questionCompletion = computed(() => [
  Boolean(firstGoal.value && secondGoal.value && firstGoal.value !== secondGoal.value),
  Number(targetAmount.value) > 0,
  Number(targetPeriodMonths.value) > 0,
  Boolean(loanPurpose.value),
  hasLoanWithinOneMonth.value !== null,
])

const completedQuestionCount = computed(
  () => questionCompletion.value.filter((isCompleted) => isCompleted).length,
)

const progressLabel = computed(
  () => `${String(completedQuestionCount.value).padStart(2, '0')} / 05`,
)

const isFormValid = computed(() => completedQuestionCount.value === questionCompletion.value.length)

const createInvitationMutation = useMutation({
  mutationFn: createInvitation,
  onSuccess: ({ inviteCode }) => {
    formError.value = ''
    router.push({
      name: 'couple-invite-created',
      query: { inviteCode },
    })
  },
  onError: (error) => {
    formError.value = error.message || '공동 설문 저장에 실패했어요.'
  },
})

const isSubmitting = computed(() => createInvitationMutation.isPending.value)

function selectedGoalRank(value) {
  if (firstGoal.value === value) return '1순위'
  if (secondGoal.value === value) return '2순위'
  return ''
}

function selectGoal(value) {
  formError.value = ''

  if (firstGoal.value === value) {
    firstGoal.value = secondGoal.value
    secondGoal.value = ''
    return
  }

  if (secondGoal.value === value) {
    secondGoal.value = ''
    return
  }

  if (!firstGoal.value) {
    firstGoal.value = value
    return
  }

  if (!secondGoal.value) {
    secondGoal.value = value
    return
  }

  secondGoal.value = value
}

function updateAmount(event) {
  targetAmount.value = event.target.value.replace(/\D/g, '').slice(0, 15)
  formError.value = ''
}

function updatePeriod(event) {
  targetPeriodMonths.value = event.target.value.replace(/\D/g, '').slice(0, 3)
  formError.value = ''
}

function selectLoanPurpose(value) {
  loanPurpose.value = value
  formError.value = ''
}

function selectLoanPlan(value) {
  hasLoanWithinOneMonth.value = value
  formError.value = ''
}

function goBack() {
  if (window.history.state?.back) router.back()
  else router.replace({ name: 'couple-start' })
}

function submitSurvey() {
  if (isSubmitting.value) return

  if (!isFormValid.value) {
    formError.value = '공동 목표를 모두 입력해 주세요.'
    return
  }

  createInvitationMutation.mutate({
    goalType1: firstGoal.value,
    goalType2: secondGoal.value,
    targetAmount: Number(targetAmount.value),
    targetPeriodMonths: Number(targetPeriodMonths.value),
    loanPurpose: loanPurpose.value,
    hasLoanWithinOneMonth: hasLoanWithinOneMonth.value,
  })
}
</script>

<template>
  <section class="mx-auto flex min-h-screen flex-col bg-canvas px-4 pt-3 pb-7">
    <header class="flex flex-none items-center">
      <button
        type="button"
        class="flex h-8 w-8 cursor-pointer items-center justify-start"
        aria-label="뒤로 가기"
        @click="goBack"
      >
        <ChevronLeft :size="20" :stroke-width="2" />
      </button>
      <h1 class="-ml-8 flex-1 text-center text-[13px] font-semibold text-ink">공동 설문</h1>
    </header>

    <div class="mt-5">
      <div class="flex items-center justify-between">
        <span class="text-brand-ink text-[10px] font-extrabold">{{ progressLabel }}</span>
      </div>
      <div class="mt-2 grid h-1 grid-cols-5 gap-1">
        <div
          v-for="(_, index) in questionCompletion"
          :key="index"
          class="h-full rounded-full transition-colors"
          :class="index < completedQuestionCount ? 'bg-brand-deep' : 'bg-line-card'"
        ></div>
      </div>
    </div>

    <main class="mt-8 flex-1">
      <div class="rounded-field flex items-center gap-3 bg-brand px-3 py-3">
        <img :src="characterExcited" alt="" class="h-12 w-12 flex-none object-contain" />
        <p class="text-[13px] leading-[1.45] font-bold text-brand-ink">
          두 사람의 함께 준비하고 싶은 목표를<br />
          알려주세요.
        </p>
      </div>

      <form class="mt-8 space-y-7" @submit.prevent="submitSurvey">
        <fieldset>
          <legend class="text-[13px] leading-[1.45] font-extrabold text-ink">
            Q1. 지금 돈을 모으는 가장 큰 이유는?<br />
            <span class="font-semibold">(1순위, 2순위 선택)</span>
          </legend>

          <div class="mt-3 space-y-2">
            <button
              v-for="option in goalOptions"
              :key="option.value"
              type="button"
              class="rounded-chip flex h-10 w-full items-center justify-between border px-3 text-left text-[13px] font-semibold transition"
              :class="
                selectedGoalRank(option.value)
                  ? 'border-good bg-mint text-ink'
                  : 'border-gray-900 bg-white text-ink'
              "
              @click="selectGoal(option.value)"
            >
              <span>{{ option.label }}</span>
              <span
                v-if="selectedGoalRank(option.value)"
                class="rounded-[4px] bg-white/70 px-1.5 py-0.5 text-[10px] font-extrabold text-good"
              >
                {{ selectedGoalRank(option.value) }}
              </span>
            </button>
          </div>
        </fieldset>

        <fieldset>
          <label for="target-amount" class="text-[13px] font-extrabold text-ink">
            Q2. 1순위 목표를 위해 필요한 금액은?
          </label>

          <div class="border-line-field rounded-field mt-3 bg-white px-3 py-3">
            <div class="flex h-11 items-center border border-line-field bg-white px-3">
              <input
                id="target-amount"
                :value="formattedTargetAmount"
                type="text"
                inputmode="numeric"
                class="min-w-0 flex-1 bg-transparent text-right text-[16px] font-semibold text-ink outline-none"
                @input="updateAmount"
              />
              <span class="ml-2 text-[13px] font-medium text-ink">원</span>
            </div>
            <div class="mt-2 flex justify-end">
              <span class="rounded-[4px] bg-mint px-2 py-1 text-[11px] font-extrabold text-good">
                {{ targetAmount ? `${Number(targetAmount) / 100000000}억원` : '금액 입력' }}
              </span>
            </div>
          </div>
        </fieldset>

        <fieldset>
          <label for="target-period" class="text-[13px] font-extrabold text-ink">
            Q3. 1순위 목표를 이루고 싶은 기간은?
          </label>

          <div class="border-line-field rounded-field mt-3 bg-white px-3 py-3">
            <div class="flex h-11 items-center border border-line-field bg-white px-3">
              <input
                id="target-period"
                :value="targetPeriodMonths"
                type="text"
                inputmode="numeric"
                class="min-w-0 flex-1 bg-transparent text-right text-[16px] font-semibold text-ink outline-none"
                @input="updatePeriod"
              />
              <span class="ml-2 text-[13px] font-medium text-ink">개월</span>
            </div>
            <div class="mt-2 flex justify-end">
              <span class="rounded-[4px] bg-mint px-2 py-1 text-[11px] font-extrabold text-good">
                {{ periodSummary || '0개월' }}
              </span>
            </div>
          </div>
        </fieldset>

        <fieldset>
          <legend class="text-[13px] font-extrabold text-ink">
            Q4. 앞으로 대출이 필요한 일이 있다면?
          </legend>

          <div class="mt-3 space-y-2">
            <button
              v-for="option in loanPurposeOptions"
              :key="option.value"
              type="button"
              class="rounded-chip flex h-10 w-full items-center border px-3 text-left text-[13px] font-semibold transition"
              :class="
                loanPurpose === option.value
                  ? 'border-good bg-mint text-ink'
                  : 'border-gray-900 bg-white text-ink'
              "
              @click="selectLoanPurpose(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </fieldset>

        <fieldset>
          <legend class="text-[13px] leading-[1.45] font-extrabold text-ink">
            Q5. 최근 1개월 이내 대출을 받았거나, 앞으로 1개월 이내 대출을 받을 예정인가요?
          </legend>

          <div class="mt-3 grid grid-cols-2 gap-3">
            <button
              type="button"
              class="rounded-chip h-12 border text-[13px] font-extrabold transition"
              :class="
                hasLoanWithinOneMonth === true
                  ? 'border-good bg-mint text-ink'
                  : 'border-line-field bg-white text-ink'
              "
              @click="selectLoanPlan(true)"
            >
              예
            </button>
            <button
              type="button"
              class="rounded-chip h-12 border text-[13px] font-extrabold transition"
              :class="
                hasLoanWithinOneMonth === false
                  ? 'border-good bg-mint text-ink'
                  : 'border-line-field bg-white text-ink'
              "
              @click="selectLoanPlan(false)"
            >
              아니요
            </button>
          </div>
        </fieldset>

        <div class="pt-2">
          <p
            v-if="formError"
            class="mb-3 min-h-4 text-center text-[12px] font-semibold text-red-500"
          >
            {{ formError }}
          </p>

          <div class="mx-auto w-full max-w-[337px]">
            <BaseButton
              class="w-full shadow-[0_8px_18px_rgba(250,230,77,0.28)]"
              :variant="isFormValid && !isSubmitting ? 'primary' : 'disabled'"
              @click="submitSurvey"
            >
              <LoaderCircle v-if="isSubmitting" :size="18" class="animate-spin" />
              <span class="text-center leading-none">
                {{ isSubmitting ? '저장 중' : '공동 목표 설정 완료하기' }}
              </span>
            </BaseButton>
          </div>

          <p class="text-muted-soft mt-4 text-center text-[10px] leading-[1.5] font-medium">
            입력한 정보는 파트너와 공유되어<br />
            금융 궁합 분석에 사용됩니다.
          </p>
        </div>
      </form>
    </main>
  </section>
</template>
