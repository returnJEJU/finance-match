<script setup>
import { ref, computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import {
  ChevronDown,
  ChevronUp,
  Wallet,
  BarChart3,
  Heart,
  Search,
  Building2,
} from 'lucide-vue-next'

import { getReport } from '@/api/report'

import typeStable from '@/assets/images/characters/types/type-stable.png'
import typeStabilitySeeking from '@/assets/images/characters/types/type-stability-seeking.png'
import typeRiskNeutral from '@/assets/images/characters/types/type-risk-neutral.png'
import typeActive from '@/assets/images/characters/types/type-active.png'
import typeAggressive from '@/assets/images/characters/types/type-aggressive.png'

const investmentTypeMeta = {
  STABLE: { label: '안정형', character: typeStable },
  STABLE_SEEKING: { label: '안정추구형', character: typeStabilitySeeking },
  NEUTRAL: { label: '위험중립형', character: typeRiskNeutral },
  AGGRESSIVE: { label: '적극투자형', character: typeActive },
  VERY_AGGRESSIVE: { label: '공격투자형', character: typeAggressive },
}

// 커플 공통 성향(investmentProfile.we)은 개인 성향과 다른 값이다 — 두 사람의 개인 성향 단계
// 차이(0~4)를 나타내는 CoupleInvestmentType(DIFF_0~DIFF_4, 백엔드 CoupleInvestmentTypeCalculator)다.
// TEMP: 실제 디자인·문구가 아직 기획되지 않아 임시 라벨로 연결해둔다. 확정되면 여기만 교체하면 된다.
const coupleInvestmentTypeMeta = {
  DIFF_0: { label: '찰떡궁합형' },
  DIFF_1: { label: '비슷한 성향형' },
  DIFF_2: { label: '적당히 다른 성향형' },
  DIFF_3: { label: '많이 다른 성향형' },
  DIFF_4: { label: '정반대 성향형' },
}

// 점수 축(key)별 아이콘 — report API 의 scoreAxes[].key 와 매핑한다.
const scoreIcons = {
  ASSET_STABILITY: Wallet,
  DEBT_REPAYMENT: BarChart3,
  FINANCIAL_VALUE: Heart,
  GOAL_FEASIBILITY: Search,
  TAX_STRATEGY: Building2,
}

const {
  data: report,
  isLoading,
  isError,
  error,
} = useQuery({
  queryKey: ['report'],
  queryFn: getReport,
})

// 리포트가 아직 없는 건(파트너 설문 미완료·계산 전) 진짜 오류가 아니라 "준비 중" 상태다.
// ReportService 가 이때 NOT_FOUND 로 응답한다(GET /v1/members/me/report).
const isNotReady = computed(() => error.value?.code === 'NOT_FOUND')

// 목표 달성 가능성 카드의 진행 현황 — report API 의 goalProgress 를 그대로 쓴다.
// achieved=false(부족)면 마젠타(warn 토큰), true(초과)면 초록(good 토큰)으로 갈린다.
const goalProgress = computed(() => report.value.goalProgress)

const goalDifferenceLabel = computed(() =>
  goalProgress.value?.achieved ? '목표를 넘어선 예상액' : '목표까지 부족한 금액',
)

const coupleTypeLabel = computed(
  () => coupleInvestmentTypeMeta[report.value.investmentProfile.we].label,
)

const me = computed(() => ({
  name: report.value.name,
  ...investmentTypeMeta[report.value.investmentProfile.me],
}))
const partner = computed(() => ({
  name: report.value.partnerName,
  ...investmentTypeMeta[report.value.investmentProfile.you],
}))

// 슬라이더 위치(%) — DIFF_0(0%, 성향 거의 같음) ~ DIFF_4(100%, 성향 정반대) 5단계.
// TEMP: "안정형~공격형" 축 라벨은 원래 개인 성향 스펙트럼용이라 의미가 완전히 들어맞진 않지만,
// 실제 디자인이 나오기 전까지 슬라이더가 최소한 정상 범위(0~100%) 안에서 움직이게만 해둔다.
const coupleInvestmentTypeOrder = ['DIFF_0', 'DIFF_1', 'DIFF_2', 'DIFF_3', 'DIFF_4']
const sliderPosition = computed(() => {
  const index = coupleInvestmentTypeOrder.indexOf(report.value.investmentProfile.we)
  return (index / (coupleInvestmentTypeOrder.length - 1)) * 100
})

// 카드별 펼침 상태(복수 개 동시에 펼칠 수 있음). 기본은 전부 펼친 상태라, "닫힌 것만" 기록한다
// (scoreAxes 가 비동기로 나중에 도착해도 미리 키를 알 필요가 없다).
const closedKeys = ref(new Set())

const isOpen = (key) => !closedKeys.value.has(key)

const roundScore = (score) => Math.round(score)

const toggleCard = (key) => {
  const next = new Set(closedKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  closedKeys.value = next
}
</script>

<template>
  <section class="px-4 pb-8 pt-4">
    <p v-if="isLoading" class="py-10 text-center text-[13px] text-muted">불러오는 중...</p>
    <p v-else-if="isNotReady" class="py-10 text-center text-[13px] text-muted">
      리포트를 준비하고 있어요. 두 분의 설문이 모두 끝나면 확인할 수 있어요.
    </p>
    <p v-else-if="isError" class="py-10 text-center text-[13px] text-warn">
      리포트를 불러오지 못했어요.
    </p>

    <template v-else>
      <!-- 커플 성향 히어로 -->
      <div class="text-center">
        <p class="text-[13px] font-medium text-ink-sub">우리 커플의 금융 스타일은</p>
        <p class="mt-1 text-[26px] font-extrabold">
          <span class="text-good">{{ coupleTypeLabel }}</span>
          <span class="text-ink">입니다.</span>
        </p>
      </div>

      <div class="mt-6 flex items-center justify-between px-2">
        <p class="text-[12px] text-ink-sub">
          {{ me.name }}님은 <span class="font-semibold text-ink">{{ me.label }}</span>
        </p>
        <p class="text-[12px] text-ink-sub">
          {{ partner.name }}님은 <span class="font-semibold text-ink">{{ partner.label }}</span>
        </p>
      </div>

      <div class="mt-2 flex items-center justify-center gap-4">
        <img :src="me.character" :alt="me.label" class="h-24 w-24 object-contain" />
        <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand">
          <Heart :size="16" fill="currentColor" class="text-ink" />
        </div>
        <img :src="partner.character" :alt="partner.label" class="h-24 w-24 object-contain" />
      </div>

      <!-- 안정형 ~ 공격형 슬라이더 -->
      <div class="mt-6 flex items-center gap-3">
        <span class="shrink-0 text-[11px] text-muted">안정형</span>
        <div class="relative h-1 flex-1 rounded-full bg-line-card">
          <span
            class="absolute top-1/2 h-3 w-3 -translate-y-1/2 rounded-full border-2 border-ink bg-brand-deep"
            :style="{ left: `calc(${sliderPosition}% - 6px)` }"
          />
        </div>
        <span class="shrink-0 text-[11px] text-muted">공격형</span>
      </div>

      <!-- 점수 상세 분석 -->
      <div class="mt-8">
        <h2 class="text-[15px] font-bold text-ink">점수 상세 분석</h2>
        <p class="mt-1 text-[12px] text-muted">항목을 눌러 각 점수의 근거를 확인하세요.</p>

        <div class="mt-4 space-y-3">
          <div
            v-for="axis in report.scoreAxes"
            :key="axis.key"
            class="rounded-card border border-line-card bg-white p-4"
          >
            <button
              type="button"
              class="flex w-full items-center justify-between"
              @click="toggleCard(axis.key)"
            >
              <span class="flex items-center gap-2.5">
                <span
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand"
                >
                  <component :is="scoreIcons[axis.key]" :size="16" class="text-ink" />
                </span>
                <span class="text-[14px] font-bold text-ink">{{ axis.name }}</span>
              </span>
              <span class="flex items-center gap-1.5">
                <span class="text-[15px] font-bold text-good"
                  >{{ roundScore(axis.score) }}/{{ axis.maxScore }}</span
                >
                <ChevronUp v-if="isOpen(axis.key)" :size="16" class="text-muted" />
                <ChevronDown v-else :size="16" class="text-muted" />
              </span>
            </button>

            <div v-if="isOpen(axis.key)" class="mt-3">
              <p class="text-[12px] leading-[1.6] text-ink-sub">{{ axis.reason }}</p>

              <!-- 목표 달성 가능성 카드 전용: 초과/부족 진행 현황 -->
              <div v-if="axis.key === 'GOAL_FEASIBILITY' && goalProgress" class="mt-3">
                <p
                  class="text-[12px] font-medium"
                  :class="goalProgress.achieved ? 'text-good' : 'text-warn'"
                >
                  {{ goalDifferenceLabel }}
                </p>
                <p
                  class="mt-1 text-[26px] font-extrabold"
                  :class="goalProgress.achieved ? 'text-good' : 'text-warn'"
                >
                  {{ goalProgress.amountLabel }}
                </p>

                <div class="relative mt-3 h-10 overflow-hidden rounded-full bg-line-card">
                  <div
                    class="absolute inset-y-0 left-0 rounded-full"
                    :class="goalProgress.achieved ? 'bg-good' : 'bg-warn'"
                    :style="{
                      width: goalProgress.achieved ? '100%' : goalProgress.achievementRate,
                    }"
                  />
                  <div class="relative flex h-full items-center justify-end px-4">
                    <span
                      class="text-[12px] font-semibold"
                      :class="goalProgress.achieved ? 'text-white' : 'text-ink'"
                      >{{ goalProgress.barLabel }}</span
                    >
                  </div>
                </div>

                <div class="mt-4 flex items-center justify-between">
                  <div>
                    <p class="text-[11px] text-muted">예상 가용자산</p>
                    <p class="mt-1 text-[16px] font-bold text-ink">
                      {{ goalProgress.availableAsset }}
                    </p>
                  </div>
                  <div class="text-right">
                    <p class="text-[11px] text-muted">달성률</p>
                    <p
                      class="mt-1 text-[16px] font-bold"
                      :class="goalProgress.achieved ? 'text-good' : 'text-warn'"
                    >
                      {{ goalProgress.achievementRate }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
