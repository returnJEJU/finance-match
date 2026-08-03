<script setup>
import { computed, ref, watch } from 'vue'
import { RefreshCw, X } from 'lucide-vue-next'
import RecommendationProductCard from '@/components/recommendation/RecommendationProductCard.vue'

const props = defineProps({
  slots: {
    type: Array,
    required: true,
  },
})

const selectedProductIds = ref({})
const activeSlot = ref(null)
const pendingProduct = ref(null)
const favoriteProductIds = ref(new Set())

watch(
  () => props.slots,
  (slots) => {
    const nextSelectedProductIds = { ...selectedProductIds.value }
    slots.forEach((slot) => {
      if (!nextSelectedProductIds[slot.slotId]) {
        nextSelectedProductIds[slot.slotId] = slot.selectedProductId
      }
    })
    selectedProductIds.value = nextSelectedProductIds
  },
  { immediate: true },
)

const selectedProducts = computed(() =>
  props.slots
    .map((slot) => ({
      slot,
      product: slot.products.find(
        ({ productId }) => productId === selectedProductIds.value[slot.slotId],
      ),
    }))
    .filter(({ product }) => Boolean(product)),
)

const currentProduct = computed(() => {
  if (!activeSlot.value) return null
  return activeSlot.value.products.find(
    ({ productId }) => productId === selectedProductIds.value[activeSlot.value.slotId],
  )
})

const openProductList = (slot) => {
  activeSlot.value = slot
}

const closeProductList = () => {
  activeSlot.value = null
  pendingProduct.value = null
}

const requestProductChange = (product) => {
  if (product.productId === selectedProductIds.value[activeSlot.value.slotId]) return
  pendingProduct.value = product
}

const cancelProductChange = () => {
  pendingProduct.value = null
}

const confirmProductChange = () => {
  if (!activeSlot.value || !pendingProduct.value) return

  selectedProductIds.value = {
    ...selectedProductIds.value,
    [activeSlot.value.slotId]: pendingProduct.value.productId,
  }
  pendingProduct.value = null
}

const isFavorite = (productId) => favoriteProductIds.value.has(productId)

const toggleFavorite = (productId) => {
  const nextFavoriteProductIds = new Set(favoriteProductIds.value)
  if (nextFavoriteProductIds.has(productId)) {
    nextFavoriteProductIds.delete(productId)
  } else {
    nextFavoriteProductIds.add(productId)
  }
  favoriteProductIds.value = nextFavoriteProductIds
}

const addAllPackageProducts = () => {
  const nextFavoriteProductIds = new Set(favoriteProductIds.value)
  selectedProducts.value.forEach(({ product }) => nextFavoriteProductIds.add(product.productId))
  favoriteProductIds.value = nextFavoriteProductIds
}
</script>

<template>
  <section class="rounded-[18px] border border-ink bg-[#FFF56E] p-3">
    <div class="mb-2">
      <span class="rounded-full bg-ink px-2.5 py-1 text-[9px] font-semibold text-[#A2F5E6]">
        찰떡 PICK 패키지
      </span>
      <h2 class="mt-2 text-[14px] font-bold">우리 커플을 위한 맞춤 패키지</h2>
      <p class="mt-1 text-[10px] text-brand-ink">
        현재 조건에 맞는 {{ slots.length }}가지 금융상품을 골랐어요
      </p>
    </div>

    <div class="divide-y divide-line-soft rounded-card bg-[#FFFFFF] px-3">
      <RecommendationProductCard
        v-for="{ slot, product } in selectedProducts"
        :key="slot.slotId"
        :product="product"
        :slot-type="slot.slotType"
        :favorite-state="isFavorite(product.productId)"
        @select="openProductList(slot)"
        @toggle-favorite="toggleFavorite"
      />
    </div>

    <button
      type="button"
      class="mt-3 h-11 w-full rounded-xl bg-ink text-xs font-semibold text-white"
      @click="addAllPackageProducts"
    >
      이 패키지 모두 담기
    </button>
  </section>

  <Teleport to="body">
    <div
      v-if="activeSlot"
      class="fixed inset-0 z-50 flex items-end justify-center bg-black/45"
      role="dialog"
      aria-modal="true"
      :aria-label="`${activeSlot.slotName} 상품 목록`"
      @click.self="closeProductList"
    >
      <section
        class="max-h-[82vh] w-full max-w-[393px] overflow-y-auto rounded-t-[24px] bg-white px-4 pt-4 pb-8"
      >
        <header class="flex items-start gap-3">
          <div class="ml-3 min-w-0 flex-1">
            <h2 class="text-[19px] leading-6 font-extrabold">{{ activeSlot.slotName }} 목록</h2>
            <p class="mt-0.5 text-[10px] text-muted">원하는 상품을 선택해 변경할 수 있어요</p>
          </div>
          <button
            type="button"
            class="flex h-7 w-7 items-center justify-center rounded-full bg-gray-100 text-muted"
            aria-label="상품 목록 닫기"
            @click="closeProductList"
          >
            <X class="h-4 w-4" />
          </button>
        </header>

        <div class="mt-5 space-y-4">
          <div v-for="product in activeSlot.products" :key="product.productId">
            <RecommendationProductCard
              :product="product"
              :slot-type="activeSlot.slotType"
              :favorite-state="isFavorite(product.productId)"
              :current="product.productId === selectedProductIds[activeSlot.slotId]"
              compact
              roomy
              list-mode
              @select="requestProductChange(product)"
              @toggle-favorite="toggleFavorite"
            />
          </div>
        </div>
      </section>
    </div>

    <div
      v-if="pendingProduct"
      class="fixed inset-0 z-[60] flex items-center justify-center bg-black/45 px-5"
      role="alertdialog"
      aria-modal="true"
      aria-label="패키지 상품 변경 확인"
    >
      <section
        class="relative w-full max-w-[350px] rounded-[28px] bg-white px-7 pt-7 pb-6 text-center shadow-xl"
      >
        <button
          type="button"
          class="absolute top-4 right-4 flex h-7 w-7 items-center justify-center rounded-full bg-gray-100 text-muted"
          aria-label="상품 변경 취소"
          @click="cancelProductChange"
        >
          <X class="h-4 w-4" />
        </button>

        <div
          class="mx-auto flex h-14 w-14 items-center justify-center rounded-[16px] bg-[#FFF9C7] text-[#4F8EF7]"
        >
          <span class="flex h-8 w-8 items-center justify-center rounded-full bg-white"
            ><RefreshCw class="h-5 w-5"
          /></span>
        </div>
        <h2 class="mt-4 text-[15px] leading-5 font-extrabold">
          패키지 내 상품을<br />
          이 상품으로 변경하시겠습니까?
        </h2>

        <div class="mt-5 rounded-[14px] bg-[#F8F8F8] px-4 py-4 text-left">
          <div class="grid grid-cols-[72px_1fr] items-start gap-2">
            <p class="text-[10px] text-[#9CA3AF]">현재 대표</p>
            <p class="text-[11px] leading-4 font-semibold text-[#9CA3AF] line-through">
              {{ currentProduct?.productName }}
            </p>
          </div>
          <div class="mt-4 grid grid-cols-[72px_1fr] items-start gap-2">
            <p class="text-[10px] font-semibold text-[#35CBAA]">변경 후</p>
            <div>
              <p class="text-[11px] leading-4 font-bold text-ink">
                {{ pendingProduct.productName }}
              </p>
              <p v-if="pendingProduct.description" class="mt-1 text-[9px] leading-4 text-[#B97008]">
                {{ pendingProduct.description }}
              </p>
            </div>
          </div>
        </div>

        <div class="mt-5 grid grid-cols-2 gap-2">
          <button
            type="button"
            class="h-11 rounded-xl border border-line-card text-xs font-semibold text-muted"
            @click="cancelProductChange"
          >
            취소
          </button>
          <button
            type="button"
            class="h-11 rounded-xl bg-brand text-xs font-bold text-ink"
            @click="confirmProductChange"
          >
            변경하기
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>
