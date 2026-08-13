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

// 대시보드 캐릭터 이미지
import dashboardCharacter from '@/assets/images/characters/character-dashboard.png'

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

  /*
   * 기존 1400ms에서 2200ms로 변경해 점수가 천천히 올라가도록 한다.
   */
  const duration = 2200
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
          class="text-center text-[21px] leading-[1.35] font-extrabold tracking-[-0.7px] text-[#242424]"
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
        <div class="score-ring relative h-[230px] w-[230px]">
          <svg
            class="h-full w-full overflow-visible"
            viewBox="0 0 200 200"
            role="img"
            :aria-label="`금융 궁합 총점 ${totalScore}점`"
          >
            <defs>
              <!-- 노란 진행 바 그라데이션 -->
              <linearGradient
                id="scoreRingGradient"
                x1="35"
                y1="25"
                x2="170"
                y2="175"
                gradientUnits="userSpaceOnUse"
              >
                <stop offset="0%" stop-color="#fff943" />
                <stop offset="48%" stop-color="#ffed22" />
                <stop offset="100%" stop-color="#fff338" />
              </linearGradient>

              <!-- 원형 바 전체 그림자 -->
              <filter id="ringBaseShadow" x="-30%" y="-30%" width="160%" height="170%">
                <feDropShadow
                  dx="0"
                  dy="4"
                  stdDeviation="5"
                  flood-color="#8f7c38"
                  flood-opacity="0.14"
                />
              </filter>

              <!-- 노란색 바 그림자 -->
              <filter id="yellowRingShadow" x="-30%" y="-30%" width="160%" height="170%">
                <feDropShadow
                  dx="0"
                  dy="3"
                  stdDeviation="2"
                  flood-color="#d49b00"
                  flood-opacity="0.38"
                />
              </filter>
            </defs>

            <!-- 전체 원형 바 그림자 -->
            <circle
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="#e4e1d9"
              stroke-width="20"
              opacity="0.42"
              filter="url(#ringBaseShadow)"
            />

            <!-- 채워지지 않은 아이보리색 바 -->
            <circle
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="#f0efea"
              stroke-width="18"
            />

            <!-- 아이보리색 바의 안쪽 밝은 선 -->
            <circle
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="#faf9f6"
              stroke-width="13"
            />

            <!-- 연한 노란색 테두리 -->
            <circle
              class="score-progress-circle"
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="#f4d65c"
              stroke-width="19"
              stroke-linecap="round"
              :stroke-dasharray="CIRCLE_CIRCUMFERENCE"
              :stroke-dashoffset="circleDashOffset"
              filter="url(#yellowRingShadow)"
            />

            <!-- 메인 노란색 진행 바 -->
            <circle
              class="score-progress-circle"
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="url(#scoreRingGradient)"
              stroke-width="17"
              stroke-linecap="round"
              :stroke-dasharray="CIRCLE_CIRCUMFERENCE"
              :stroke-dashoffset="circleDashOffset"
            />

            <!-- 노란색 바 안쪽의 흰색 하이라이트 -->
            <circle
              class="score-progress-circle score-progress-highlight"
              cx="100"
              cy="100"
              :r="CIRCLE_RADIUS"
              fill="none"
              stroke="rgba(255, 255, 255, 0.65)"
              stroke-width="2"
              stroke-linecap="round"
              :stroke-dasharray="CIRCLE_CIRCUMFERENCE"
              :stroke-dashoffset="circleDashOffset"
            />
          </svg>

          <!-- 중앙 점수 -->
          <div class="pointer-events-none absolute inset-0 flex items-center justify-center">
            <span
              class="score-number text-[78px] leading-none font-black tracking-[-5px] text-[#005538]"
            >
              {{ animatedScore }}
            </span>
          </div>

          <!-- 원형 바 시작점의 잎 장식 -->
          <div
            aria-hidden="true"
            class="leaf-decoration pointer-events-none absolute top-[-2px] left-1/2 z-20"
          >
            <span class="leaf leaf-left" />
            <span class="leaf leaf-right" />
          </div>
        </div>
      </div>

      <!-- 항목별 점수 카드 -->
      <div class="mt-4 grid shrink-0 grid-cols-2 gap-[10px]">
        <article
          v-for="card in scoreCards"
          :key="card.key"
          class="score-card relative rounded-[11px] border border-[#f1f1ed] bg-white px-[14px] py-[13px] shadow-[0_3px_10px_rgba(0,0,0,0.035)]"
          :class="[card.layout === 'full' ? 'col-span-2 min-h-[104px]' : 'min-h-[94px]']"
        >
          <!-- 카드 상단: 아이콘과 제목을 같은 줄에 배치 -->
          <div class="relative z-20 flex items-center justify-between gap-2">
            <div class="flex min-w-0 items-center gap-2">
              <!-- 카드 아이콘 -->
              <div
                class="flex h-[28px] w-[28px] shrink-0 items-center justify-center rounded-[7px]"
                :class="card.iconClass"
              >
                <component :is="card.icon" :size="16" :stroke-width="2" />
              </div>

              <!-- 카드 제목 -->
              <p
                class="truncate text-[13px] leading-[1.2] font-semibold tracking-[-0.3px] text-[#575757]"
              >
                {{ card.title }}
              </p>
            </div>

            <!-- 도움말 버튼 -->
            <button
              type="button"
              class="relative z-30 shrink-0 text-[#c8c3ad] transition hover:text-[#8d876c]"
              :aria-label="`${card.title} 도움말`"
              :aria-expanded="openedHelpKey === card.key"
              :data-help-card="card.key"
              @click.stop="toggleHelp(card.key)"
            >
              <CircleHelp :size="15" :stroke-width="1.8" />
            </button>
          </div>

          <!-- 점수 -->
          <p
            v-if="card.key !== 'tax' || card.calculated"
            class="relative z-20 mt-[12px] text-[17px] leading-none font-extrabold tracking-[-0.45px] text-[#242424]"
          >
            {{ card.score }}점

            <span class="font-semibold"> / {{ card.maxScore }}점 </span>
          </p>

          <!-- 절세 평가 제외 -->
          <p v-else class="relative z-20 mt-[12px] text-[15px] leading-none font-bold text-[#888]">
            평가 제외
          </p>

          <!-- 목표 달성률 카드에만 캐릭터 표시 -->
          <img
            v-if="card.key === 'goal'"
            :src="dashboardCharacter"
            alt=""
            aria-hidden="true"
            class="pointer-events-none absolute right-[45px] bottom-[-18px] z-10 w-[140px]"
          />

          <!-- 점수 산출 이유 도움말 -->
          <div
            v-if="openedHelpKey === card.key"
            role="tooltip"
            class="absolute top-10 right-2 left-2 z-50 rounded-[9px] bg-[#d8f7e8] px-3 py-2 text-[12px] leading-[1.45] font-medium text-[#26372f] shadow-[0_5px_15px_rgba(0,0,0,0.12)]"
          >
            {{ reasonsByAxisKey[AXIS_KEY_BY_CARD[card.key]] || card.description }}
          </div>
        </article>
      </div>

      <!-- 버튼이 첫 화면 바로 아래에서 보이도록 만드는 스크롤 여백 -->
      <div aria-hidden="true" class="min-h-[4vh] shrink-0" />

      <!-- 상세 리포트 버튼 -->
      <button
        type="button"
        class="flex h-[52px] w-full shrink-0 items-center justify-center gap-2 rounded-full bg-[#ffef3d] text-[16px] font-extrabold text-[#202020] shadow-[0_6px_16px_rgba(236,215,16,0.18)] transition duration-150 hover:bg-[#ffe926] active:scale-[0.98]"
        @click="moveToReport"
      >
        상세 리포트 보러가기

        <ArrowRight :size="20" :stroke-width="2.5" />
      </button>
    </div>
  </section>
</template>

<style scoped>
.leaf-decoration {
  position: absolute;
  width: 56px;
  height: 30px;

  /* 가운데 정렬하면서 위아래 반전 */
  transform: translateX(-50%) scaleY(-1);
}

.leaf {
  position: absolute;
  bottom: 0;
  display: block;
  width: 29px;
  height: 19px;
  box-shadow: 0 2px 3px rgba(0, 75, 46, 0.12);
}

/* 왼쪽 위를 향하는 진한 잎 */
.leaf-left {
  left: 1px;
  border-radius: 100% 0 100% 0;
  background: linear-gradient(135deg, #00633d 0%, #00894f 55%, #006b40 100%);
}

/* 오른쪽 위를 향하는 연한 잎 */
.leaf-right {
  right: 1px;
  border-radius: 0 100% 0 100%;
  background: linear-gradient(135deg, #80ca58 0%, #50b448 55%, #288e41 100%);
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
  will-change: 0.75;
}

.score-number {
  font-variant-numeric: tabular-nums;
  text-shadow:
    0 2px 0 rgba(255, 255, 255, 0.9),
    0 3px 5px rgba(20, 73, 53, 0.08);
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
