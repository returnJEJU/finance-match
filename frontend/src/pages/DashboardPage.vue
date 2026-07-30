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
      accent: '아주 좋아요.',
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
      description: '또래 평균 금융자산과 비교해 현재 자산 수준을 평가했어요.',
      score: Math.round(result.value.assetStabilityScore),
      maxScore: 30,
      icon: markRaw(BadgeDollarSign),
      iconClass: 'bg-brand-soft text-brand-ink',
      fullWidth: false,
    },
    {
      key: 'debt',
      title: '부채 관리',
      description: '소득 대비 상환 능력과 금융자산 대비 부채 부담을 함께 평가했어요.',
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
      description: '현재 자산과 저축 계획을 바탕으로 목표 달성 가능성을 계산했어요.',
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
      description: '연금계좌와 ISA의 절세 혜택 활용 정도를 평가했어요.',
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
  <section class="h-[calc(100dvh-108px)] overflow-hidden px-4 py-2">
    <!-- 로딩 -->
    <div v-if="loading" class="flex h-full items-center justify-center">
      <div class="h-9 w-9 animate-spin rounded-full border-4 border-line-card border-t-brand-ink" />
    </div>

    <!-- 오류 -->
    <div
      v-else-if="errorMessage"
      class="flex h-full flex-col items-center justify-center px-6 text-center"
    >
      <p class="text-[15px] font-semibold text-ink">
        {{ errorMessage }}
      </p>

      <button
        type="button"
        class="mt-5 rounded-full bg-brand px-7 py-3 text-[14px] font-bold text-ink"
        @click="loadDashboard"
      >
        다시 시도하기
      </button>
    </div>

    <!-- 대시보드 -->
    <div v-else-if="result" class="flex h-full flex-col">
      <!-- 총점 영역 -->
      <div class="flex shrink-0 flex-col items-center">
        <div class="relative mt-1 h-[168px] w-[168px]">
          <!-- 점수 원 -->
          <div
            class="absolute inset-0 rounded-full p-[12px]"
            :style="{
              background: `conic-gradient(
                from -20deg,
                #fff44f 0%,
                #fae64d ${totalProgress}%,
                #e5f4ee ${totalProgress}%,
                #e5f4ee 100%
              )`,
            }"
          >
            <div
              class="flex h-full w-full flex-col items-center justify-center rounded-full bg-canvas"
            >
              <span class="text-[58px] leading-none font-bold tracking-[-3px] text-[#275e50]">
                {{ totalScore }}
              </span>

              <span class="mt-2 text-[12px] text-brand-deep">♥</span>
            </div>
          </div>

          <!-- 레몬 장식 -->
          <div class="absolute -top-2 left-1/2 -translate-x-1/2 text-[21px]">🍋</div>
        </div>

        <!-- 궁합 문구 -->
        <div class="mt-2 w-full px-3">
          <p
            class="text-center text-[19px] leading-[1.3] font-extrabold tracking-[-0.7px] text-ink"
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
      <div class="mt-3 grid shrink-0 grid-cols-2 gap-2">
        <article
          v-for="card in scoreCards"
          :key="card.key"
          class="relative rounded-[15px] border border-line-soft bg-white px-3.5 py-2.5 shadow-[0_2px_7px_rgba(0,0,0,0.035)]"
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
              <p class="text-[12px] leading-none text-ink-sub">
                {{ card.title }}
              </p>

              <p class="mt-1 text-[19px] leading-none font-bold text-ink">
                {{ card.score }}점 / {{ card.maxScore }}점
              </p>
            </div>
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

              <!-- 두 칸 카드 도움말 -->
              <div class="relative z-40">
                <button
                  type="button"
                  class="block text-[#d2ccb5]"
                  :aria-label="`${card.title} 도움말`"
                  :aria-expanded="openedHelpKey === card.key"
                  @click="toggleHelp(card.key)"
                >
                  <CircleHelp :size="15" :stroke-width="2" />
                </button>

                <div
                  v-if="openedHelpKey === card.key"
                  role="tooltip"
                  class="absolute top-full z-50 mt-2 w-[300px] rounded-[12px] bg-[#d5fae7] px-4 py-3 text-left text-[12px] leading-[1.45] font-medium text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
                  :class="
                    card.key === 'debt' || card.key === 'tax'
                      ? 'right-0'
                      : 'left-1/2 -translate-x-1/2'
                  "
                >
                  <!-- 말풍선 꼬리 -->
                  <span
                    class="absolute -top-1.5 h-3 w-3 rotate-45 bg-[#d5fae7]"
                    :class="
                      card.key === 'debt' || card.key === 'tax'
                        ? 'right-0.5'
                        : 'left-1/2 -translate-x-1/2'
                    "
                  />

                  <span class="relative">
                    {{ card.description }}
                  </span>
                </div>
              </div>
            </div>

            <p class="mt-2 text-[12px] leading-none text-ink-sub">
              {{ card.title }}
            </p>

            <p
              v-if="card.key !== 'tax' || card.calculated"
              class="mt-1 whitespace-nowrap text-[17px] leading-none font-bold text-ink"
            >
              {{ card.score }}점 / {{ card.maxScore }}점
            </p>

            <p v-else class="mt-1 text-[16px] leading-none font-bold text-muted">평가 제외</p>

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

          <!-- 전체 너비 카드 도움말 -->
          <div v-if="card.fullWidth" class="absolute top-3 right-3 z-40">
            <button
              type="button"
              class="block text-[#d2ccb5]"
              :aria-label="`${card.title} 도움말`"
              :aria-expanded="openedHelpKey === card.key"
              @click="toggleHelp(card.key)"
            >
              <CircleHelp :size="15" :stroke-width="2" />
            </button>

            <div
              v-if="openedHelpKey === card.key"
              role="tooltip"
              class="absolute top-full right-0 z-50 mt-2 w-[300px] rounded-[12px] bg-[#d5fae7] px-4 py-3 text-left text-[12px] leading-[1.45] font-medium text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
            >
              <!-- 말풍선 꼬리 -->
              <span class="absolute -top-1.5 right-0.5 h-3 w-3 rotate-45 bg-[#d5fae7]" />

              <span class="relative">
                {{ card.description }}
              </span>
            </div>
          </div>
        </article>
      </div>

      <!-- 상세 리포트 버튼 -->
      <button
        type="button"
        class="mt-auto flex h-[52px] w-full shrink-0 items-center justify-center gap-2 rounded-full bg-brand text-[16px] font-bold text-ink shadow-[0_6px_14px_rgba(250,230,77,0.2)] transition active:scale-[0.98]"
        @click="moveToReport"
      >
        상세 리포트 보러가기

        <ArrowRight :size="22" :stroke-width="2.2" />
      </button>
    </div>
  </section>
</template>
