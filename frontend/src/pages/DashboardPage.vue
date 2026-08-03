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

// 점수를 0~100 범위로 제한하고 원형 그래프의 노란색 비율로 사용
const totalProgress = computed(() => Math.min(Math.max(totalScore.value, 0), 100))

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

const scoreCards = computed(() => {
  if (!result.value) {
    return []
  }

  return [
    {
      key: 'asset',
      title: '금융자산',
      description: '또래 평균과 비교해 현재 금융자산 수준을 평가했어요.',
      score: Math.round(result.value.assetStabilityScore),
      maxScore: 30,
      icon: markRaw(BadgeDollarSign),
      iconClass: 'bg-brand-soft text-brand-ink',
      fullWidth: false,
    },
    {
      key: 'debt',
      title: '부채 관리',
      description: '소득 대비 상환 능력과 자산 대비 부채 부담을 평가했어요.',
      score: Math.round(result.value.debtRepaymentScore),
      maxScore: 20,
      icon: markRaw(Landmark),
      iconClass: 'bg-[#fff3e9] text-[#a94700]',
      fullWidth: false,
    },
    {
      key: 'value',
      title: '투자 가치관 일치도',
      description: '투자 성향과 위험 선호가 얼마나 비슷한지 분석했어요.',
      score: Math.round(result.value.financialValueScore),
      maxScore: 25,
      icon: markRaw(HeartHandshake),
      iconClass: 'bg-[#f5f5dc] text-[#777000]',
      fullWidth: true,
    },
    {
      key: 'goal',
      title: '목표 달성률',
      description: '현재 자산과 저축 계획으로 목표 달성 가능성을 계산했어요.',
      score: Math.round(result.value.goalFeasibilityScore),
      maxScore: 15,
      icon: markRaw(Target),
      iconClass: 'bg-[#eef7e9] text-[#46763b]',
      fullWidth: false,
      showProgress: true,
    },
    {
      key: 'tax',
      title: '절세 활용도',
      description: '연금계좌와 ISA의 절세 혜택 활용도를 평가했어요.',
      score: Math.round(result.value.taxStrategyScore),
      maxScore: 10,
      icon: markRaw(BadgeDollarSign),
      iconClass: 'bg-[#f5f0ff] text-[#66528c]',
      fullWidth: false,
      calculated: result.value.taxStrategyCalculated,
    },
  ]
})

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

const toggleHelp = (key) => {
  openedHelpKey.value = openedHelpKey.value === key ? null : key
}

const moveToReport = () => {
  router.push('/report')
}

onMounted(() => {
  loadDashboard()
})
</script>

<template>
  <section class="h-[calc(100dvh-108px)] overflow-hidden px-[18px] pt-4 pb-3">
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
    <div v-else-if="result" class="flex h-full flex-col">
      <!-- 총점 영역 -->
      <div class="flex shrink-0 flex-col items-center">
        <div class="relative mt-3 h-[190px] w-[190px]">
          <!-- 점수 원 -->
          <div
            class="absolute inset-0 rounded-full p-[13px]"
            :style="{
              background: `conic-gradient(
                from 0deg,
                #fae64d 0% ${totalProgress}%,
                #e5f4ee ${totalProgress}% 100%
              )`,
            }"
          >
            <div
              class="flex h-full w-full flex-col items-center justify-center rounded-full bg-canvas"
            >
              <span class="text-[62px] leading-none font-bold tracking-[-3px] text-[#275e50]">
                {{ totalScore }}
              </span>

              <span class="mt-2 text-[13px] text-brand-deep">♥</span>
            </div>
          </div>

          <!-- 레몬 장식 -->
          <div class="absolute -top-3 left-1/2 z-10 -translate-x-1/2 text-[24px]">🍋</div>
        </div>

        <!-- 궁합 문구 -->
        <div class="mt-4 w-full px-3">
          <p
            class="text-center text-[20px] leading-[1.35] font-extrabold tracking-[-0.7px] text-ink"
          >
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
      </div>

      <!-- 항목별 점수 -->
      <div class="mt-5 grid shrink-0 grid-cols-2 gap-2.5">
        <article
          v-for="card in scoreCards"
          :key="card.key"
          class="relative rounded-[15px] border border-line-soft bg-white px-3.5 py-3 shadow-[0_2px_7px_rgba(0,0,0,0.035)]"
          :class="card.fullWidth ? 'col-span-2' : ''"
        >
          <!-- 전체 너비 카드 -->
          <div v-if="card.fullWidth" class="flex h-[47px] items-center gap-2.5">
            <div
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-[10px]"
              :class="card.iconClass"
            >
              <component :is="card.icon" :size="21" :stroke-width="2" />
            </div>

            <div>
              <p class="text-[13px] leading-none text-ink-sub">
                {{ card.title }}
              </p>

              <p class="mt-1.5 text-[19px] leading-none font-bold text-ink">
                {{ card.score }}점 / {{ card.maxScore }}점
              </p>
            </div>

            <!-- 전체 너비 카드 도움말 버튼 -->
            <button
              type="button"
              class="absolute top-3 right-3 z-40 block text-[#d2ccb5]"
              :aria-label="`${card.title} 도움말`"
              :aria-expanded="openedHelpKey === card.key"
              @click="toggleHelp(card.key)"
            >
              <CircleHelp :size="15" :stroke-width="2" />
            </button>
          </div>

          <!-- 두 칸 카드 -->
          <template v-else>
            <div class="flex items-start justify-between">
              <div
                class="flex h-8 w-8 items-center justify-center rounded-[9px]"
                :class="card.iconClass"
              >
                <component :is="card.icon" :size="19" :stroke-width="2" />
              </div>

              <!-- 두 칸 카드 도움말 버튼 -->
              <button
                type="button"
                class="relative z-40 block text-[#d2ccb5]"
                :aria-label="`${card.title} 도움말`"
                :aria-expanded="openedHelpKey === card.key"
                @click="toggleHelp(card.key)"
              >
                <CircleHelp :size="15" :stroke-width="2" />
              </button>
            </div>

            <p class="mt-2 text-[13px] leading-none text-ink-sub">
              {{ card.title }}
            </p>

            <p
              v-if="card.key !== 'tax' || card.calculated"
              class="mt-1.5 whitespace-nowrap text-[18px] leading-none font-bold text-ink"
            >
              {{ card.score }}점 / {{ card.maxScore }}점
            </p>

            <p v-else class="mt-1.5 text-[17px] leading-none font-bold text-muted">평가 제외</p>

            <!-- 목표 달성률 진행바 -->
            <div
              v-if="card.showProgress"
              class="mt-2.5 h-[5px] overflow-hidden rounded-full bg-line-card"
            >
              <div
                class="h-full rounded-full bg-brand-ink"
                :style="{
                  width: `${(card.score / card.maxScore) * 100}%`,
                }"
              />
            </div>
          </template>

          <!-- 공통 도움말 말풍선 -->
          <div
            v-if="openedHelpKey === card.key"
            role="tooltip"
            class="absolute top-9 z-50 whitespace-nowrap rounded-[12px] bg-[#d5fae7] px-3 py-3 text-left text-[11px] leading-none font-medium tracking-[-0.3px] text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
            :class="{
              'left-0 w-full': card.fullWidth,
              'left-0 w-[calc(200%+10px)]':
                !card.fullWidth && (card.key === 'asset' || card.key === 'goal'),
              'right-0 w-[calc(200%+10px)]':
                !card.fullWidth && (card.key === 'debt' || card.key === 'tax'),
            }"
          >
            <!-- 왼쪽 카드 말풍선 꼬리 -->
            <span
              v-if="!card.fullWidth && (card.key === 'asset' || card.key === 'goal')"
              class="absolute -top-1.5 left-[calc(50%-20px)] h-3 w-3 rotate-45 bg-[#d5fae7]"
            />

            <!-- 오른쪽 및 전체 너비 카드 말풍선 꼬리 -->
            <span v-else class="absolute -top-1.5 right-2 h-3 w-3 rotate-45 bg-[#d5fae7]" />

            <span class="relative">
              {{ card.description }}
            </span>
          </div>
        </article>
      </div>

      <!-- 상세 리포트 버튼 -->
      <button
        type="button"
        class="mt-auto mb-2 flex h-[52px] w-full shrink-0 items-center justify-center gap-2 rounded-full bg-brand text-[16px] font-bold text-ink shadow-[0_6px_14px_rgba(250,230,77,0.2)] transition active:scale-[0.98]"
        @click="moveToReport"
      >
        상세 리포트 보러가기
        <ArrowRight :size="22" :stroke-width="2.2" />
      </button>
    </div>
  </section>
</template>
