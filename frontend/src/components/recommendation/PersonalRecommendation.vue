<script setup>
import { computed, ref, watch } from 'vue'
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
const visibleProducts = computed(() =>
  activeType.value === 'tax'
    ? (props.taxSaving?.products ?? [])
    : (props.investment?.products ?? []),
)
</script>

<template>
  <section v-if="hasTaxSaving || hasInvestment" class="mt-7">
    <div class="flex items-center justify-between">
      <h2 class="text-[15px] font-bold">{{ targetName }}님을 위한 추천 목록</h2>
      <div
        v-if="hasBoth"
        class="mr-2 flex overflow-hidden rounded-xl border border-[#BDBDBD] bg-[#F1F1F1] text-[11px]"
      >
        <button
          type="button"
          class="relative min-w-9 px-1.5 py-1.5 transition-colors"
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
          class="relative min-w-9 px-1.5 py-1.5 transition-colors"
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

    <div class="mt-3 space-y-3">
      <RecommendationProductCard
        v-for="product in visibleProducts"
        :key="product.productId"
        :product="product"
        :slot-type="activeType === 'investment' ? 'INVESTMENT' : 'TAX_SAVING'"
        compact
      />
    </div>
  </section>
</template>
