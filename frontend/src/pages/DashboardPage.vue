<script setup>
import { computed, markRaw, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  BadgeDollarSign,
  CircleHelp,
  HeartHandshake,
  Landmark,
  Target,
} from 'lucide-vue-next'

import { getCompatibility } from '@/api/match'

const router = useRouter()

const result = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const openedHelpKey = ref(null)

const totalScore = computed(() => Math.round(result.value?.totalScore ?? 0))

// 원형 그래프에서 사용할 수 있도록 총점을 0~100으로 제한
const totalProgress = computed(() => {
  return Math.min(Math.max(totalScore.value, 0), 100)
})

// 총점에 따른 안내 문구
const compatibilityMessage = computed(() => {
  const score = totalScore.value

  if (score >= 90) {
    return {
      first: '환상의 찰떡귱합! 우리 부부는',
      second: '금융 호흡까지 ',
      accent: '완벽해요.',
    }
  }

  if (score >= 80) {
    return {
      first: '찰떡귱합! 우리 부부의',
      second: '금융 조화는 ',
      accent: '완벽해요.',
    }
  }

  if (score >= 60) {
    return {
      first: '제법 잘 맞는 우리 부부!',
      second: '조금만 더 맞추면 더욱 든든해져요.',
      accent: '',
    }
  }

  return {
    first: '우리, 이제 맞춰가는 중!',
    second: '부부의 금융 습관을 하나씩 맞춰봐요.',
    accent: '',
  }
})

// 항목별 점수
const scoreCards = computed(() => {
  if (!result.value) {
    return []
  }

  return [
    {
      key: 'asset',
      title: '금융 자산',
      description: '또래 평균과 비교해 현재 금융자산 수준을 평가했어요.',
      score: Math.round(result.value.assetStabilityScore),
      maxScore: 30,
      icon: markRaw(BadgeDollarSign),
      iconClass: 'bg-brand-soft text-brand-ink',
      progressClass: 'bg-[#707500]',
    },
    {
      key: 'debt',
      title: '부채',
      description: '소득 대비 상환 능력과 자산 대비 부채 부담을 평가했어요.',
      score: Math.round(result.value.debtRepaymentScore),
      maxScore: 20,
      icon: markRaw(Landmark),
      iconClass: 'bg-[#fff3e9] text-[#a94700]',
      progressClass: 'bg-[#707500]',
    },
    {
      key: 'value',
      title: '투자 가치관 일치도',
      description: '투자 성향과 위험 선호가 얼마나 비슷한지 분석했어요.',
      score: Math.round(result.value.financialValueScore),
      maxScore: 25,
      icon: markRaw(HeartHandshake),
      iconClass: 'bg-[#f5f5dc] text-[#777000]',
      progressClass: 'bg-[#707500]',
    },
    {
      key: 'goal',
      title: '목표달성률',
      description: '현재 자산과 저축 계획으로 목표 달성 가능성을 계산했어요.',
      score: Math.round(result.value.goalFeasibilityScore),
      maxScore: 15,
      icon: markRaw(Target),
      iconClass: 'bg-[#eef7e9] text-[#46763b]',
      progressClass: 'bg-[#707500]',
    },
    {
      key: 'tax',
      title: '절세 활용도',
      description: '연금계좌와 ISA의 절세 혜택 활용도를 평가했어요.',
      score: Math.round(result.value.taxStrategyScore),
      maxScore: 10,
      icon: markRaw(BadgeDollarSign),
      iconClass: 'bg-[#f5f0ff] text-[#66528c]',
      progressClass: 'bg-[#707500]',
      calculated: result.value.taxStrategyCalculated,
    },
  ]
})

// 항목별 진행 바 너비 계산
const getProgressWidth = (card) => {
  if (card.key === 'tax' && !card.calculated) {
    return '0%'
  }

  const percentage = (card.score / card.maxScore) * 100
  const limitedPercentage = Math.min(Math.max(percentage, 0), 100)

  return `${limitedPercentage}%`
}

// 대시보드 데이터 조회
const loadDashboard = async () => {
  loading.value = true
  errorMessage.value = ''
  openedHelpKey.value = null

  try {
    result.value = await getCompatibility()
  } catch (error) {
    if (error?.status === 404) {
      router.replace('/dashboard/waiting')
      return
    }

    errorMessage.value = error?.message || '금융 궁합도 결과를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// 도움말 열기/닫기
const toggleHelp = (key) => {
  openedHelpKey.value = openedHelpKey.value === key ? null : key
}

// 상세 리포트로 이동
const moveToReport = () => {
  router.push('/report')
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <section class="h-[calc(100dvh-108px)] overflow-y-auto px-[18px] pt-3 pb-6">
    <!-- 로딩 -->
    <div v-if="loading" class="flex h-full items-center justify-center">
      <div class="h-9 w-9 animate-spin rounded-full border-4 border-line-card border-t-brand-ink" />
    </div>

    <!-- 오류 -->
    <div
      v-else-if="errorMessage"
      class="flex h-full flex-col items-center justify-center px-6 text-center"
    >
      <p class="text-[16px] font-semibold text-ink">
        {{ errorMessage }}
      </p>

      <button
        type="button"
        class="mt-5 rounded-full bg-brand px-7 py-3 text-[15px] font-bold text-ink"
        @click="loadDashboard"
      >
        다시 시도하기
      </button>
    </div>

    <!-- 완성된 대시보드 -->
    <div v-else-if="result" class="flex min-h-full flex-col">
      <!-- 궁합 안내 문구 -->
      <div class="shrink-0 px-5 pt-2">
        <p class="text-center text-[20px] leading-[1.35] font-extrabold tracking-[-0.7px] text-ink">
          {{ compatibilityMessage.first }}<br />

          {{ compatibilityMessage.second }}

          <span
            v-if="compatibilityMessage.accent"
            class="border-b-2 border-brand-deep pb-0.5 text-brand-ink"
          >
            {{ compatibilityMessage.accent }}
          </span>
        </p>
      </div>

      <!-- 총점 원형 그래프 -->
      <div class="flex shrink-0 justify-center pt-4">
        <div class="relative h-[190px] w-[190px]">
          <!-- 점수 원 -->
          <div
            class="absolute inset-0 rounded-full p-[14px]"
            :style="{
              background: `conic-gradient(
                from 0deg,
                #fae64d 0% ${totalProgress}%,
                #e5f4ee ${totalProgress}% 100%
              )`,
            }"
          >
            <!-- 원 내부 -->
            <div
              class="flex h-full w-full flex-col items-center justify-center rounded-full bg-canvas"
            >
              <span class="text-[68px] leading-none font-medium tracking-[-4px] text-[#275e50]">
                {{ totalScore }}
              </span>

              <span class="mt-3 text-[12px] text-brand-deep">♥</span>
            </div>
          </div>

          <!-- 레몬 장식 -->
          <div class="absolute -top-3 left-1/2 z-10 -translate-x-1/2 text-[23px]">🍋</div>

          <!-- 주변 하트 장식 -->
          <span class="absolute top-9 -left-7 text-[14px] text-brand">♥</span>
          <span class="absolute top-[92px] -right-7 text-[17px] text-brand">♥</span>
          <span class="absolute right-2 -bottom-1 text-[12px] text-brand">♥</span>
          <span class="absolute bottom-3 left-1 text-[11px] text-brand">♥</span>
        </div>
      </div>

      <!-- 항목별 점수 -->
      <div class="mx-auto mt-4 flex w-[92%] shrink-0 flex-col gap-[14px]">
        <article v-for="card in scoreCards" :key="card.key" class="relative">
          <!-- 항목 이름과 점수 -->
          <div class="flex items-center justify-between">
            <div class="flex min-w-0 items-center gap-2">
              <!-- 아이콘 -->
              <div
                class="flex h-6 w-6 shrink-0 items-center justify-center rounded-[7px]"
                :class="card.iconClass"
              >
                <component :is="card.icon" :size="14" :stroke-width="2" />
              </div>

              <!-- 항목 이름 -->
              <span class="text-[13px] font-semibold text-ink">
                {{ card.title }}
              </span>

              <!-- 도움말 버튼 -->
              <button
                type="button"
                class="shrink-0 text-[#c8c3ad]"
                :aria-label="`${card.title} 도움말`"
                :aria-expanded="openedHelpKey === card.key"
                @click="toggleHelp(card.key)"
              >
                <CircleHelp :size="14" :stroke-width="2" />
              </button>
            </div>

            <!-- 점수 -->
            <p
              v-if="card.key !== 'tax' || card.calculated"
              class="shrink-0 text-[12px] font-bold text-ink"
            >
              <span class="text-[#777000]">{{ card.score }}점</span>
              / {{ card.maxScore }}점
            </p>

            <!-- 절세 평가 제외 -->
            <p v-else class="shrink-0 text-[12px] font-bold text-muted">평가 제외</p>
          </div>

          <!-- 진행 바 -->
          <div class="mt-2 h-[6px] overflow-hidden rounded-full bg-[#e4e4e4]">
            <div
              class="h-full rounded-full transition-all duration-500"
              :class="card.progressClass"
              :style="{ width: getProgressWidth(card) }"
            />
          </div>

          <!-- 도움말 말풍선 -->
          <div
            v-if="openedHelpKey === card.key"
            role="tooltip"
            class="absolute top-7 left-7 z-50 max-w-[300px] rounded-[10px] bg-[#d5fae7] px-3 py-2 text-[11px] leading-4 font-medium text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
          >
            {{ card.description }}
          </div>
        </article>
      </div>

      <!-- 상세 리포트 버튼 -->
      <button
        type="button"
        class="mx-auto mt-8 mb-3 flex h-[52px] w-[92%] shrink-0 items-center justify-center gap-2 rounded-full bg-brand text-[16px] font-bold text-ink shadow-[0_6px_14px_rgba(250,230,77,0.2)] transition active:scale-[0.98]"
        @click="moveToReport"
      >
        상세 리포트 보러가기
        <ArrowRight :size="22" :stroke-width="2.2" />
      </button>
    </div>
  </section>
</template>
