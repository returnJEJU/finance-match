<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import {
  ChevronDown,
  ChevronUp,
  BadgeDollarSign,
  Landmark,
  HeartHandshake,
  Target,
} from 'lucide-vue-next'

import { getReport, getReportStatus } from '@/api/report'
import AnimatedCharacter from '@/components/ui/AnimatedCharacter.vue'
import { investmentTypeMeta as investmentTypeMetaByLabel } from '@/constants/investmentTypeMeta'
import { abbreviateKoreanName } from '@/utils/koreanName'

// 백엔드(PersonalInvestmentType) enum 코드 → 한글 라벨. investmentTypeMetaByLabel(공용 상수)이
// 한글 라벨을 키로 쓰기 때문에 다리 역할이 필요하다. 캐릭터 이미지·accentClass는 이제 그 공용
// 상수 하나로만 관리되어, 색이 바뀌어도 SurveyResultPage.vue와 여기 둘 다 자동으로 맞는다.
//
// TEMP: description 은 백엔드에 아직 "확정 후 수정" 임시 문구뿐이고, 이 리포트 API는 애초에
// 본인 것만 조회 가능해 파트너 설명을 못 받아온다 — 실제 문구 나오면 교체.
const INVESTMENT_TYPE_DESCRIPTIONS = {
  STABLE: '안정성과 예측 가능한 관리를 가장 중요하게 생각해요.',
  STABLE_SEEKING: '안정성을 우선하면서도 약간의 위험은 감수해 꾸준히 자산을 불려가요.',
  NEUTRAL: '안정과 수익 사이에서 균형을 유연하게 고려해요.',
  AGGRESSIVE: '높은 수익을 위해 어느 정도의 위험은 감수하고 적극적으로 기회를 활용해요.',
  VERY_AGGRESSIVE: '높은 위험을 감수하더라도 최대한의 수익을 추구하는 걸 선호해요.',
}

const INVESTMENT_TYPE_LABELS = {
  STABLE: '든든지킴형',
  STABLE_SEEKING: '차곡성장형',
  NEUTRAL: '균형설계형',
  AGGRESSIVE: '적극성장형',
  VERY_AGGRESSIVE: '과감도전형',
}

function investmentTypeMetaFor(code) {
  const label = INVESTMENT_TYPE_LABELS[code]
  return {
    label,
    description: INVESTMENT_TYPE_DESCRIPTIONS[code],
    ...investmentTypeMetaByLabel[label],
  }
}

// 커플 공통 성향(investmentProfile.we)은 개인 성향과 다른 값이다 — 두 사람의 개인 성향 단계
// 차이(0~4)를 나타내는 CoupleInvestmentType(DIFF_0~DIFF_4, 백엔드 CoupleInvestmentTypeCalculator)다.
// TEMP: 실제 디자인·문구가 아직 기획되지 않아 임시 라벨·설명으로 연결해둔다. 확정되면 여기만 교체하면 된다.
const coupleInvestmentTypeMeta = {
  DIFF_0: {
    label: '찰떡귱합형',
    description: '금융을 바라보는 기준과 방향이 꼭 닮은 찰떡귱합이에요.',
  },
  DIFF_1: {
    label: '닮은성향형',
    description: '금융 기준이 대체로 비슷해 중요한 결정을 함께 내리기 편해요',
  },
  DIFF_2: {
    label: '균형조합형',
    description: '서로 다른 장점이 적절하게 만나 균형을 만들어가는 조합이에요',
  },
  DIFF_3: {
    label: '조율성장형',
    description: '차이는 있지만 대화를 통해 서로에게 맞는 방향을 찾아갈 수 있어요',
  },
  DIFF_4: {
    label: '반전케미형',
    description: '서로의 금융 관점이 뚜렷하게 달라요. 함께 지킬 기준을 정하는게 중요해요.',
  },
}

// 점수 축(key)별 아이콘 — report API 의 scoreAxes[].key 와 매핑한다.
// DashboardPage.vue 의 scoreCards 아이콘·색상과 동일하게 맞춘다(같은 축은 페이지가 달라도 같은 아이콘).
const scoreIconMeta = {
  ASSET_STABILITY: { icon: BadgeDollarSign, iconClass: 'bg-brand-soft text-brand-ink' },
  DEBT_REPAYMENT: { icon: Landmark, iconClass: 'bg-[#fff3e9] text-[#a94700]' },
  FINANCIAL_VALUE: { icon: HeartHandshake, iconClass: 'bg-[#f5f5dc] text-[#777000]' },
  GOAL_FEASIBILITY: { icon: Target, iconClass: 'bg-[#eef7e9] text-[#46763b]' },
  TAX_STRATEGY: { icon: BadgeDollarSign, iconClass: 'bg-[#f5f0ff] text-[#66528c]' },
}

const queryClient = useQueryClient()

const {
  data: report,
  isLoading,
  isError,
  error,
} = useQuery({
  queryKey: ['report'],
  queryFn: getReport,
  // NOT_FOUND(준비 중)는 기본 재시도로 몇 번 더 불러봐야 소용없다 — 아래 reportStatus 폴링이
  // 다 끝났을 때만 다시 부르도록 직접 트리거한다.
  retry: false,
})

// 리포트가 아직 없는 건(파트너 설문 미완료·계산 전) 진짜 오류가 아니라 "준비 중" 상태다.
// ReportService 가 이때 NOT_FOUND 로 응답한다(GET /v1/members/me/report).
const isNotReady = computed(() => error.value?.code === 'NOT_FOUND')

// 준비 중일 때만 진행 상황을 가볍게 폴링한다 — 5축 중 몇 축이 끝났는지(ReportService.getReportStatus).
// ready 가 되는 순간 report 쿼리를 다시 불러 완성된 리포트로 자연스럽게 넘어간다.
const { data: reportStatus } = useQuery({
  queryKey: ['reportStatus'],
  queryFn: getReportStatus,
  enabled: isNotReady,
  refetchInterval: (query) => (query.state.data?.ready ? false : 4000),
})

watch(
  () => reportStatus.value?.ready,
  (ready) => {
    if (ready) {
      queryClient.invalidateQueries({ queryKey: ['report'] })
    }
  },
)

// 목표 달성 가능성 카드의 진행 현황 — report API 의 goalProgress 를 그대로 쓴다.
// achieved=false(부족)면 마젠타(warn 토큰), true(초과)면 초록(good 토큰)으로 갈린다.
const goalProgress = computed(() => report.value.goalProgress)

const goalDifferenceLabel = computed(() =>
  goalProgress.value?.achieved ? '목표를 넘어선 예상액' : '목표까지 부족한 금액',
)

const coupleTypeLabel = computed(
  () => coupleInvestmentTypeMeta[report.value.investmentProfile.we].label,
)
const coupleTypeDescription = computed(
  () => coupleInvestmentTypeMeta[report.value.investmentProfile.we].description,
)

const me = computed(() => ({
  name: report.value.name,
  ...investmentTypeMetaFor(report.value.investmentProfile.me),
}))
const partner = computed(() => ({
  name: report.value.partnerName,
  ...investmentTypeMetaFor(report.value.investmentProfile.you),
}))

// "OOOO형" 글자색 — 반으로 딱 잘리지 않고 나(왼쪽 위) 색에서 파트너(오른쪽 아래) 색으로 자연스럽게
// 이어지는 대각선 그라데이션. accentClass는 'text-[#RRGGBB]' 형태라 그 안의 hex만 뽑아 쓴다.
const accentColorOf = (person) => person?.accentClass?.match(/#[0-9a-fA-F]{3,6}/)?.[0] ?? '#333333'
const coupleTypeGradientStyle = computed(() => ({
  backgroundImage: `linear-gradient(135deg, ${accentColorOf(me.value)}, ${accentColorOf(partner.value)})`,
  WebkitBackgroundClip: 'text',
  backgroundClip: 'text',
  color: 'transparent',
}))

// 슬라이더 위치(%) — 안정형(0%) ~ 공격투자형(100%) 5단계 중 개인 성향의 인덱스로 계산.
// 커플 공통 노브 1개 대신, 두 사람 각자의 위치를 따로 찍는다 — 이 축(안정형~공격형) 자체가
// 원래 개인 성향 스펙트럼이라 "각자 어디에 있는지"가 "커플이 얼마나 다른지(DIFF)"보다 더 잘 맞는다.
const investmentTypeOrder = ['STABLE', 'STABLE_SEEKING', 'NEUTRAL', 'AGGRESSIVE', 'VERY_AGGRESSIVE']
const typePosition = (code) => {
  const index = investmentTypeOrder.indexOf(code)
  return (index / (investmentTypeOrder.length - 1)) * 100
}
const mePosition = computed(() => typePosition(report.value.investmentProfile.me))
const partnerPosition = computed(() => typePosition(report.value.investmentProfile.you))

// 슬라이더 색은 사람이 아니라 "위치"(안정 쪽=민트 ~ 도전 쪽=노랑) 기준으로 정해진다 —
// 누가 왼쪽/오른쪽에 오는지는 그때그때 다르므로, 매번 낮은 쪽·높은 쪽을 다시 구한다.
const isMeLower = computed(() => mePosition.value <= partnerPosition.value)
const lowerPerson = computed(() => (isMeLower.value ? me.value : partner.value))
const higherPerson = computed(() => (isMeLower.value ? partner.value : me.value))
const lowerPosition = computed(() => Math.min(mePosition.value, partnerPosition.value))
const higherPosition = computed(() => Math.max(mePosition.value, partnerPosition.value))

// 두 사람 성향이 같으면 노브 위치도 같아져, 나중에 그려지는(도전 쪽) 노브가 다른 노브를 완전히
// 가려버려서 안쪽 노브는 호버가 아예 안 먹힌다. 겹칠 때만 양옆으로 살짝 벌려 둘 다 호버 가능하게 한다.
const isOverlapping = computed(() => lowerPosition.value === higherPosition.value)
const lowerKnobLeft = computed(() =>
  isOverlapping.value ? `calc(${lowerPosition.value}% - 6px)` : `${lowerPosition.value}%`,
)
const higherKnobLeft = computed(() =>
  isOverlapping.value ? `calc(${higherPosition.value}% + 6px)` : `${higherPosition.value}%`,
)

// 캐릭터·커플 성향 라벨을 클릭하면 그 설명을 보여준다. 'me' | 'partner' | 'couple' | null —
// 하나만 열려있다. 모바일에선 hover 가 안 먹히니 클릭으로 여닫는다. 같은 걸 다시 누르면 닫힌다.
const openDescription = ref(null)
const toggleDescription = (who) => {
  openDescription.value = openDescription.value === who ? null : who
}

// 말풍선이 열려있을 때 화면의 다른 곳을 클릭하면 닫는다. 세 트리거(커플·나·파트너) 중
// 클릭한 지점이 하나에도 안 속해있으면 바깥 클릭으로 보고 닫는다.
const coupleDescriptionRef = ref(null)
const meDescriptionRef = ref(null)
const partnerDescriptionRef = ref(null)
const handleOutsideClick = (event) => {
  if (!openDescription.value) {
    return
  }
  const containers = [
    coupleDescriptionRef.value,
    meDescriptionRef.value,
    partnerDescriptionRef.value,
  ]
  const clickedInside = containers.some((el) => el?.contains(event.target))
  if (!clickedInside) {
    openDescription.value = null
  }
}
onMounted(() => document.addEventListener('click', handleOutsideClick))
onUnmounted(() => document.removeEventListener('click', handleOutsideClick))

// 두 캐릭터 사이의 "글래스 하트" — 얇은 하트 SVG를 여러 겹 쌓아 Z축으로 펼치고 rotateY 로 돌려서
// 3D처럼 보이게 만든다(Claude Design "Glass heart rotation effect" 포팅). 정적인 값이라 매 렌더마다
// 다시 계산할 필요 없이 한 번만 만들어둔다.
const HEART_PATH =
  'M50 84 C 21 62, 11 45, 11 30 C 11 17, 22 10, 32 10 C 41 10, 46 15, 50 22 C 54 15, 59 10, 68 10 C 78 10, 89 17, 89 30 C 89 45, 79 62, 50 84 Z'
const HEART_LAYER_COUNT = 20
const HEART_DEPTH = 22
const heartLayers = Array.from({ length: HEART_LAYER_COUNT }, (_, i) => {
  const t = i / (HEART_LAYER_COUNT - 1)
  return {
    key: i,
    z: (t - 0.5) * HEART_DEPTH,
    hue: Math.round(210 + t * 170),
    isFace: i === 0 || i === HEART_LAYER_COUNT - 1,
  }
})
const heartGlintZ = (HEART_DEPTH / 2 + 1).toFixed(2)

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
    <p v-if="isLoading" class="py-10 text-center text-[13px] text-muted">
      불러오는 중...(약 1분 소요)
    </p>
    <div v-else-if="isNotReady" class="px-4 py-16 text-center">
      <p class="text-[13px] text-muted">리포트를 준비하고 있어요. (약 2분 소요)</p>

      <div v-if="reportStatus" class="mx-auto mt-6 w-4/5">
        <div class="flex items-center justify-end text-[12px] font-semibold text-brand-ink">
          <span
            >{{ Math.round((reportStatus.completedAxes / reportStatus.totalAxes) * 100) }}%</span
          >
        </div>
        <div class="mt-2 h-2 overflow-hidden rounded-full bg-line-card">
          <div
            class="bg-brand-deep h-full rounded-full transition-all duration-500"
            :style="{ width: `${(reportStatus.completedAxes / reportStatus.totalAxes) * 100}%` }"
          />
        </div>
      </div>
    </div>
    <p v-else-if="isError" class="py-10 text-center text-[13px] text-warn">
      리포트를 불러오지 못했어요.
    </p>

    <template v-else>
      <!-- 커플 성향 히어로 -->
      <div ref="coupleDescriptionRef" class="relative text-center">
        <p class="text-[16px] font-medium text-ink-sub">우리 커플의 금융 스타일은</p>
        <button
          type="button"
          class="mt-1 cursor-pointer text-[40px] font-extrabold"
          :aria-expanded="openDescription === 'couple'"
          @click="toggleDescription('couple')"
        >
          <span :style="coupleTypeGradientStyle">{{ coupleTypeLabel }}</span>
          <span class="text-ink"></span>
        </button>
        <div
          v-if="openDescription === 'couple'"
          role="tooltip"
          class="absolute top-full left-1/2 z-10 mt-2 w-[220px] -translate-x-1/2 rounded-[12px] bg-[#d5fae7] px-3 py-3 text-center text-[14px] leading-[1.5] font-medium tracking-[-0.3px] text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
        >
          <span
            class="absolute -top-1.5 left-1/2 h-3 w-3 -translate-x-1/2 rotate-45 bg-[#d5fae7]"
          />
          <p class="relative font-bold">{{ coupleTypeLabel }}</p>
          <p class="relative mt-0.5">{{ coupleTypeDescription }}</p>
        </div>
      </div>

      <div class="mt-6 flex items-center justify-between px-2">
        <p class="text-[16px] text-ink-sub">
          {{ abbreviateKoreanName(me.name) }}님은
          <span class="font-semibold" :class="me.accentClass">{{ me.label }}</span>
        </p>
        <p class="text-[16px] text-ink-sub">
          {{ abbreviateKoreanName(partner.name) }}님은
          <span class="font-semibold" :class="partner.accentClass">{{ partner.label }}</span>
        </p>
      </div>

      <div class="mt-2 flex items-center justify-center gap-2">
        <div ref="meDescriptionRef" class="relative">
          <button
            type="button"
            class="block cursor-pointer"
            :aria-expanded="openDescription === 'me'"
            @click="toggleDescription('me')"
          >
            <AnimatedCharacter
              :src="me.character"
              :alt="me.label"
              img-class="h-36 w-36 object-contain"
            />
          </button>
          <div
            v-if="openDescription === 'me'"
            role="tooltip"
            class="absolute top-full left-1/2 z-10 mt-2 w-[180px] -translate-x-1/2 rounded-[12px] bg-[#d5fae7] px-3 py-3 text-center text-[14px] leading-[1.5] font-medium tracking-[-0.3px] text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
          >
            <span
              class="absolute -top-1.5 left-1/2 h-3 w-3 -translate-x-1/2 rotate-45 bg-[#d5fae7]"
            />
            <p class="relative font-bold">{{ me.label }}</p>
            <p class="relative mt-0.5">{{ me.description }}</p>
          </div>
        </div>

        <!-- 글래스 하트: 얇은 SVG 하트를 여러 겹 쌓아 3D 회전 -->
        <div class="relative flex h-16 w-16 shrink-0 items-center justify-center">
          <span
            class="absolute bottom-1 h-2 w-8 rounded-full bg-black/15 blur-[3px]"
            aria-hidden="true"
          />
          <div class="heart-float relative h-12 w-12 rounded-full" style="perspective: 300px">
            <div
              class="absolute inset-0 rounded-full"
              style="
                background: radial-gradient(
                  circle at 32% 26%,
                  rgba(255, 255, 255, 0.28),
                  rgba(255, 255, 255, 0) 58%
                );
                box-shadow: inset 0 0 12px rgba(120, 140, 200, 0.14);
              "
            />
            <div
              class="heart-spin absolute inset-0 flex items-center justify-center"
              style="transform-style: preserve-3d; opacity: 0.9"
            >
              <div class="relative" style="width: 0; height: 0; transform-style: preserve-3d">
                <svg
                  v-for="layer in heartLayers"
                  :key="layer.key"
                  width="34"
                  height="31"
                  viewBox="0 0 100 92"
                  class="absolute top-1/2 left-1/2"
                  :style="{
                    transform: `translate(-50%, -50%) translateZ(${layer.z.toFixed(2)}px)`,
                  }"
                >
                  <path
                    :d="HEART_PATH"
                    :fill="`hsl(${layer.hue} 92% 66%)`"
                    :fill-opacity="layer.isFace ? 0.55 : 0.14"
                    :stroke="layer.isFace ? 'rgba(255,255,255,0.85)' : 'none'"
                    :stroke-width="layer.isFace ? 2 : 0"
                  />
                </svg>
                <svg
                  width="34"
                  height="31"
                  viewBox="0 0 100 92"
                  class="absolute top-1/2 left-1/2"
                  :style="{ transform: `translate(-50%, -50%) translateZ(${heartGlintZ}px)` }"
                >
                  <ellipse
                    cx="36"
                    cy="30"
                    rx="11"
                    ry="6"
                    fill="#ffffff"
                    opacity="0.7"
                    transform="rotate(-30 36 30)"
                  />
                </svg>
              </div>
            </div>
            <div
              class="pointer-events-none absolute inset-0 rounded-full"
              style="
                background:
                  radial-gradient(
                    circle at 30% 24%,
                    rgba(255, 255, 255, 0.92),
                    rgba(255, 255, 255, 0) 20%
                  ),
                  radial-gradient(
                    circle at 72% 76%,
                    rgba(255, 255, 255, 0.4),
                    rgba(255, 255, 255, 0) 26%
                  );
                box-shadow:
                  inset 0 0 0 1px rgba(255, 255, 255, 0.4),
                  inset 0 -8px 14px rgba(90, 110, 170, 0.12),
                  inset 0 6px 12px rgba(255, 255, 255, 0.25);
              "
            />
          </div>
        </div>

        <div ref="partnerDescriptionRef" class="relative">
          <button
            type="button"
            class="block cursor-pointer"
            :aria-expanded="openDescription === 'partner'"
            @click="toggleDescription('partner')"
          >
            <AnimatedCharacter
              :src="partner.character"
              :alt="partner.label"
              :delay="1"
              img-class="h-36 w-36 object-contain"
            />
          </button>
          <div
            v-if="openDescription === 'partner'"
            role="tooltip"
            class="absolute top-full left-1/2 z-10 mt-2 w-[180px] -translate-x-1/2 rounded-[12px] bg-[#d5fae7] px-3 py-3 text-center text-[14px] leading-[1.5] font-medium tracking-[-0.3px] text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
          >
            <span
              class="absolute -top-1.5 left-1/2 h-3 w-3 -translate-x-1/2 rotate-45 bg-[#d5fae7]"
            />
            <p class="relative font-bold">{{ partner.label }}</p>
            <p class="relative mt-0.5">{{ partner.description }}</p>
          </div>
        </div>
      </div>

      <!-- 안정 ~ 도전 슬라이더: 안정 쪽=민트, 도전 쪽=노랑 그라데이션. 낮은 위치·높은 위치에
           각각 점을 찍고, 호버하면 이름·성향이 뜬다 -->
      <div class="mt-7 mx-auto w-64">
        <div class="flex items-center justify-between">
          <span class="text-[16px] font-bold text-ink">안정</span>
          <span class="text-[16px] font-bold text-ink">도전</span>
        </div>
        <div class="relative mt-3 h-1.5 rounded-full bg-line-card">
          <div
            class="absolute top-0 h-full rounded-full"
            style="background: linear-gradient(to right, #78f2dc, #c3f29c, #fff44f)"
            :style="{
              left: `${lowerPosition}%`,
              width: `${higherPosition - lowerPosition}%`,
            }"
          />
          <span
            class="group absolute top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 cursor-default rounded-full border-[3px] border-[#3fc9ae] bg-[#bff5ea]"
            style="box-shadow: 0 2px 5px rgba(0, 0, 0, 0.18)"
            :style="{ left: lowerKnobLeft }"
          >
            <span
              class="pointer-events-none absolute bottom-full left-1/2 mb-2 -translate-x-1/2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-[11px] font-medium text-white opacity-0 transition-opacity group-hover:opacity-100"
            >
              {{ lowerPerson.name }}님 · {{ lowerPerson.label }}
            </span>
          </span>
          <span
            class="group absolute top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 cursor-default rounded-full border-[3px] border-[#e0c400] bg-[#fff9b3]"
            style="box-shadow: 0 2px 5px rgba(0, 0, 0, 0.18)"
            :style="{ left: higherKnobLeft }"
          >
            <span
              class="pointer-events-none absolute top-full left-1/2 mt-2 -translate-x-1/2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-[11px] font-medium text-white opacity-0 transition-opacity group-hover:opacity-100"
            >
              {{ higherPerson.name }}님 · {{ higherPerson.label }}
            </span>
          </span>
        </div>
      </div>

      <!-- 점수 상세 분석 -->
      <div class="mt-8">
        <h2 class="text-[18px] font-bold text-ink">점수 상세 분석</h2>
        <p class="mt-1 text-[12px] text-muted">각 점수의 근거를 확인하세요.</p>

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
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-[9px]"
                  :class="scoreIconMeta[axis.key]?.iconClass"
                >
                  <component :is="scoreIconMeta[axis.key]?.icon" :size="19" :stroke-width="2" />
                </span>
                <span class="text-[18px] font-bold text-ink">{{ axis.name }}</span>
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
              <p class="text-[16px] leading-[1.6] text-ink-sub">{{ axis.reason }}</p>

              <!-- 목표 달성 가능성 카드 전용: 초과/부족 진행 현황 -->
              <div v-if="axis.key === 'GOAL_FEASIBILITY' && goalProgress" class="mt-5">
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

                <div
                  class="relative mt-3 h-10 w-4/5 mx-auto overflow-hidden rounded-full bg-line-card"
                >
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
                      {{ goalProgress.achieved ? '100%' : goalProgress.achievementRate }}
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

<style scoped>
.heart-spin {
  animation: heart-spin-y 6s linear infinite;
}
.heart-float {
  animation: heart-float 4.8s ease-in-out infinite;
}
@keyframes heart-spin-y {
  from {
    transform: rotateY(0deg);
  }
  to {
    transform: rotateY(360deg);
  }
}
@keyframes heart-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}
</style>
