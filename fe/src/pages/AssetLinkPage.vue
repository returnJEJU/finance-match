<script setup>
// 회원가입 - 자산 불러오기 (4/4) · 레이아웃: BlankLayout
//
// 어떤 자산을 불러올지 고르는 화면. 실제 마이데이터 연동은 붙이지 않는다.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, CreditCard, Info, Landmark, TrendingUp } from 'lucide-vue-next'
import characterWorking from '@/assets/images/characters/character-working.png'

// 불러올 자산 종류. icon 은 lucide 컴포넌트, tint 는 아이콘 칩 배경색.
const ASSET_TYPES = [
  { key: 'bank', name: '은행', desc: '입출금 · 예적금', icon: Landmark, tint: '#FFF3D6' },
  { key: 'stock', name: '증권', desc: '주식 · 펀드', icon: TrendingUp, tint: '#FFE9E9' },
  { key: 'loan', name: '대출', desc: '대출내역', icon: CreditCard, tint: '#E8EEFF' },
]

const router = useRouter()

// 기본값은 모두 켬 — 디자인 기준
const selected = ref(Object.fromEntries(ASSET_TYPES.map((type) => [type.key, true])))

const allSelected = computed(() => ASSET_TYPES.every((type) => selected.value[type.key]))

function toggleAll() {
  const next = !allSelected.value
  selected.value = Object.fromEntries(ASSET_TYPES.map((type) => [type.key, next]))
}

function toggle(key) {
  selected.value = { ...selected.value, [key]: !selected.value[key] }
}

/**
 * 뒤로 가기.
 *
 * 주소를 직접 열거나 새로고침하면 앱 안에 돌아갈 기록이 없어서
 * router.back() 이 앱 밖(브라우저 이전 페이지)으로 나가버린다.
 * 그래서 기록이 없으면 퍼널의 이전 단계로 보낸다.
 */
function goBack() {
  if (window.history.state?.back) router.back()
  else router.replace({ name: 'signup-cert' })
}
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <!-- 상단 진행바 — 4단계 중 4단계 -->
    <div class="flex flex-none items-center gap-2.5 px-7 pt-3.5 pb-1.5">
      <button
        type="button"
        class="w-6 flex-none cursor-pointer"
        aria-label="뒤로 가기"
        @click="goBack"
      >
        <ChevronLeft class="h-5 w-5" />
      </button>
      <div class="bg-line-card h-1 flex-1 overflow-hidden rounded-full">
        <i class="bg-brand-deep block h-full w-full rounded-full"></i>
      </div>
      <span class="text-muted-soft flex-none text-[12px] font-medium">4/4</span>
    </div>

    <div class="flex flex-1 flex-col px-7">
      <img :src="characterWorking" alt="" class="mt-1.5 w-32 self-center" />

      <h1 class="mt-2 text-center text-[26px] leading-[1.38] font-extrabold tracking-[-0.4px]">
        임민지님의 자산<br />한 번에 찾아볼게요
      </h1>
      <p class="text-muted mt-2 text-center text-[13px] leading-[1.5]">
        흩어져 있는 금융 정보를 하나로 합쳐<br />완벽한 찰떡귱합 리포트를 만들어요.
      </p>

      <!-- 전체 선택 -->
      <div class="mt-5.5 mb-2 flex items-center">
        <span class="text-[13px] font-extrabold">불러올 자산</span>
        <span class="flex-1"></span>
        <span class="text-ink-sub mr-2 text-[12px] font-semibold">전체 선택</span>
        <button
          type="button"
          class="relative h-[22px] w-[38px] flex-none cursor-pointer rounded-full transition-colors"
          :class="allSelected ? 'bg-brand-deep' : 'bg-[#E2E2E4]'"
          :aria-label="allSelected ? '전체 해제' : '전체 선택'"
          @click="toggleAll"
        >
          <i
            class="absolute top-[3px] h-4 w-4 rounded-full bg-white shadow-[0_1px_3px_rgba(0,0,0,0.22)] transition-all"
            :class="allSelected ? 'left-[19px]' : 'left-[3px]'"
          ></i>
        </button>
      </div>

      <!-- 자산 종류 목록 -->
      <div class="border-line-card rounded-card border bg-white px-4 py-1">
        <template v-for="(type, index) in ASSET_TYPES" :key="type.key">
          <hr v-if="index > 0" class="border-line-soft border-t" />
          <div class="flex items-center gap-3 py-[13px]">
            <span
              class="flex h-[38px] w-[38px] flex-none items-center justify-center rounded-[11px]"
              :style="{ backgroundColor: type.tint }"
            >
              <component :is="type.icon" class="text-ink h-[19px] w-[19px]" />
            </span>
            <span>
              <span class="block text-[14px] font-bold">{{ type.name }}</span>
              <span class="text-muted mt-0.5 block text-[11.5px]">{{ type.desc }}</span>
            </span>
            <span class="flex-1"></span>
            <button
              type="button"
              class="relative h-[27px] w-[46px] flex-none cursor-pointer rounded-full transition-colors"
              :class="selected[type.key] ? 'bg-brand-deep' : 'bg-[#E2E2E4]'"
              :aria-label="`${type.name} ${selected[type.key] ? '해제' : '선택'}`"
              @click="toggle(type.key)"
            >
              <i
                class="absolute top-[3px] h-[21px] w-[21px] rounded-full bg-white shadow-[0_1px_3px_rgba(0,0,0,0.22)] transition-all"
                :class="selected[type.key] ? 'left-[22px]' : 'left-[3px]'"
              ></i>
            </button>
          </div>
        </template>
      </div>

      <p class="text-muted mt-3.5 flex gap-[7px] text-[11px] leading-[1.55]">
        <Info class="mt-px h-[13px] w-[13px] flex-none" />
        <span>
          불러온 정보는 <b class="text-ink font-bold">분석 목적으로만</b> 사용되며, 상대방에게는
          궁합 결과만 공유돼요.
        </span>
      </p>

      <div class="h-6 flex-1"></div>
    </div>

    <div class="flex flex-none flex-col gap-2.5 px-7 pb-14">
      <RouterLink
        :to="{ name: 'signup-asset-linking' }"
        class="bg-brand rounded-card flex h-[54px] cursor-pointer items-center justify-center gap-[7px] text-[16px] font-bold transition-transform duration-100 active:scale-[0.98]"
      >
        자산 불러오기
      </RouterLink>

      <RouterLink
        :to="{ name: 'signup-asset-institutions' }"
        class="border-line-field rounded-card flex h-[54px] cursor-pointer items-center justify-center border bg-white text-[16px] font-semibold text-[#444] transition-transform duration-100 active:scale-[0.98]"
      >
        기관 직접 선택
      </RouterLink>
    </div>
  </div>
</template>
