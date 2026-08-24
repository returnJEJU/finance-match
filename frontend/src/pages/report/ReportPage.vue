<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import {
  ArrowRight,
  ChevronDown,
  ChevronUp,
  BadgeDollarSign,
  Landmark,
  HeartHandshake,
  Target,
  Lightbulb,
  TriangleAlert,
} from 'lucide-vue-next'

import { getReport, getReportStatus } from '@/api/report'
import AnimatedCharacter from '@/components/ui/AnimatedCharacter.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import characterExcited from '@/assets/images/characters/character-excited.png'
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
    label: '찰떡궁합형',
    description: '금융을 바라보는 기준과 방향이 꼭 닮은 찰떡궁합이에요.',
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

// 상세 분석 카드 5개의 고정 순서/키. report.scoreDetails 로딩 전에도 closedKeys(전체 접힘) 초기값을
// 만들어야 해서 정적으로 둔다 — 실제 표시 데이터는 scoreDetailSections(아래, report 로딩 후 계산)에서 온다.
const scoreDetailKeys = [
  'ASSET_STABILITY',
  'FINANCIAL_VALUE',
  'DEBT_REPAYMENT',
  'GOAL_FEASIBILITY',
  'TAX_STRATEGY',
]

const ASSET_AXIS_MIN_MAX = 1
const ASSET_AXIS_TICK_COUNT = 5
const ASSET_LABEL_INSIDE_MIN_PROGRESS = 70

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

// report.scoreAxes(점수·만점)와 report.scoreDetails(항목별 상세 데이터)를 합쳐서 카드 하나로 만든다.
const scoreAxisByKey = computed(
  () => new Map(report.value?.scoreAxes?.map((axis) => [axis.key, axis]) ?? []),
)

const scoreSection = (key, type, detail) => {
  const axis = scoreAxisByKey.value.get(key)
  return {
    key,
    type,
    title: axis?.name ?? '',
    score: axis?.score ?? 0,
    maxScore: axis?.maxScore ?? 0,
    ...detail,
  }
}

const scoreDetailSections = computed(() => {
  const details = report.value?.scoreDetails
  if (!details) {
    return []
  }

  return [
    scoreSection('ASSET_STABILITY', 'asset', details.asset),
    scoreSection('FINANCIAL_VALUE', 'investment', details.investmentValue),
    scoreSection('DEBT_REPAYMENT', 'debt', details.debt),
    scoreSection('GOAL_FEASIBILITY', 'goal', details.goal),
    scoreSection('TAX_STRATEGY', 'tax', details.tax),
  ]
})

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

// 슬라이더 노브 채움색 — 각자 accentColorOf 그대로 쓰면 트랙(회색) 위에서 진하게 붕 떠 보여서,
// 흰색 쪽으로 섞어(lighten) 옅은 파스텔로 만든다. 테두리는 원래 색 그대로 둬서 "옅은 채움 +
// 진한 테두리 링" 패턴을 유지한다(민트/노랑 고정색이었을 때와 같은 스타일).
const lightenHex = (hex, amount) => {
  const num = parseInt(hex.slice(1), 16)
  const mix = (channel) => Math.round(channel + (255 - channel) * amount)
  const r = mix((num >> 16) & 0xff)
  const g = mix((num >> 8) & 0xff)
  const b = mix(num & 0xff)
  return `#${[r, g, b].map((c) => c.toString(16).padStart(2, '0')).join('')}`
}
const knobFillOf = (person) => lightenHex(accentColorOf(person), 0.7)

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

const assetAnimationProgress = ref(0)
const debtAnimationProgress = ref(0)
const goalAnimationProgress = ref(0)
let assetAnimationFrameId = null
let debtAnimationFrameId = null
let goalAnimationFrameId = null

const cancelAssetAnimation = () => {
  if (assetAnimationFrameId) {
    cancelAnimationFrame(assetAnimationFrameId)
    assetAnimationFrameId = null
  }
}

const cancelDebtAnimation = () => {
  if (debtAnimationFrameId) {
    cancelAnimationFrame(debtAnimationFrameId)
    debtAnimationFrameId = null
  }
}

const cancelGoalAnimation = () => {
  if (goalAnimationFrameId) {
    cancelAnimationFrame(goalAnimationFrameId)
    goalAnimationFrameId = null
  }
}

const animateAssetBars = () => {
  cancelAssetAnimation()
  assetAnimationProgress.value = 0

  const duration = 1000
  const startedAt = performance.now()

  const tick = (now) => {
    const elapsed = Math.min((now - startedAt) / duration, 1)
    assetAnimationProgress.value = 1 - Math.pow(1 - elapsed, 3)

    if (elapsed < 1) {
      assetAnimationFrameId = requestAnimationFrame(tick)
    } else {
      assetAnimationFrameId = null
    }
  }

  assetAnimationFrameId = requestAnimationFrame(tick)
}

const animateDebtGauges = () => {
  cancelDebtAnimation()
  debtAnimationProgress.value = 0

  const duration = 1100
  const startedAt = performance.now()

  const tick = (now) => {
    const elapsed = Math.min((now - startedAt) / duration, 1)
    debtAnimationProgress.value = 1 - Math.pow(1 - elapsed, 3)

    if (elapsed < 1) {
      debtAnimationFrameId = requestAnimationFrame(tick)
    } else {
      debtAnimationFrameId = null
    }
  }

  debtAnimationFrameId = requestAnimationFrame(tick)
}

const animateGoalSummary = () => {
  cancelGoalAnimation()
  goalAnimationProgress.value = 0

  const duration = 1000
  const startedAt = performance.now()

  const tick = (now) => {
    const elapsed = Math.min((now - startedAt) / duration, 1)
    goalAnimationProgress.value = 1 - Math.pow(1 - elapsed, 3)

    if (elapsed < 1) {
      goalAnimationFrameId = requestAnimationFrame(tick)
    } else {
      goalAnimationFrameId = null
    }
  }

  goalAnimationFrameId = requestAnimationFrame(tick)
}

onMounted(() => {
  document.addEventListener('click', handleOutsideClick)
})
onUnmounted(() => {
  document.removeEventListener('click', handleOutsideClick)
  cancelAssetAnimation()
  cancelDebtAnimation()
  cancelGoalAnimation()
})

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

// 카드별 펼침 상태(복수 개 동시에 펼칠 수 있음). 상세 리포트는 처음엔 전체 접힘으로 시작한다.
const closedKeys = ref(new Set(scoreDetailKeys))

const isOpen = (key) => !closedKeys.value.has(key)

const roundScore = (score) => Math.round(score)

// AI 코멘트 body의 "**강조**" 구간을 <strong>으로 렌더링하기 위해 텍스트를 세그먼트로 쪼갠다.
// v-html 대신 세그먼트 배열 + v-for로 렌더링해서, LLM이 생성한 텍스트에 임의의 HTML이 섞여도
// 태그로 해석되지 않고 그대로 문자열로만 표시된다(XSS 방지).
const parseBoldSegments = (text) => {
  if (!text) {
    return []
  }
  return text
    .split(/(\*\*[^*]+\*\*)/g)
    .filter((part) => part.length > 0)
    .map((part) => {
      const isBold = part.startsWith('**') && part.endsWith('**')
      return { text: isBold ? part.slice(2, -2) : part, bold: isBold }
    })
}

const aiCommentBodySegments = computed(() =>
  parseBoldSegments(report.value?.scoreDetails?.aiComment?.body),
)

const progressWidth = (value) => `${Math.min(Math.max(value, 0), 100)}%`

const assetAxisMax = (section) => {
  const maxValue = Math.max(section.referenceValue ?? 0, section.currentValue ?? 0)
  return Math.max(ASSET_AXIS_MIN_MAX, Math.ceil(maxValue))
}

const assetValueProgress = (section, value) => ((value ?? 0) / assetAxisMax(section)) * 100

const animatedAssetProgressWidth = (section, value) =>
  progressWidth(assetValueProgress(section, value) * assetAnimationProgress.value)

const isAssetLabelInside = (section, value) =>
  assetValueProgress(section, value) > ASSET_LABEL_INSIDE_MIN_PROGRESS

const formatAssetAxisLabel = (value) => {
  if (value === 0) {
    return '0'
  }
  return `${Number(value.toFixed(2)).toLocaleString('ko-KR')}억`
}

const assetAxisTicks = (section) => {
  const max = assetAxisMax(section)
  return Array.from({ length: ASSET_AXIS_TICK_COUNT }, (_, index) => {
    const value = (max / (ASSET_AXIS_TICK_COUNT - 1)) * index
    return {
      label: formatAssetAxisLabel(value),
      position: (value / max) * 100,
    }
  })
}

const assetTickLabelClass = (position) => {
  if (position === 0) {
    return 'translate-x-0'
  }
  if (position === 100) {
    return '-translate-x-full'
  }
  return '-translate-x-1/2'
}

const debtStatusClass = (status) => {
  if (status === '안정') {
    return 'text-[#35a853]'
  }
  if (status === '주의') {
    return 'text-[#f28b22]'
  }
  return 'text-[#e05252]'
}

const animatedAssetValue = (value) => `${(value * assetAnimationProgress.value).toFixed(2)}억원`

const debtGaugeStyle = (progress) => ({
  background: `conic-gradient(from 270deg, #ff9f22 0deg ${progress * debtAnimationProgress.value * 1.8}deg, #eeeeee ${progress * debtAnimationProgress.value * 1.8}deg 180deg, transparent 180deg 360deg)`,
})

const animatedDebtValue = (gauge) => {
  const value = gauge.value * debtAnimationProgress.value
  return `${value.toFixed(gauge.decimals)}${gauge.unit}`
}

const goalDetail = computed(() => report.value?.scoreDetails?.goal)

// 슬라이더는 사용자가 직접 조작하는 로컬 상태라 ref가 필요하다 — goalDetail은 report 로딩 후에야
// 값이 생기므로, 도착하는 시점에 실제 현재 저축액(selectedMonthlySaving)으로 한 번 맞춰준다.
const goalMonthlySaving = ref(0)
watch(
  goalDetail,
  (detail) => {
    if (detail) {
      goalMonthlySaving.value = detail.selectedMonthlySaving
    }
  },
  { immediate: true },
)

const goalSavingRange = computed(() => {
  const detail = goalDetail.value
  return detail ? detail.maxMonthlySaving - detail.minMonthlySaving : 0
})

const goalSavingRatio = computed(() => {
  const detail = goalDetail.value
  if (!detail || goalSavingRange.value === 0) {
    return 0
  }
  return (goalMonthlySaving.value - detail.minMonthlySaving) / goalSavingRange.value
})

const goalSimulation = computed(() => {
  const section = goalDetail.value
  if (!section) {
    return {
      achievement: 0,
      shortage: 0,
    }
  }

  const selectedMonthlySaving = section.selectedMonthlySaving
  const savingDifference = goalMonthlySaving.value - selectedMonthlySaving

  if (savingDifference === 0) {
    return {
      achievement: section.baseAchievement,
      shortage: section.baseShortage,
    }
  }

  const isIncreasing = savingDifference > 0
  const simulationRange = isIncreasing
    ? section.maxMonthlySaving - selectedMonthlySaving
    : selectedMonthlySaving - section.minMonthlySaving

  if (simulationRange <= 0) {
    return {
      achievement: section.baseAchievement,
      shortage: section.baseShortage,
    }
  }

  const ratio = Math.min(Math.abs(savingDifference) / simulationRange, 1)
  const achievementGap = section.maxAchievement - section.baseAchievement
  const shortageGap = section.baseShortage - section.minShortage

  if (isIncreasing) {
    return {
      achievement: section.baseAchievement + achievementGap * ratio,
      shortage: section.baseShortage - shortageGap * ratio,
    }
  }

  return {
    achievement: Math.max(section.baseAchievement - achievementGap * ratio, 0),
    shortage: section.baseShortage + shortageGap * ratio,
  }
})

const goalSimulatedAchievement = computed(() => Math.round(goalSimulation.value.achievement))

const goalSimulatedShortage = computed(() => Math.round(goalSimulation.value.shortage))

const goalSliderPercent = computed(() => progressWidth(goalSavingRatio.value * 100))

const goalSliderTrackStyle = computed(() => ({
  background: `linear-gradient(to right, #fff44f 0%, #fff44f ${goalSliderPercent.value}, #e6e6e6 ${goalSliderPercent.value}, #e6e6e6 100%)`,
}))

const formatManwon = (amount) => {
  const rounded = Math.round(amount)
  const eok = Math.floor(rounded / 10000)
  const manwon = rounded % 10000

  if (eok === 0) {
    return `${manwon.toLocaleString('ko-KR')}만원`
  }
  return manwon === 0
    ? `${eok.toLocaleString('ko-KR')}억원`
    : `${eok.toLocaleString('ko-KR')}억 ${manwon.toLocaleString('ko-KR')}만원`
}
const formatNegativeManwon = (amount) => `-${formatManwon(amount)}`

const normalizeMoneyLabel = (label) =>
  label.replace(/(\d[\d,]*)만원/g, (_, value) => {
    const manwon = Number(value.replaceAll(',', ''))
    return formatManwon(manwon)
  })

const animatedGoalShortage = (amount) => formatNegativeManwon(amount * goalAnimationProgress.value)

const animatedGoalProgress = (progress) =>
  Math.min(Math.max(progress * goalAnimationProgress.value, 0), 100)

const animatedGoalProgressWidth = (progress) => progressWidth(animatedGoalProgress(progress))

const animatedGoalProgressClipPath = (progress) => ({
  clipPath: `inset(0 ${100 - animatedGoalProgress(progress)}% 0 0)`,
})

const animatedGoalRate = (progress) => Math.round(progress * goalAnimationProgress.value)

const taxStatusClass = (status) => {
  if (status === '활용') {
    return 'text-[#22b85a]'
  }
  if (status === '미개설') {
    return 'text-[#ff4f73]'
  }
  return 'text-[#6f5bd5]'
}

const investmentDifferenceValue = (match) => {
  const value = Number.parseInt(match, 10)
  return Number.isNaN(value) ? 0 : value
}

const investmentDifferenceLevel = (match) => {
  const value = investmentDifferenceValue(match)

  if (match.includes('%p')) {
    if (value <= 25) {
      return 'low'
    }
    if (value <= 75) {
      return 'medium'
    }
    return 'high'
  }

  if (value <= 1) {
    return 'low'
  }
  if (value <= 3) {
    return 'medium'
  }
  return 'high'
}

const investmentDifferenceClass = (match) => {
  const level = investmentDifferenceLevel(match)

  if (level === 'low') {
    return 'text-[#22b85a]'
  }
  if (level === 'medium') {
    return 'text-[#f28b22]'
  }
  return 'text-[#ff4b1f]'
}

const showInvestmentDifferenceAlert = (match) => investmentDifferenceLevel(match) !== 'low'

const debtThresholdMarkerStyle = (threshold) => {
  const angle = 180 - threshold * 1.8
  const radian = (angle * Math.PI) / 180
  const x = 66 + 51 * Math.cos(radian)
  const y = 66 - 51 * Math.sin(radian)

  return {
    left: `${x}px`,
    top: `${y}px`,
    transform: `translate(-50%, -50%) rotate(${90 - angle}deg)`,
  }
}

const debtThresholdLabelStyle = (threshold) => {
  const angle = 180 - threshold * 1.8
  const radian = (angle * Math.PI) / 180
  const x = 66 + 51 * Math.cos(radian)
  const y = 66 - 51 * Math.sin(radian)

  return {
    left: `${x}px`,
    top: `${Math.max(y - 20, 0)}px`,
  }
}

const playCardAnimation = (key) => {
  if (key === 'ASSET_STABILITY') {
    animateAssetBars()
    return
  }
  if (key === 'DEBT_REPAYMENT') {
    animateDebtGauges()
    return
  }
  if (key === 'GOAL_FEASIBILITY') {
    animateGoalSummary()
  }
}

const toggleCard = (key) => {
  const next = new Set(closedKeys.value)
  if (next.has(key)) {
    next.delete(key)
    playCardAnimation(key)
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
          class="absolute top-full left-1/2 z-10 mt-2 w-[220px] -translate-x-1/2 rounded-[12px] bg-mint px-3 py-3 text-center text-[14px] leading-[1.5] font-medium tracking-[-0.3px] text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
        >
          <span class="absolute -top-1.5 left-1/2 h-3 w-3 -translate-x-1/2 rotate-45 bg-mint" />
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
            class="absolute top-full left-1/2 z-10 mt-2 w-[180px] -translate-x-1/2 rounded-[12px] bg-mint px-3 py-3 text-center text-[14px] leading-[1.5] font-medium tracking-[-0.3px] text-ink shadow-[0_5px_14px_rgba(0,0,0,0.1)]"
          >
            <span class="absolute -top-1.5 left-1/2 h-3 w-3 -translate-x-1/2 rotate-45 bg-mint" />
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
            :style="{
              backgroundImage: `linear-gradient(to right, ${accentColorOf(lowerPerson)}, ${accentColorOf(higherPerson)})`,
              left: `${lowerPosition}%`,
              width: `${higherPosition - lowerPosition}%`,
            }"
          />
          <span
            class="group absolute top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 cursor-default rounded-full border-[3px]"
            :style="{
              left: lowerKnobLeft,
              borderColor: accentColorOf(lowerPerson),
              backgroundColor: knobFillOf(lowerPerson),
              boxShadow: '0 2px 5px rgba(0, 0, 0, 0.18)',
            }"
          >
            <span
              class="pointer-events-none absolute bottom-full left-1/2 mb-2 -translate-x-1/2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-[11px] font-medium text-white opacity-0 transition-opacity group-hover:opacity-100"
            >
              {{ lowerPerson.name }}님 · {{ lowerPerson.label }}
            </span>
          </span>
          <span
            class="group absolute top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 cursor-default rounded-full border-[3px]"
            :style="{
              left: higherKnobLeft,
              borderColor: accentColorOf(higherPerson),
              backgroundColor: knobFillOf(higherPerson),
              boxShadow: '0 2px 5px rgba(0, 0, 0, 0.18)',
            }"
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
        <h2 class="text-[21px] font-bold text-ink">점수 상세 분석</h2>
        <p class="mt-1 text-[15px] text-muted">항목별 우리 커플의 금융 현황을 확인하세요.</p>

        <div class="mt-4 space-y-3">
          <div
            v-for="section in scoreDetailSections"
            :key="section.key"
            class="rounded-card border border-line-card bg-white p-4"
          >
            <button
              type="button"
              class="flex w-full items-center justify-between"
              @click="toggleCard(section.key)"
            >
              <span class="flex items-center gap-2.5">
                <span
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-[9px]"
                  :class="scoreIconMeta[section.key]?.iconClass"
                >
                  <component :is="scoreIconMeta[section.key]?.icon" :size="19" :stroke-width="2" />
                </span>
                <span class="text-[18px] font-bold text-ink">{{ section.title }}</span>
              </span>
              <span class="flex items-center gap-1.5">
                <span class="text-[17px] font-bold text-ink">
                  {{ roundScore(section.score)
                  }}<span class="text-muted">/{{ section.maxScore }}</span>
                </span>
                <ChevronUp v-if="isOpen(section.key)" :size="16" class="text-muted" />
                <ChevronDown v-else :size="16" class="text-muted" />
              </span>
            </button>

            <div v-if="isOpen(section.key)" class="mt-4">
              <div v-if="section.type === 'asset'">
                <div class="grid grid-cols-[66px_minmax(0,1fr)] gap-x-2">
                  <div class="relative h-[98px] text-[16px] font-bold leading-none">
                    <p class="absolute top-[20px] left-0 -translate-y-1/2 text-ink-sub">
                      {{ section.referenceLabel }}
                    </p>
                    <p class="absolute top-[59px] left-0 -translate-y-1/2 text-ink">
                      {{ section.currentLabel }}
                    </p>
                  </div>

                  <div class="min-w-0">
                    <div class="relative h-[98px]">
                      <span
                        class="absolute left-0 top-0 h-[72px] border-l border-[#d5dbe4]"
                        aria-hidden="true"
                      />
                      <span
                        class="absolute left-1/2 top-0 h-[72px] border-l border-dashed border-[#9fa4ac]"
                        aria-hidden="true"
                      />
                      <span
                        class="absolute left-0 right-0 top-[72px] border-t border-[#d5dbe4]"
                        aria-hidden="true"
                      />

                      <div class="absolute left-0 right-0 top-[8px] h-6">
                        <div
                          class="flex h-full items-center justify-end overflow-hidden rounded-[4px] bg-[#f8f6da] pr-2"
                          :style="{
                            width: animatedAssetProgressWidth(section, section.referenceValue),
                          }"
                        >
                          <span
                            v-if="isAssetLabelInside(section, section.referenceValue)"
                            class="min-w-0 truncate whitespace-nowrap text-right text-[15px] font-extrabold text-ink-sub"
                          >
                            {{ animatedAssetValue(section.referenceValue) }}
                          </span>
                        </div>
                        <span
                          v-if="!isAssetLabelInside(section, section.referenceValue)"
                          class="absolute top-1/2 ml-2 -translate-y-1/2 whitespace-nowrap text-[15px] font-extrabold text-ink-sub"
                          :style="{
                            left: animatedAssetProgressWidth(section, section.referenceValue),
                          }"
                        >
                          {{ animatedAssetValue(section.referenceValue) }}
                        </span>
                      </div>

                      <div class="absolute left-0 right-0 top-[47px] h-6">
                        <div
                          class="flex h-full items-center justify-end overflow-hidden rounded-[4px] bg-[#7fbd72] pr-2"
                          :style="{
                            width: animatedAssetProgressWidth(section, section.currentValue),
                          }"
                        >
                          <span
                            v-if="isAssetLabelInside(section, section.currentValue)"
                            class="min-w-0 truncate whitespace-nowrap text-right text-[15px] font-extrabold text-ink"
                          >
                            {{ animatedAssetValue(section.currentValue) }}
                          </span>
                        </div>
                        <span
                          v-if="!isAssetLabelInside(section, section.currentValue)"
                          class="absolute top-1/2 ml-2 -translate-y-1/2 whitespace-nowrap text-[15px] font-extrabold text-ink"
                          :style="{
                            left: animatedAssetProgressWidth(section, section.currentValue),
                          }"
                        >
                          {{ animatedAssetValue(section.currentValue) }}
                        </span>
                      </div>

                      <span
                        v-for="tick in assetAxisTicks(section)"
                        :key="tick.label"
                        class="absolute top-[72px] h-2 border-l border-[#c9cfd8]"
                        :style="{ left: progressWidth(tick.position) }"
                        aria-hidden="true"
                      />
                      <span
                        v-for="tick in assetAxisTicks(section)"
                        :key="`${tick.label}-label`"
                        class="absolute top-[82px] whitespace-nowrap text-[14px] font-medium text-[#969daa]"
                        :class="assetTickLabelClass(tick.position)"
                        :style="{ left: progressWidth(tick.position) }"
                      >
                        {{ tick.label }}
                      </span>
                    </div>
                  </div>
                </div>
                <div
                  class="mx-auto mt-4 flex w-fit items-center gap-1.5 rounded-full bg-[#fff8db] px-3 py-1 text-[14px] font-semibold text-brand-ink"
                >
                  <Lightbulb :size="14" :stroke-width="2.2" class="shrink-0 text-brand-ink" />
                  {{ section.note }}
                </div>
              </div>

              <div v-else-if="section.type === 'debt'">
                <p class="text-[16px] font-bold text-[#4a4a4a]">
                  {{ normalizeMoneyLabel(section.summary) }}
                </p>
                <div class="mt-5 grid grid-cols-2 divide-x divide-line-soft">
                  <div
                    v-for="gauge in section.gauges"
                    :key="gauge.label"
                    class="min-w-0 px-2 first:pl-0 last:pr-0"
                  >
                    <div class="min-h-[76px]">
                      <p class="text-center text-[16px] font-extrabold text-ink">
                        {{ gauge.label }}
                      </p>
                      <p class="mt-1 text-center text-[13px] leading-[1.35] text-muted">
                        {{ gauge.description }}
                      </p>
                      <p
                        v-if="gauge.thresholdLabel"
                        class="mt-0.5 text-center text-[13px] font-bold text-muted"
                      >
                        기준 {{ gauge.thresholdLabel }}
                      </p>
                      <p v-else class="mt-0.5 text-center text-[13px] font-bold text-transparent">
                        기준 없음
                      </p>
                    </div>

                    <div class="relative mx-auto mt-2 h-[98px] w-[132px] max-w-full">
                      <div class="relative mx-auto h-[80px] w-[132px] max-w-full overflow-hidden">
                        <div
                          class="absolute top-0 left-1/2 h-[132px] w-[132px] max-w-[132px] -translate-x-1/2 rounded-full"
                          :style="debtGaugeStyle(gauge.progress)"
                        />
                        <div
                          class="absolute top-[27px] left-1/2 h-[78px] w-[78px] -translate-x-1/2 rounded-full bg-white"
                        />

                        <template v-if="gauge.threshold">
                          <span
                            class="absolute z-[1] h-8 w-[3px] rounded-full bg-[#ff6b6b] shadow-[0_0_0_2px_rgba(255,255,255,0.85)]"
                            :style="debtThresholdMarkerStyle(gauge.threshold)"
                            aria-hidden="true"
                          />
                          <span
                            class="absolute z-[1] -translate-x-1/2 rounded-full bg-white px-1.5 text-[13px] font-extrabold text-[#ff6b6b] shadow-[0_1px_3px_rgba(0,0,0,0.08)]"
                            :style="debtThresholdLabelStyle(gauge.threshold)"
                          >
                            {{ gauge.thresholdLabel }}
                          </span>
                        </template>
                      </div>

                      <div class="pointer-events-none absolute inset-x-0 top-[46px] text-center">
                        <p class="text-[21px] font-extrabold text-ink">
                          {{ animatedDebtValue(gauge) }}
                        </p>
                        <p
                          class="mt-1 text-[18px] font-extrabold"
                          :class="debtStatusClass(gauge.status)"
                        >
                          {{ gauge.status }}
                        </p>
                      </div>

                      <div
                        class="absolute inset-x-0 bottom-1 flex justify-between text-[13px] font-bold text-muted"
                      >
                        <span>0%</span>
                        <span>100%</span>
                      </div>
                    </div>

                    <div class="mt-3 border-t border-line-soft pt-3 text-center">
                      <p class="text-[12px] font-semibold text-muted">{{ gauge.amountLabel }}</p>
                      <p class="mt-1 text-[17px] font-extrabold text-ink">
                        {{ normalizeMoneyLabel(gauge.amount) }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div v-else-if="section.type === 'investment'">
                <div class="grid grid-cols-[1.55fr_0.8fr_0.8fr_0.9fr] text-[15px]">
                  <span class="pb-3 text-center font-bold text-muted">항목</span>
                  <span class="pb-3 text-center font-bold text-muted">{{
                    section.columns[0]
                  }}</span>
                  <span class="pb-3 text-center font-bold text-muted">{{
                    section.columns[1]
                  }}</span>
                  <span class="pb-3 text-center font-bold text-muted">차이</span>

                  <template v-for="row in section.rows" :key="row.label">
                    <div class="flex items-center justify-center border-t border-line-soft py-4">
                      <span class="text-[16px] font-extrabold text-ink">{{ row.label }}</span>
                    </div>
                    <div class="flex items-center justify-center border-t border-line-soft py-4">
                      <span
                        class="rounded-full px-3 py-1 text-[16px] font-extrabold"
                        :class="
                          row.me.includes('%')
                            ? 'bg-[#d9f8e5] text-[#1f8b4b]'
                            : 'bg-[#f4f4f6] text-ink'
                        "
                      >
                        {{ row.me }}
                      </span>
                    </div>
                    <div class="flex items-center justify-center border-t border-line-soft py-4">
                      <span
                        class="rounded-full px-3 py-1 text-[16px] font-extrabold"
                        :class="
                          row.partner === '0%'
                            ? 'bg-[#ffe0e0] text-[#ff4b1f]'
                            : 'bg-[#f4f4f6] text-ink'
                        "
                      >
                        {{ row.partner }}
                      </span>
                    </div>
                    <div
                      class="flex items-center justify-center gap-1.5 border-t border-line-soft py-4 text-center text-[17px] font-extrabold"
                      :class="investmentDifferenceClass(row.match)"
                    >
                      <TriangleAlert
                        v-if="showInvestmentDifferenceAlert(row.match)"
                        :size="13"
                        :stroke-width="2.4"
                        class="shrink-0"
                      />
                      {{ row.match }}
                    </div>
                  </template>
                </div>
              </div>

              <div v-else-if="section.type === 'goal'">
                <p class="text-[16px] font-bold text-warn">{{ section.shortageLabel }}</p>
                <div class="mt-2">
                  <p
                    class="break-keep text-[31px] font-extrabold leading-tight tracking-normal text-warn"
                  >
                    {{ animatedGoalShortage(section.shortageValue) }}
                  </p>
                  <div class="mt-1 text-right text-[14px] font-extrabold leading-snug text-muted">
                    목표 금액 {{ normalizeMoneyLabel(section.targetAmount) }}
                  </div>
                </div>
                <div
                  class="relative mt-2 h-8 overflow-hidden rounded-full bg-line-card text-[14px] font-extrabold"
                >
                  <div
                    class="absolute inset-y-0 left-0 rounded-full bg-warn"
                    :style="{ width: animatedGoalProgressWidth(section.progress) }"
                    aria-hidden="true"
                  />
                  <span class="absolute inset-0 flex items-center justify-center text-ink">
                    예상 달성률 {{ animatedGoalRate(section.progress) }}%
                  </span>
                  <span
                    class="absolute inset-0 flex items-center justify-center text-white"
                    :style="animatedGoalProgressClipPath(section.progress)"
                  >
                    예상 달성률 {{ animatedGoalRate(section.progress) }}%
                  </span>
                </div>
                <div class="mt-5 flex items-baseline gap-2">
                  <span class="text-[15px] font-semibold text-muted">예상 가용자산</span>
                  <span class="text-[28px] font-extrabold text-ink">{{
                    normalizeMoneyLabel(section.availableAsset)
                  }}</span>
                </div>

                <div class="mt-8">
                  <p class="text-[16px] font-extrabold text-ink">월 저축액을 옮겨 보세요</p>
                  <div class="relative mt-4 px-3">
                    <input
                      v-model.number="goalMonthlySaving"
                      type="range"
                      class="goal-saving-range w-full"
                      :min="section.minMonthlySaving"
                      :max="section.maxMonthlySaving"
                      step="10"
                      :style="goalSliderTrackStyle"
                      aria-label="월 저축액 조정"
                    />
                  </div>
                  <div class="mt-4 grid grid-cols-3 items-start text-[14px] text-muted">
                    <span
                      >최소 월 저축액<br /><b class="text-[17px] text-ink">{{
                        normalizeMoneyLabel(section.monthlySaving)
                      }}</b></span
                    >
                    <span class="text-center"
                      >월 저축액<br /><b class="text-[17px] text-ink">{{
                        formatManwon(goalMonthlySaving)
                      }}</b></span
                    >
                    <span class="text-right"
                      >최대 월 저축액<br /><b class="text-[17px] text-ink">{{
                        formatManwon(section.maxMonthlySaving)
                      }}</b></span
                    >
                  </div>
                </div>

                <div class="mt-6 space-y-0 border-t border-line-soft text-[15px]">
                  <div class="flex items-center justify-between">
                    <span class="py-4 font-semibold text-muted">예상 달성률</span>
                    <span class="py-4 font-extrabold">
                      <span class="text-[#c9c9c9]">{{ section.baseAchievement }}%</span>
                      <span class="mx-1 text-[#c9c9c9]">→</span>
                      <span class="text-[#35b978]">{{ goalSimulatedAchievement }}%</span>
                    </span>
                  </div>
                  <div class="flex items-center justify-between border-t border-line-soft">
                    <span class="py-4 font-semibold text-muted">부족한 금액</span>
                    <span class="py-4 font-extrabold">
                      <span class="text-[#c9c9c9]">{{
                        formatNegativeManwon(section.baseShortage)
                      }}</span>
                      <span class="mx-1 text-[#c9c9c9]">→</span>
                      <span class="text-[#e05252]">{{
                        formatNegativeManwon(goalSimulatedShortage)
                      }}</span>
                    </span>
                  </div>
                </div>
              </div>

              <div v-else-if="section.type === 'tax'">
                <div class="grid grid-cols-[1fr_1fr_1fr] gap-y-4 text-center text-[15px]">
                  <span class="font-semibold text-muted">항목</span>
                  <span
                    v-for="column in section.columns"
                    :key="column"
                    class="font-semibold text-muted"
                  >
                    {{ column }}
                  </span>
                  <template v-for="row in section.rows" :key="row.label">
                    <span class="text-[16px] font-extrabold text-ink">{{ row.label }}</span>
                    <span class="text-[16px] font-extrabold" :class="taxStatusClass(row.me)">
                      {{ row.me }}
                    </span>
                    <span class="text-[16px] font-extrabold" :class="taxStatusClass(row.partner)">
                      {{ row.partner }}
                    </span>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <div class="relative rounded-card border border-line-card bg-white p-4">
            <div class="flex items-center gap-2.5">
              <span
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-[9px] bg-[#fff5eb] text-[#9c5f27]"
              >
                <HeartHandshake :size="19" :stroke-width="2" />
              </span>
              <h3 class="text-[18px] font-bold text-ink">
                {{ report.scoreDetails.aiComment.title }}
              </h3>
            </div>
            <div class="mt-4 pb-16">
              <p class="text-[16px] font-extrabold text-warn">
                {{ report.scoreDetails.aiComment.headline }}
              </p>
              <p class="mt-2 whitespace-pre-line text-[15px] leading-[1.65] text-ink-sub">
                <template v-for="(segment, index) in aiCommentBodySegments" :key="index">
                  <strong v-if="segment.bold" class="font-bold text-ink">{{ segment.text }}</strong>
                  <template v-else>{{ segment.text }}</template>
                </template>
              </p>
            </div>
            <AnimatedCharacter
              :src="characterExcited"
              alt="AI 코멘트 캐릭터"
              img-class="absolute bottom-2 right-2 h-20 w-20 object-contain"
            />
          </div>
        </div>

        <BaseButton class="mt-5 w-full" to="/recommend">
          추천 상품 보러가기
          <ArrowRight :size="20" :stroke-width="2.5" />
        </BaseButton>
      </div>
    </template>
  </section>
</template>

<style scoped>
.goal-saving-range {
  height: 10px;
  appearance: none;
  border-radius: 9999px;
  outline: none;
}
.goal-saving-range::-webkit-slider-thumb {
  width: 24px;
  height: 24px;
  appearance: none;
  cursor: pointer;
  background: #ffffff;
  border: 5px solid #fff44f;
  border-radius: 9999px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.16);
}
.goal-saving-range::-moz-range-thumb {
  width: 24px;
  height: 24px;
  cursor: pointer;
  background: #ffffff;
  border: 5px solid #fff44f;
  border-radius: 9999px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.16);
}
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
