<script setup>
import { computed } from 'vue'
import { ClipboardList } from 'lucide-vue-next'

const props = defineProps({
  survey: { type: Object, default: null },
})

const goalTypeLabels = {
  INVESTMENT: '여유 자금 투자',
  RETIREMENT: '노후 자금 마련',
  MARRIAGE: '결혼 자금 마련',
  HOUSING: '부동산 자금 마련',
  SHORT_TERM: '단기 자금 운용',
}

const loanPurposeLabels = {
  NONE: '계획 없음',
  JEONSE: '전세 자금',
  HOUSING: '주택 구입',
  CAR: '자동차 구입',
  BUSINESS: '사업/창업',
}

const goalSummary = computed(() => ({
  primaryGoal: goalTypeLabels[props.survey?.goalType1] || '-',
  secondaryGoal: goalTypeLabels[props.survey?.goalType2] || '-',
  targetAmount: formatKoreanAmount(props.survey?.targetAmount),
  targetPeriod: formatTargetPeriod(props.survey?.targetPeriodMonths) || '-',
  loanPurpose: loanPurposeLabels[props.survey?.loanPurpose] || '-',
}))

function formatKoreanAmount(value) {
  if (value === '') return '금액 입력'

  const amount = Number(value)

  if (!Number.isFinite(amount) || amount < 0) return '금액 입력'
  if (amount === 0) return '0원'

  const jo = Math.floor(amount / 1_000_000_000_000)
  const eok = Math.floor((amount % 1_000_000_000_000) / 100_000_000)
  const man = Math.floor((amount % 100_000_000) / 10_000)
  const won = amount % 10_000
  const parts = []

  if (jo > 0) parts.push(`${jo}조`)
  if (eok > 0) parts.push(`${eok}억`)
  if (man > 0) parts.push(`${man}만`)
  if (won > 0) parts.push(`${won}원`)

  if (won === 0) return `${parts.join(' ')} 원`
  return parts.join(' ')
}

function formatTargetPeriod(value) {
  const months = Number(value)
  if (!months) return ''

  const years = Math.floor(months / 12)
  const restMonths = months % 12

  if (years === 0) return `${restMonths}개월`
  if (restMonths === 0) return `${years}년`
  return `${years}년 ${restMonths}개월`
}
</script>

<template>
  <section class="rounded-card border-line-card mt-7 border bg-white px-5 py-5 shadow-sm">
    <h2 class="flex items-center gap-2 text-[20px] font-extrabold text-ink">
      <ClipboardList :size="18" class="text-brand-ink" />
      우리의 공동 목표 요약
    </h2>

    <div class="mt-4">
      <div class="rounded-field border border-good bg-mint px-4 py-4">
        <div class="flex items-center gap-2">
          <p class="text-[16px] font-bold text-muted">목표</p>
          <span
            class="inline-flex rounded-full bg-good px-2.5 py-1 text-[16px] font-extrabold text-white"
          >
            1순위
          </span>
        </div>

        <p class="mt-3 break-keep text-[18px] leading-[1.35] font-extrabold text-ink">
          {{ goalSummary.primaryGoal }}
        </p>

        <div class="border-good/30 mt-4 grid grid-cols-2 gap-3 border-t pt-4">
          <div>
            <p class="text-[16px] font-bold text-muted">목표 금액</p>
            <p class="mt-1.5 break-keep text-[18px] font-extrabold text-ink">
              {{ goalSummary.targetAmount }}
            </p>
          </div>

          <div>
            <p class="text-[16px] font-bold text-muted">목표 기간</p>
            <p class="mt-1.5 break-keep text-[18px] font-extrabold text-ink">
              {{ goalSummary.targetPeriod }}
            </p>
          </div>
        </div>
      </div>

      <div class="mt-3 grid grid-cols-2 gap-3">
        <div class="rounded-field bg-surface-muted px-4 py-4">
          <div class="flex h-7 items-center gap-2">
            <p class="text-[16px] font-bold text-muted">목표</p>
            <span
              class="border-line-card inline-flex rounded-full border bg-white px-2.5 py-1 text-[16px] font-extrabold text-muted"
            >
              2순위
            </span>
          </div>
          <p class="mt-3 break-keep text-[18px] leading-[1.35] font-extrabold text-ink">
            {{ goalSummary.secondaryGoal }}
          </p>
        </div>

        <div class="rounded-field bg-surface-muted px-4 py-4">
          <div class="flex h-7 items-center">
            <p class="text-[16px] font-bold text-muted">대출 계획</p>
          </div>
          <p class="mt-3 break-keep text-[18px] leading-[1.35] font-extrabold text-ink">
            {{ goalSummary.loanPurpose }}
          </p>
        </div>
      </div>
    </div>
  </section>
</template>
