<script setup>
import { ref, computed } from 'vue'
import {
  ChevronDown,
  ChevronUp,
  Wallet,
  BarChart3,
  Heart,
  Search,
  Building2,
} from 'lucide-vue-next'

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

// 점수 축(key)별 아이콘 — 반응형 데이터가 아니므로 컴포넌트 밖에 고정 매핑으로 둔다.
const scoreIcons = {
  asset: Wallet,
  debt: BarChart3,
  value: Heart,
  goal: Search,
  tax: Building2,
}

// 지금은 화면 확인용 목데이터.
// report API 스펙이 정해지면 이 ref 를 TanStack Query(useQuery) 호출로 교체한다.
// 응답 형태는 be/db 스키마의 compatibility_result·report 테이블 컬럼을 그대로 따른다:
//   score/maxScore ← compatibility_result.*_score (분모는 CHECK 제약 범위 30/20/25/15/10)
//   reason         ← report.*_reason
//   goal 카드의 progress 값은 report API가 계산해서 내려줄 예정(프론트 계산 없음).
const report = ref({
  couple: {
    investmentType: 'NEUTRAL',
    memberA: { name: '민수', investmentType: 'VERY_AGGRESSIVE' },
    memberB: { name: '현지', investmentType: 'STABLE' },
  },
  scores: [
    {
      key: 'asset',
      title: '금융 자산',
      score: 26,
      maxScore: 30,
      reason: '두 분의 금융 자산은 동연령대(30대) 중앙값 대비 4% 높아요.',
    },
    {
      key: 'debt',
      title: '부채',
      score: 20,
      maxScore: 20,
      reason: '두 분 모두 부채가 있어요. 하지만 부채 위험도는 민수님이 더 높아요.',
    },
    {
      key: 'value',
      title: '투자 가치관 일치도',
      score: 18,
      maxScore: 25,
      reason:
        '두 분은 금융 상품에 대한 이해도가 비슷하네요. 다만 두 분은 손실감내력 차이가 있어요.',
    },
    {
      key: 'goal',
      title: '목표 달성 가능성',
      score: 15,
      maxScore: 15,
      reason: null,
      goal: {
        shortfallLabel: '목표까지 부족한 금액',
        shortfallAmount: '-8,000만원',
        progressBarLabel: '8,000만원 부족',
        availableAssetLabel: '예상 가용자산',
        availableAsset: '1억 2,000만원',
        achievementRateLabel: '달성률',
        achievementRate: '60%',
        footnote: '목표 달성을 도와줄 상품을 추천탭에서 만나보세요.',
      },
    },
    {
      key: 'tax',
      title: '절세 활용도',
      score: 8,
      maxScore: 10,
      reason:
        '민수님은 ISA계좌를 개설하지 않아 점수가 깎이고 있어요. 추천탭에서 ISA 상품들을 만나보세요.',
    },
  ],
})

const coupleTypeLabel = computed(() => investmentTypeMeta[report.value.couple.investmentType].label)

const memberA = computed(() => ({
  ...report.value.couple.memberA,
  ...investmentTypeMeta[report.value.couple.memberA.investmentType],
}))
const memberB = computed(() => ({
  ...report.value.couple.memberB,
  ...investmentTypeMeta[report.value.couple.memberB.investmentType],
}))

// 슬라이더 위치(%) — 안정형(0%) ~ 공격투자형(100%) 5단계 중 커플 성향의 인덱스로 계산.
const sliderPosition = computed(() => {
  const index = investmentTypeOrder.indexOf(report.value.couple.investmentType)
  return (index / (investmentTypeOrder.length - 1)) * 100
})

// 카드별 펼침 상태(복수 개 동시에 펼칠 수 있음). 기본은 전부 펼친 상태.
const openKeys = ref(new Set(report.value.scores.map((card) => card.key)))

const isOpen = (key) => openKeys.value.has(key)

const toggleCard = (key) => {
  const next = new Set(openKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  openKeys.value = next
}
</script>

<template>
  <section class="px-4 pb-8 pt-4">
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
        {{ memberA.name }}님은 <span class="font-semibold text-ink">{{ memberA.label }}</span>
      </p>
      <p class="text-[12px] text-ink-sub">
        {{ memberB.name }}님은 <span class="font-semibold text-ink">{{ memberB.label }}</span>
      </p>
    </div>

    <div class="mt-2 flex items-center justify-center gap-4">
      <img :src="memberA.character" :alt="memberA.label" class="h-24 w-24 object-contain" />
      <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand">
        <Heart :size="16" fill="currentColor" class="text-ink" />
      </div>
      <img :src="memberB.character" :alt="memberB.label" class="h-24 w-24 object-contain" />
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
          v-for="card in report.scores"
          :key="card.key"
          class="rounded-card border border-line-card bg-white p-4"
        >
          <button
            type="button"
            class="flex w-full items-center justify-between"
            @click="toggleCard(card.key)"
          >
            <span class="flex items-center gap-2.5">
              <span class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand">
                <component :is="scoreIcons[card.key]" :size="16" class="text-ink" />
              </span>
              <span class="text-[14px] font-bold text-ink">{{ card.title }}</span>
            </span>
            <span class="flex items-center gap-1.5">
              <span class="text-[15px] font-bold text-good"
                >{{ card.score }}/{{ card.maxScore }}</span
              >
              <ChevronUp v-if="isOpen(card.key)" :size="16" class="text-muted" />
              <ChevronDown v-else :size="16" class="text-muted" />
            </span>
          </button>

          <div v-if="isOpen(card.key)" class="mt-3">
            <p v-if="card.reason" class="text-[12px] leading-[1.6] text-ink-sub">
              {{ card.reason }}
            </p>

            <!-- 목표 달성 가능성 카드 전용: 부족금액·진행바·가용자산·달성률 -->
            <div v-if="card.goal">
              <p class="text-[12px] font-medium text-warn">{{ card.goal.shortfallLabel }}</p>
              <p class="mt-1 text-[26px] font-extrabold text-warn">
                {{ card.goal.shortfallAmount }}
              </p>

              <div class="mt-3 flex h-10 items-center justify-end rounded-full bg-brand px-4">
                <span class="text-[12px] font-semibold text-ink">{{
                  card.goal.progressBarLabel
                }}</span>
              </div>

              <div class="mt-4 flex items-center justify-between">
                <div>
                  <p class="text-[11px] text-muted">{{ card.goal.availableAssetLabel }}</p>
                  <p class="mt-1 text-[16px] font-bold text-ink">{{ card.goal.availableAsset }}</p>
                </div>
                <div class="text-right">
                  <p class="text-[11px] text-muted">{{ card.goal.achievementRateLabel }}</p>
                  <p class="mt-1 text-[16px] font-bold text-good">
                    {{ card.goal.achievementRate }}
                  </p>
                </div>
              </div>

              <p class="mt-3 text-[12px] leading-[1.6] text-ink-sub">{{ card.goal.footnote }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
