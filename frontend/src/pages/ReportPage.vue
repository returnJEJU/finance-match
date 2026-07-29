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

// 투자성향 코드값 순서(안정형 → 공격투자형) — 슬라이더 위치·캐릭터 이미지 매핑에 공용으로 쓴다.
// (db/README.md 가정 코드값: STABLE·STABLE_SEEKING·NEUTRAL·AGGRESSIVE·VERY_AGGRESSIVE)
const investmentTypeOrder = ['STABLE', 'STABLE_SEEKING', 'NEUTRAL', 'AGGRESSIVE', 'VERY_AGGRESSIVE']

const investmentTypeMeta = {
  STABLE: { label: '안정형', character: typeStable },
  STABLE_SEEKING: { label: '안정추구형', character: typeStabilitySeeking },
  NEUTRAL: { label: '위험중립형', character: typeRiskNeutral },
  AGGRESSIVE: { label: '적극투자형', character: typeActive },
  VERY_AGGRESSIVE: { label: '공격투자형', character: typeAggressive },
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
} = useQuery({
  queryKey: ['report'],
  queryFn: getReport,
})

const coupleTypeLabel = computed(() => investmentTypeMeta[report.value.investmentProfile.we].label)

const me = computed(() => ({
  name: report.value.name,
  ...investmentTypeMeta[report.value.investmentProfile.me],
}))
const partner = computed(() => ({
  name: report.value.partnerName,
  ...investmentTypeMeta[report.value.investmentProfile.you],
}))

// 슬라이더 위치(%) — 안정형(0%) ~ 공격투자형(100%) 5단계 중 커플 성향의 인덱스로 계산.
const sliderPosition = computed(() => {
  const index = investmentTypeOrder.indexOf(report.value.investmentProfile.we)
  return (index / (investmentTypeOrder.length - 1)) * 100
})

// 카드별 펼침 상태(복수 개 동시에 펼칠 수 있음). 기본은 전부 펼친 상태라, "닫힌 것만" 기록한다
// (scoreAxes 가 비동기로 나중에 도착해도 미리 키를 알 필요가 없다).
const closedKeys = ref(new Set())

const isOpen = (key) => !closedKeys.value.has(key)

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
                  >{{ axis.score }}/{{ axis.maxScore }}</span
                >
                <ChevronUp v-if="isOpen(axis.key)" :size="16" class="text-muted" />
                <ChevronDown v-else :size="16" class="text-muted" />
              </span>
            </button>

            <p v-if="isOpen(axis.key)" class="mt-3 text-[12px] leading-[1.6] text-ink-sub">
              {{ axis.reason }}
            </p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
