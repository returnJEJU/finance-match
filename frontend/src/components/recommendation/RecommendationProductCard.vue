<script setup>
import { computed } from 'vue'
import {
  ArrowUpRight,
  Briefcase,
  CarFront,
  Check,
  Flame,
  House,
  Lightbulb,
  Building2,
  PiggyBank,
  Rocket,
  Scale,
  ShieldCheck,
  Store,
  TrendingUp,
  Umbrella,
  WalletCards,
} from 'lucide-vue-next'

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
  slotType: {
    type: String,
    default: '',
  },
  compact: {
    type: Boolean,
    default: false,
  },
  roomy: {
    type: Boolean,
    default: false,
  },
  listMode: {
    type: Boolean,
    default: false,
  },
  current: {
    type: Boolean,
    default: false,
  },
  recommended: {
    type: Boolean,
    default: false,
  },
  showRecommendationReason: {
    type: Boolean,
    default: false,
  },
  personalMode: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select'])

const productIcon = computed(() => {
  if (props.slotType === 'TAX_SAVING') {
    const taxIcons = {
      ISA: WalletCards,
      PENSION_SAVINGS: Umbrella,
      IRP: Briefcase,
    }
    return taxIcons[props.product.accountType] ?? WalletCards
  }
  if (props.slotType === 'DEPOSIT') return Building2
  if (props.slotType === 'SAVINGS') return PiggyBank
  if (props.slotType === 'LOAN') {
    const loanIcons = {
      JEONSE: House,
      HOUSING: House,
      CAR: CarFront,
      BUSINESS: Store,
    }
    return loanIcons[props.product.loanPurpose] ?? House
  }
  if (props.slotType !== 'INVESTMENT' && !props.product.riskLabel) return Building2

  const icons = {
    안정: ShieldCheck,
    중립: Scale,
    위험: TrendingUp,
    고위험: Rocket,
    초고위험: Flame,
  }

  return icons[props.product.riskLabel] ?? TrendingUp
})

const productIconClass = computed(() => {
  const classes = {
    안정: 'bg-[#ECF8EF] text-[#2E7D43]',
    중립: 'bg-[#F5F9E8] text-[#708522]',
    위험: 'bg-[#FFF7E8] text-[#B97008]',
    고위험: 'bg-[#FFF0EB] text-[#C94B27]',
    초고위험: 'bg-[#FFF0F1] text-[#C93643]',
  }

  if (props.slotType === 'TAX_SAVING') {
    const taxColors = {
      ISA: 'bg-[#EEF5FF] text-[#3B6FB6]',
      PENSION_SAVINGS: 'bg-[#ECF8EF] text-[#2E7D43]',
      IRP: 'bg-[#F4EFFF] text-[#7357B4]',
    }
    return taxColors[props.product.accountType] ?? 'bg-surface-muted text-ink'
  }
  if (props.slotType === 'DEPOSIT') return 'bg-[#EEF5FF] text-[#3B6FB6]'
  if (props.slotType === 'SAVINGS') return 'bg-[#FFF0F4] text-[#E66F91]'
  if (props.slotType === 'LOAN') return 'bg-[#FFF5E8] text-[#B97008]'
  if (props.slotType === 'INVESTMENT' || props.product.riskLabel) {
    return classes[props.product.riskLabel] ?? 'bg-surface-muted text-ink'
  }
  return 'bg-surface-muted text-ink'
})

const riskBadgeClass = computed(() => {
  const classes = {
    안정: 'border-[#55A96B] bg-[#ECF8EF] text-[#2E7D43]',
    중립: 'border-[#9CB83C] bg-[#F5F9E8] text-[#708522]',
    위험: 'border-[#E9A23B] bg-[#FFF7E8] text-[#B97008]',
    고위험: 'border-[#EF7955] bg-[#FFF0EB] text-[#C94B27]',
    초고위험: 'border-[#E6535F] bg-[#FFF0F1] text-[#C93643]',
  }

  return classes[props.product.riskLabel] ?? 'border-gray-300 bg-gray-50 text-gray-600'
})

const openProduct = (url) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}

const formattedAum = computed(() =>
  props.product.aum == null ? null : `${props.product.aum.toLocaleString('ko-KR')}억 원`,
)
</script>

<template>
  <article
    class="relative cursor-pointer rounded-card bg-white"
    :class="[
      compact
        ? roomy
          ? 'px-5 py-6 shadow-[0_2px_12px_rgba(0,0,0,0.04)]'
          : 'p-5 shadow-[0_2px_12px_rgba(0,0,0,0.04)]'
        : 'pt-2 pb-6',
      compact && current ? 'border-2 border-[#FFF56E]' : compact ? 'border border-line-card' : '',
    ]"
    @click="emit('select')"
  >
    <div class="flex items-start gap-3">
      <div
        class="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl"
        :class="productIconClass"
      >
        <component
          :is="productIcon"
          class="h-8 w-8"
          :class="slotType === 'SAVINGS' ? 'fill-[#F7A8B8]' : ''"
          stroke-width="2"
        />
      </div>

      <div class="min-w-0 flex-1">
        <div class="flex min-h-14 items-start gap-3">
          <h3
            class="flex min-h-14 min-w-0 flex-1 items-center text-[19px] leading-7 font-bold text-ink"
          >
            {{ product.productName }}
          </h3>

          <span
            v-if="slotType === 'INVESTMENT' && product.riskLabel"
            class="shrink-0 whitespace-nowrap rounded-md border px-2 py-0.5 text-center text-[12px] font-semibold"
            :class="riskBadgeClass"
          >
            {{ product.riskLabel }}
          </span>

          <span
            v-else-if="!listMode && product.comparisonValue"
            class="shrink-0 whitespace-nowrap pt-0.5 text-[15px] leading-5 font-bold text-ink"
          >
            {{ product.comparisonValue }}
          </span>
        </div>

        <p
          v-if="product.description"
          class="mt-2 -ml-[68px] flex items-start gap-2 text-[15px] leading-6 text-muted"
        >
          <Check class="mt-0.5 h-5 w-5 shrink-0 text-muted" stroke-width="2.5" />
          <span>{{ product.description }}</span>
        </p>

        <button
          type="button"
          class="-ml-[48px] inline-flex items-center gap-1.5 border-b border-ink text-[15px] font-bold text-ink"
          :class="personalMode && !showRecommendationReason ? 'mt-7' : 'mt-4'"
          @click.stop="openProduct(product.productUrl)"
        >
          상품 상세 보기
          <ArrowUpRight class="h-5 w-5" stroke-width="2.5" />
        </button>
        <div v-if="listMode" class="mt-7 -ml-[68px] flex min-h-7 items-center">
          <div v-if="slotType === 'INVESTMENT' && formattedAum" class="flex items-baseline gap-2">
            <span class="text-[14px] text-muted">순자산</span>
            <strong class="text-[16px] text-ink">{{ formattedAum }}</strong>
          </div>

          <div v-else-if="product.comparisonValue" class="flex items-baseline gap-2">
            <span class="text-[14px] text-muted">
              {{ product.comparisonLabel }}
            </span>
            <strong class="text-[16px] text-ink">
              {{ product.comparisonValue }}
            </strong>
          </div>

          <span
            class="ml-auto translate-y-2 rounded-md px-2.5 py-1.5 text-[13px] leading-none font-semibold"
            :class="recommended ? 'bg-ink text-[#FFF56E]' : 'bg-[#F3F4F6] text-muted'"
          >
            {{ recommended ? '추천 상품' : '대안 상품' }}
          </span>
        </div>

        <div
          v-if="showRecommendationReason && product.recommendationReason"
          class="mt-4 -ml-[68px] flex items-start gap-1.5 rounded-xl bg-[#FFFBE0] px-3.5 py-3.5 text-[15px] leading-6 text-[#665F18]"
        >
          <Lightbulb class="mt-0.5 h-5 w-5 shrink-0 text-[#C79B00]" stroke-width="2.3" />
          <p>
            <strong class="mr-1 font-bold text-ink">추천 이유</strong>
            {{ product.recommendationReason }}
          </p>
        </div>
      </div>
    </div>
  </article>
</template>
