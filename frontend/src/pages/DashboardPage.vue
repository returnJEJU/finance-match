<script setup>
import { computed, markRaw, onMounted, onUnmounted, ref } from 'vue'
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
import { getReport } from '@/api/report'

const router = useRouter()

const result = ref(null)
const loading = ref(true)
const errorMessage = ref('')
const openedHelpKey = ref(null)

/*
 * 대시보드 카드 key와 리포트 API의 점수 축 key를 연결한다.
 */
const AXIS_KEY_BY_CARD = {
  asset: 'ASSET_STABILITY',
  debt: 'DEBT_REPAYMENT',
  value: 'FINANCIAL_VALUE',
  goal: 'GOAL_FEASIBILITY',
  tax: 'TAX_STRATEGY',
}

/*
 * 백엔드에서 생성한 점수별 설명 문구
 */
const reasonsByAxisKey = ref({})

/*
 * 원형 그래프 애니메이션 상태
 */
const animatedScore = ref(0)
const animatedProgress = ref(0)

let animationFrameId = null

const CIRCLE_RADIUS = 76
const CIRCLE_CIRCUMFERENCE = 2 * Math.PI * CIRCLE_RADIUS

/*
 * 총점
 */
const totalScore = computed(() => {
  return Math.round(result.value?.totalScore ?? 0)
})

/*
 * 원형 그래프에 표시할 0~100 범위의 점수
 */
const totalProgress = computed(() => {
  return Math.min(Math.max(totalScore.value, 0), 100)
})

/*
 * SVG 원형 그래프의 채워지지 않은 길이
 */
const circleDashOffset = computed(() => {
  const progress = Math.min(Math.max(animatedProgress.value, 0), 100) / 100

  return CIRCLE_CIRCUMFERENCE * (1 - progress)
})

/*
 * 총점에 따른 안내 문구
 */
const compatibilityMessage = computed(() => {
  const score = totalScore.value

  if (score >= 90) {
    return {
      first: '환상의 찰떡궁합! 우리 부부는',
      second: '금융 호흡까지 ',
      accent: '완벽해요.',
    }
  }

  if (score >= 80) {
    return {
      first: '찰떡궁합! 우리 부부의',
      second: '금융 조화는 ',
      accent: '완벽해요.',
    }
  }

  if (score >= 60) {
    return {
      first: '제법 잘 맞는 우리 부부!',
      second: '조금만 더 맞추면 ',
      accent: '더욱 든든해져요.',
    }
  }

  return {
    first: '우리, 이제 맞춰가는 중!',
    second: '금융 습관을 하나씩 ',
    accent: '맞춰봐요.',
  }
})

/*
 * 항목별 점수 카드
 */
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
      maxScore: 25,
      icon: markRaw(BadgeDollarSign),
      iconClass: 'bg-[#fff8c9] text-[#887d16]',
      layout: 'half',
    },
    {
      key: 'value',
      title: '투자 가치관 일치도',
      description: '투자 성향과 위험 선호가 얼마나 비슷한지 분석했어요.',
      score: Math.round(result.value.financialValueScore),
      maxScore: 25,
      icon: markRaw(HeartHandshake),
      iconClass: 'bg-[#f4f2d9] text-[#7f7a27]',
      layout: 'half',
    },
    {
      key: 'goal',
      title: '목표 달성률',
      description: '현재 자산과 저축 계획으로 목표 달성 가능성을 계산했어요.',
      score: Math.round(result.value.goalFeasibilityScore),
      maxScore: 20,
      icon: markRaw(Target),
      iconClass: 'bg-[#eaf5e7] text-[#57814f]',
      layout: 'full',
    },
    {
      key: 'debt',
      title: '부채 관리',
      description: '소득 대비 상환 능력과 자산 대비 부채 부담을 평가했어요.',
      score: Math.round(result.value.debtRepaymentScore),
      maxScore: 20,
      icon: markRaw(Landmark),
      iconClass: 'bg-[#fff0e4] text-[#a65e37]',
      layout: 'half',
    },
    {
      key: 'tax',
      title: '절세 활용도',
      description: '연금계좌와 ISA의 절세 혜택 활용도를 평가했어요.',
      score: Math.round(result.value.taxStrategyScore),
      maxScore: 10,
      icon: markRaw(BadgeDollarSign),
      iconClass: 'bg-[#f0eaf8] text-[#755b96]',
      layout: 'half',
      calculated: result.value.taxStrategyCalculated,
    },
  ]
})

/*
 * 애니메이션이 끝날수록 속도를 줄인다.
 */
const easeOutCubic = (progress) => {
  return 1 - Math.pow(1 - progress, 3)
}

/*
 * 숫자와 원형 그래프를 0부터 실제 점수까지 증가시킨다.
 */
const startScoreAnimation = () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }

  const targetScore = totalProgress.value

  animatedScore.value = 0
  animatedProgress.value = 0

  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  if (prefersReducedMotion) {
    animatedScore.value = targetScore
    animatedProgress.value = targetScore
    return
  }

  const duration = 1400
  const startTime = performance.now()

  const animate = (currentTime) => {
    const elapsed = currentTime - startTime

    const progress = Math.min(elapsed / duration, 1)

    const easedProgress = easeOutCubic(progress)

    animatedScore.value = Math.round(targetScore * easedProgress)

    animatedProgress.value = targetScore * easedProgress

    if (progress < 1) {
      animationFrameId = requestAnimationFrame(animate)
      return
    }

    animatedScore.value = targetScore
    animatedProgress.value = targetScore
    animationFrameId = null
  }

  animationFrameId = requestAnimationFrame(animate)
}

/*
 * 백엔드에서 규칙 기반으로 생성한 점수 설명 문구를 조회한다.
 *
 * 리포트가 아직 준비되지 않았거나 조회에 실패하면
 * 카드에 정의한 기본 description을 사용한다.
 */
const loadReasons = async () => {
  try {
    const report = await getReport()

    reasonsByAxisKey.value = Object.fromEntries(
      report.scoreAxes.map((axis) => [axis.key, axis.reason]),
    )
  } catch {
    reasonsByAxisKey.value = {}
  }
}

/*
 * 대시보드 점수를 조회한다.
 */
const loadDashboard = async () => {
  loading.value = true
  errorMessage.value = ''
  openedHelpKey.value = null
  reasonsByAxisKey.value = {}

  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }

  animatedScore.value = 0
  animatedProgress.value = 0

  try {
    result.value = await getCompatibility()

    startScoreAnimation()

    /*
     * 점수 화면을 먼저 표시하고 설명 문구는 별도로 조회한다.
     */
    loadReasons()
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

/*
 * 도움말 열기 또는 닫기
 */
const toggleHelp = (key) => {
  openedHelpKey.value = openedHelpKey.value === key ? null : key
}

/*
 * 도움말 바깥 영역을 누르면 도움말을 닫는다.
 */
const handleOutsideHelpClick = (event) => {
  if (!openedHelpKey.value) {
    return
  }

  const onTrigger = event.target.closest('[data-help-card]')

  const onTooltip = event.target.closest('[role="tooltip"]')

  if (!onTrigger && !onTooltip) {
    openedHelpKey.value = null
  }
}

/*
 * 상세 리포트로 이동
 */
const moveToReport = () => {
  router.push('/report')
}

onMounted(() => {
  document.addEventListener('click', handleOutsideHelpClick)

  loadDashboard()
})

onUnmounted(() => {
  document.removeEventListener('click', handleOutsideHelpClick)

  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
})
</script>

<template>
  <section class="dashboard-page h-[calc(100dvh-108px)] overflow-y-auto">
    <!-- 배경 장식 -->
    <div aria-hidden="true" class="dashboard-decoration dashboard-decoration-left" />

    <div aria-hidden="true" class="dashboard-decoration dashboard-decoration-right" />

    <!-- 로딩 -->
    <div v-if="loading" class="relative z-10 flex h-full items-center justify-center">
      <div class="h-9 w-9 animate-spin rounded-full border-4 border-[#ecebdc] border-t-[#7d781e]" />
    </div>

    <!-- 오류 -->
    <div
      v-else-if="errorMessage"
      class="relative z-10 flex h-full flex-col items-center justify-center px-6 text-center"
    >
      <p class="text-[16px] font-semibold text-[#242424]">
        {{ errorMessage }}
      </p>

      <button
        type="button"
        class="mt-5 rounded-full bg-[#ffef3d] px-7 py-3 text-[15px] font-bold text-[#242424]"
        @click="loadDashboard"
      >
        다시 시도하기
      </button>
    </div>

    <!-- 대시보드 -->
    <div
      v-else-if="result"
      class="relative z-10 mx-auto flex min-h-full w-full max-w-[428px] flex-col px-[18px] pt-3 pb-7"
    >
      <!-- 안내 문구 -->
      <div class="mt-4 shrink-0">
        <p
          class="text-center text-[19px] leading-[1.35] font-extrabold tracking-[-0.7px] text-[#242424]"
        >
          {{ compatibilityMessage.first }}
          <br />

          {{ compatibilityMessage.second }}

          <span class="border-b-2 border-[#b4a91c] pb-[1px] text-[#8d8411]">
            {{ compatibilityMessage.accent }}
          </span>
        </p>
      </div>

      <!-- 총점 원형 그래프 -->
      <div class="mt-4 flex shrink-0 justify-center">
        <div class="score-ring relative h-[190px] w-[190px]">
          <svg
            class="h-full w-full overflow-visible"
            viewBox="0 0 200 200"
            role="img"
            :aria-label="`금융 궁합 총점 ${totalScore}점`"
          >
            <defs>
              <linearGradient
                id="scoreRingGradient"
                x1="20"
                y1="20"
                x2="180"
                y2="180"
                gradientUnits="userSpaceOnUse"
              >
                <stop offset="0%" stop-color="#fff700" />
                <stop offset="55%" stop-color="#ffe51e" />
                <stop offset="100%" stop-color="#fff45a" />
              </linearGradient>

              <filter id="ringShadow" x="-30%" y="-30%" width="160%" height="160%">
                <feDropShadow
                  dx="0"
                  dy="3"
                  stdDeviation="3"
                  flood-color="#d3c600"
                  flood-opacity="0.24"
                />
              </filter>
            </defs>

            <!-- 원형 그래프 배경 -->
            <circle
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="#f0f1ed"
              stroke-width="13"
            />

            <!-- 애니메이션 진행 원 -->
            <circle
              class="score-progress-circle"
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="url(#scoreRingGradient)"
              stroke-width="13"
              stroke-linecap="round"
              :stroke-dasharray="CIRCLE_CIRCUMFERENCE"
              :stroke-dashoffset="circleDashOffset"
              filter="url(#ringShadow)"
            />
          </svg>

          <!-- 중앙 점수 -->
          <div class="pointer-events-none absolute inset-0 flex items-center justify-center">
            <span
              class="score-number text-[68px] leading-none font-extrabold tracking-[-4px] text-[#19513c]"
            >
              {{ animatedScore }}
            </span>
          </div>

          <!-- 레몬 장식 -->
          <div
            aria-hidden="true"
            class="lemon-decoration absolute -top-[9px] left-1/2 z-10 -translate-x-1/2"
          >
            <span class="lemon-leaf"> ● </span>

            <span class="lemon-fruit"> ● </span>

            <span class="lemon-spark lemon-spark-one"> · </span>

            <span class="lemon-spark lemon-spark-two"> · </span>

            <span class="lemon-spark lemon-spark-three"> · </span>
          </div>
        </div>
      </div>

      <!-- 항목별 점수 카드 -->
      <div class="mt-4 grid shrink-0 grid-cols-2 gap-[10px]">
        <article
          v-for="card in scoreCards"
          :key="card.key"
          class="score-card relative min-h-[86px] rounded-[11px] border border-[#f1f1ed] bg-white px-[13px] py-[11px] shadow-[0_3px_10px_rgba(0,0,0,0.035)]"
          :class="{
            'col-span-2': card.layout === 'full',
          }"
        >
          <!-- 카드 상단 -->
          <div class="flex items-start justify-between">
            <!-- 카드 아이콘 -->
            <div
              class="flex h-[23px] w-[23px] items-center justify-center rounded-[7px]"
              :class="card.iconClass"
            >
              <component :is="card.icon" :size="14" :stroke-width="2" />
            </div>

            <!-- 도움말 버튼 -->
            <button
              type="button"
              class="text-[#c8c3ad] transition hover:text-[#8d876c]"
              :aria-label="`${card.title} 도움말`"
              :aria-expanded="openedHelpKey === card.key"
              :data-help-card="card.key"
              @click.stop="toggleHelp(card.key)"
            >
              <CircleHelp :size="13" :stroke-width="1.8" />
            </button>
          </div>

          <!-- 카드 제목 -->
          <p
            class="mt-[7px] text-[11px] leading-none font-medium tracking-[-0.25px] text-[#575757]"
          >
            {{ card.title }}
          </p>

          <!-- 점수 -->
          <p
            v-if="card.key !== 'tax' || card.calculated"
            class="mt-[6px] text-[15px] leading-none font-extrabold tracking-[-0.45px] text-[#242424]"
          >
            {{ card.score }}점

            <span class="font-semibold"> / {{ card.maxScore }}점 </span>
          </p>

          <!-- 절세 평가 제외 -->
          <p v-else class="mt-[6px] text-[14px] leading-none font-bold text-[#888]">평가 제외</p>

          <!-- 점수 산출 이유 도움말 -->
          <div
            v-if="openedHelpKey === card.key"
            role="tooltip"
            class="absolute top-8 right-2 left-2 z-50 rounded-[9px] bg-[#d8f7e8] px-3 py-2 text-[11px] leading-[1.45] font-medium text-[#26372f] shadow-[0_5px_15px_rgba(0,0,0,0.12)]"
          >
            {{ reasonsByAxisKey[AXIS_KEY_BY_CARD[card.key]] || card.description }}
          </div>
        </article>
      </div>

      <!-- 상세 리포트 버튼 -->
      <button
        type="button"
        class="mt-[14px] flex h-[48px] w-full shrink-0 items-center justify-center gap-2 rounded-full bg-[#ffef3d] text-[14px] font-extrabold text-[#202020] shadow-[0_6px_16px_rgba(236,215,16,0.18)] transition duration-150 hover:bg-[#ffe926] active:scale-[0.98]"
        @click="moveToReport"
      >
        상세 리포트 보러가기

        <ArrowRight :size="20" :stroke-width="2.5" />
      </button>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  position: relative;
  background: linear-gradient(180deg, #ffffff 0%, #ffffff 70%, #fffef6 100%);
  isolation: isolate;
}

.dashboard-decoration {
  position: absolute;
  pointer-events: none;
  z-index: 0;
}

.dashboard-decoration-left {
  top: -56px;
  left: -64px;
  width: 188px;
  height: 188px;
  border-radius: 46% 54% 58% 42%;
  background: radial-gradient(
    circle at 65% 65%,
    rgba(255, 239, 130, 0.72),
    rgba(255, 239, 130, 0.16) 66%,
    transparent 67%
  );
  transform: rotate(-18deg);
}

.dashboard-decoration-right {
  top: 126px;
  right: -118px;
  width: 260px;
  height: 330px;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(223, 245, 237, 0.86), rgba(235, 249, 244, 0.28));
  transform: rotate(29deg);
  opacity: 0.72;
}

.score-progress-circle {
  transform: rotate(-90deg);
  transform-origin: 100px 100px;
  will-change: stroke-dashoffset;
}

.score-number {
  font-variant-numeric: tabular-nums;
  text-shadow:
    0 2px 0 rgba(255, 255, 255, 0.9),
    0 3px 5px rgba(20, 73, 53, 0.08);
}

.lemon-decoration {
  width: 42px;
  height: 42px;
}

.lemon-fruit {
  position: absolute;
  top: 11px;
  left: 11px;
  color: #ffef00;
  font-size: 29px;
  line-height: 1;
  transform: scaleX(0.7) rotate(17deg);
  text-shadow:
    0 1px 0 #e2cc00,
    0 2px 3px rgba(205, 186, 0, 0.18);
}

.lemon-leaf {
  position: absolute;
  top: 1px;
  left: 19px;
  z-index: 2;
  color: #75ce3b;
  font-size: 16px;
  line-height: 1;
  transform: scaleX(1.7) rotate(-26deg);
}

.lemon-spark {
  position: absolute;
  color: #d8c900;
  font-size: 25px;
  font-weight: 900;
  line-height: 1;
}

.lemon-spark-one {
  top: 5px;
  right: 1px;
}

.lemon-spark-two {
  top: 13px;
  right: -4px;
}

.lemon-spark-three {
  top: 21px;
  right: 1px;
}

.score-card {
  overflow: visible;
}

@media (prefers-reduced-motion: reduce) {
  .score-progress-circle {
    transition: none;
  }

  .score-card,
  button {
    transition: none;
  }
}
</style>
