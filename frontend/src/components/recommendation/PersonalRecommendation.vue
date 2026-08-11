<script setup>
import { computed, ref, watch } from 'vue'
import { ChevronDown } from 'lucide-vue-next'
import RecommendationProductCard from '@/components/recommendation/RecommendationProductCard.vue'

const props = defineProps({
  taxSaving: {
    type: Object,
    default: null,
  },
  investment: {
    type: Object,
    default: null,
  },
})

const hasTaxSaving = computed(() => Boolean(props.taxSaving))
const hasInvestment = computed(() => Boolean(props.investment))
const hasBoth = computed(() => hasTaxSaving.value && hasInvestment.value)
const activeType = ref(hasTaxSaving.value ? 'tax' : 'investment')

watch([hasTaxSaving, hasInvestment], () => {
  if (!hasTaxSaving.value) activeType.value = 'investment'
  if (!hasInvestment.value) activeType.value = 'tax'
})

const targetName = computed(
  () => props.taxSaving?.targetMemberName ?? props.investment?.targetMemberName ?? '',
)
const sortType = ref('recommendation')
const originalProducts = computed(() =>
  activeType.value === 'tax'
    ? (props.taxSaving?.products ?? [])
    : (props.investment?.products ?? []),
)
const recommendedProductId = computed(() => originalProducts.value[0]?.productId ?? null)
const visibleProducts = computed(() => {
  const products = originalProducts.value

  if (sortType.value === 'name') {
    return [...products].sort((a, b) => a.productName.localeCompare(b.productName, 'ko-KR'))
  }

  return products
})
</script>

<template>
  <section v-if="hasTaxSaving || hasInvestment" class="mt-8">
    <div class="flex items-center justify-between gap-3">
      <h2 class="min-w-0 flex-1 text-[18px] leading-6 font-bold">
        {{ targetName }}님을 위한 추천 목록
      </h2>

      <div
        v-if="hasBoth"
        class="mr-1 flex shrink-0 overflow-hidden rounded-xl border border-[#BDBDBD] bg-[#F1F1F1] text-[14px]"
      >
        <button
          type="button"
          class="relative min-w-12 px-2 py-2 transition-colors"
          :class="
            activeType === 'tax'
              ? 'font-bold text-ink before:absolute before:inset-y-0 before:-inset-x-1 before:rounded-xl before:border before:border-[#D8CD3F] before:bg-[#FFF56E]'
              : 'text-ink'
          "
          @click="activeType = 'tax'"
        >
          <span class="relative z-10">절세</span>
        </button>

        <button
          type="button"
          class="relative min-w-12 px-2 py-2 transition-colors"
          :class="
            activeType === 'investment'
              ? 'font-bold text-ink before:absolute before:inset-y-0 before:-inset-x-1 before:rounded-xl before:border before:border-[#D8CD3F] before:bg-[#FFF56E]'
              : 'text-ink'
          "
          @click="activeType = 'investment'"
        >
          <span class="relative z-10">투자</span>
        </button>
      </div>
    </div>

    <div class="mt-4 flex items-center justify-between gap-3">
      <label class="relative inline-flex items-center">
        <span class="sr-only">개인 추천 정렬 기준</span>
        <select
          v-model="sortType"
          class="appearance-none rounded-lg border border-line-card bg-white py-1 pr-8 pl-3 text-[14px] font-semibold text-ink outline-none focus:border-ink"
        >
          <option value="recommendation">추천순</option>
          <option value="name">이름순</option>
        </select>
        <ChevronDown
          class="pointer-events-none absolute right-2.5 h-4 w-4 text-muted"
          stroke-width="2"
        />
      </label>

      <p class="text-[14px] font-semibold text-muted">추천 상품 {{ visibleProducts.length }}개</p>
    </div>

    <div class="mt-3 space-y-4">
      <RecommendationProductCard
        v-for="product in visibleProducts"
        :key="product.productId"
        :product="product"
        :slot-type="activeType === 'investment' ? 'INVESTMENT' : 'TAX_SAVING'"
        :show-recommendation-reason="product.productId === recommendedProductId"
        personal-mode
        compact
      />
    </div>
  </section>
</template>
