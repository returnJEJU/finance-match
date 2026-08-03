<script setup>
import { computed, ref } from 'vue'
import {
  ArrowUpRight,
  Briefcase,
  CarFront,
  Check,
  Flame,
  Heart,
  House,
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
  favoriteState: {
    type: Boolean,
    default: null,
  },
  current: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'toggle-favorite'])

const localFavorite = ref(false)
const isFavorite = computed(() =>
  props.favoriteState === null ? localFavorite.value : props.favoriteState,
)

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

const toggleFavorite = () => {
  if (props.favoriteState === null) {
    localFavorite.value = !localFavorite.value
    return
  }
  emit('toggle-favorite', props.product.productId)
}
</script>

<template>
  <article
    class="relative cursor-pointer rounded-card bg-white"
    :class="[
      compact
        ? roomy
          ? 'px-4 py-6 shadow-[0_2px_12px_rgba(0,0,0,0.04)]'
          : 'p-4 shadow-[0_2px_12px_rgba(0,0,0,0.04)]'
        : 'py-5',
      compact && current ? 'border-2 border-[#FFF56E]' : compact ? 'border border-line-card' : '',
    ]"
    @click="emit('select')"
  >
    <div class="flex items-start gap-3">
      <div
        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg"
        :class="productIconClass"
      >
        <component
          :is="productIcon"
          class="h-[22px] w-[22px]"
          :class="slotType === 'SAVINGS' ? 'fill-[#F7A8B8]' : ''"
          stroke-width="2"
        />
      </div>

      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2">
          <h3 class="min-w-0 flex-1 text-[15px] leading-5 font-bold text-ink">
            {{ product.productName }}
          </h3>

          <button
            v-if="listMode && slotType !== 'INVESTMENT'"
            type="button"
            class="flex min-w-14 shrink-0 items-center justify-center"
            :aria-label="isFavorite ? '찜 해제' : '찜하기'"
            :aria-pressed="isFavorite"
            @click.stop="toggleFavorite"
          >
            <Heart
              class="h-4 w-4 transition-colors"
              :class="
                isFavorite ? 'fill-[#fb7185] text-[#fb7185]' : 'fill-transparent text-gray-300'
              "
            />
          </button>
          <div
            v-else-if="slotType === 'INVESTMENT' && product.riskLabel"
            class="flex w-14 shrink-0 justify-center"
          >
            <span
              class="rounded-md border px-1.5 py-px text-center text-[10px] font-semibold"
              :class="riskBadgeClass"
            >
              {{ product.riskLabel }}
            </span>
          </div>
          <span
            v-else-if="product.comparisonValue"
            class="min-w-14 shrink-0 text-center text-[12px] leading-5 font-bold text-ink"
          >
            {{ product.comparisonValue }}
          </span>
          <div v-else-if="product.riskLabel" class="flex w-14 shrink-0 justify-center">
            <span
              class="rounded-md border px-1.5 py-px text-center text-[10px] font-semibold"
              :class="riskBadgeClass"
            >
              {{ product.riskLabel }}
            </span>
          </div>
        </div>

        <p
          v-if="product.description"
          class="mt-1 flex items-start gap-1 text-[10px] leading-4 text-muted"
        >
          <Check class="mt-0.5 h-3 w-3 shrink-0 text-muted" stroke-width="2.5" />
          <span>{{ product.description }}</span>
        </p>

        <div class="mt-3 flex items-center">
          <button
            type="button"
            class="inline-flex items-center gap-1 border-b border-ink text-[10px] font-bold text-ink"
            @click.stop="openProduct(product.productUrl)"
          >
            상품 상세 보기
            <ArrowUpRight class="h-3 w-3" stroke-width="2.5" />
          </button>

          <div
            v-if="!listMode || slotType === 'INVESTMENT'"
            class="absolute flex min-w-14 -translate-y-1/2 items-center justify-center"
            :class="
              listMode && slotType === 'INVESTMENT'
                ? 'top-[58px] right-4'
                : compact
                  ? 'top-1/2 right-4'
                  : 'top-1/2 right-0'
            "
          >
            <button
              type="button"
              class="flex min-w-14 items-center justify-center"
              :aria-label="isFavorite ? '찜 해제' : '찜하기'"
              :aria-pressed="isFavorite"
              @click.stop="toggleFavorite"
            >
              <Heart
                class="h-4 w-4 transition-colors"
                :class="
                  isFavorite ? 'fill-[#fb7185] text-[#fb7185]' : 'fill-transparent text-gray-300'
                "
              />
            </button>
          </div>
        </div>

        <div v-if="listMode" class="mt-4 -ml-[52px] flex min-h-5 items-center">
          <div v-if="slotType === 'INVESTMENT' && formattedAum" class="flex items-baseline gap-1.5">
            <span class="text-[10px] text-muted">순자산</span>
            <strong class="text-[12px] text-ink">{{ formattedAum }}</strong>
          </div>
          <div v-else-if="product.comparisonValue" class="flex items-baseline gap-1.5">
            <span class="text-[10px] text-muted">{{ product.comparisonLabel }}</span>
            <strong class="text-[12px] text-ink">{{ product.comparisonValue }}</strong>
          </div>
          <span
            v-else-if="product.riskLabel"
            class="rounded-md border px-1.5 py-px text-center text-[10px] font-semibold"
            :class="riskBadgeClass"
          >
            {{ product.riskLabel }}
          </span>
          <span
            v-if="current"
            class="ml-auto rounded bg-ink px-1.5 py-0.5 text-[9px] font-semibold text-[#FFF56E]"
          >
            현재 대표
          </span>
        </div>
      </div>
    </div>
  </article>
</template>
