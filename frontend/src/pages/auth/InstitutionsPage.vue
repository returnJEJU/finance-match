<script setup>
// 회원가입 - 기관 직접 선택 · 레이아웃: BlankLayout
//
// 07 자산 불러오기에서 '기관 직접 선택'을 눌렀을 때 오는 화면.
// 진행바 대신 가운데 제목을 쓴다(디자인 기준).
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, ChevronLeft } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'

// 기관 목록. mark 는 로고 대신 쓰는 머리글자, color 는 각 사의 브랜드 색.
const GROUPS = [
  {
    key: 'bank',
    label: '은행',
    items: [
      { key: 'kb', name: '국민은행', mark: 'KB', color: '#FFB800' },
      { key: 'shinhan', name: '신한은행', mark: '신', color: '#0046FF' },
      { key: 'hana', name: '하나은행', mark: '하', color: '#00857E' },
      { key: 'nh', name: '농협은행', mark: 'N', color: '#00A64F' },
      { key: 'kakaobank', name: '카카오뱅크', mark: 'k', color: '#FFDE00' },
      { key: 'tossbank', name: '토스뱅크', mark: 'T', color: '#3182F6' },
    ],
  },
  {
    key: 'stock',
    label: '증권',
    items: [
      { key: 'kbsec', name: 'KB증권', mark: 'KB', color: '#FFB800' },
      { key: 'mirae', name: '미래에셋', mark: '미', color: '#D71920' },
      { key: 'samsungsec', name: '삼성증권', mark: '삼', color: '#0046FF' },
    ],
  },
  {
    key: 'card',
    label: '카드',
    items: [
      { key: 'shinhancard', name: '신한카드', mark: '신', color: '#0046FF' },
      { key: 'hyundaicard', name: '현대카드', mark: '현', color: '#000000' },
      { key: 'lottecard', name: '롯데카드', mark: '롯', color: '#DA291C' },
    ],
  },
]

const ALL_KEYS = GROUPS.flatMap((group) => group.items.map((item) => item.key))

const router = useRouter()

// 디자인 기준 기본 선택 상태 (8개)
const selected = ref(
  new Set(['kb', 'shinhan', 'hana', 'nh', 'kakaobank', 'kbsec', 'mirae', 'shinhancard']),
)

const selectedCount = computed(() => selected.value.size)
const allSelected = computed(() => selected.value.size === ALL_KEYS.length)

function toggle(key) {
  const next = new Set(selected.value)
  next.has(key) ? next.delete(key) : next.add(key)
  selected.value = next
}

function toggleAll() {
  selected.value = allSelected.value ? new Set() : new Set(ALL_KEYS)
}

/**
 * 뒤로 가기.
 *
 * 주소를 직접 열거나 새로고침하면 앱 안에 돌아갈 기록이 없어서
 * router.back() 이 앱 밖으로 나가버린다. 그때는 자산 불러오기 화면으로 보낸다.
 */
function goBack() {
  if (window.history.state?.back) router.back()
  else router.replace({ name: 'signup-asset' })
}
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <!-- 상단바 — 진행바 없이 가운데 제목 -->
    <div class="flex flex-none items-center px-7 pt-4 pb-2.5">
      <button
        type="button"
        class="w-6 flex-none cursor-pointer"
        aria-label="뒤로 가기"
        @click="goBack"
      >
        <ChevronLeft class="h-5 w-5" />
      </button>
      <span class="flex-1 text-center text-[15px] font-bold">기관 직접 선택</span>
      <!-- 뒤로가기 버튼과 같은 폭. 제목을 정확히 가운데 두면서 버튼을 덮지 않게 한다. -->
      <span class="w-6 flex-none"></span>
    </div>

    <div class="flex flex-1 flex-col px-7">
      <!-- 전체 선택 -->
      <div class="flex items-center py-3">
        <span class="text-muted text-[13px]">연결할 기관을 골라주세요</span>
        <span class="flex-1"></span>
        <button
          type="button"
          class="text-muted flex cursor-pointer items-center gap-[7px] text-[13px] font-bold"
          @click="toggleAll"
        >
          <span
            class="flex h-5 w-5 items-center justify-center rounded-full border-[1.5px]"
            :class="
              allSelected
                ? 'bg-brand-deep border-brand-deep text-ink'
                : 'border-[#DADADA] text-transparent'
            "
          >
            <Check class="h-3 w-3" stroke-width="3.5" />
          </span>
          전체 선택
        </button>
      </div>

      <!-- 분류별 기관 그리드 -->
      <div v-for="group in GROUPS" :key="group.key">
        <p class="mt-4 mb-2.5 text-[13px] font-extrabold">
          {{ group.label }}
          <span class="text-muted-soft font-semibold">{{ group.items.length }}</span>
        </p>

        <div class="grid grid-cols-3 gap-2">
          <button
            v-for="item in group.items"
            :key="item.key"
            type="button"
            class="rounded-card relative cursor-pointer bg-white px-2.5 py-3.5 text-center transition-transform duration-100 active:scale-[0.97]"
            :class="
              selected.has(item.key)
                ? 'border-brand-deep border-[1.6px]'
                : 'border-line-card border'
            "
            @click="toggle(item.key)"
          >
            <span
              v-if="selected.has(item.key)"
              class="bg-brand-deep text-ink absolute top-2 right-2 flex h-[19px] w-[19px] items-center justify-center rounded-full"
            >
              <Check class="h-2.5 w-2.5" stroke-width="3.5" />
            </span>
            <span
              class="mx-auto mb-[7px] flex h-[34px] w-[34px] items-center justify-center rounded-full text-[14px] font-extrabold text-white"
              :style="{ backgroundColor: item.color }"
            >
              {{ item.mark }}
            </span>
            <span class="block text-[12px] font-semibold">{{ item.name }}</span>
          </button>
        </div>
      </div>

      <div class="h-6 flex-1"></div>
    </div>

    <div class="flex flex-none flex-col gap-2.5 px-7 pb-14">
      <p class="text-muted text-center text-[12.5px]">
        <b class="text-ink font-bold">{{ selectedCount }}개 기관</b> 선택됨
      </p>

      <BaseButton @click="goBack"> 선택하기 </BaseButton>
    </div>
  </div>
</template>
