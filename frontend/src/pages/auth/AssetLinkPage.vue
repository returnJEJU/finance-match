<script setup>
// 회원가입 - 자산 불러오기 (4/4) · 레이아웃: BlankLayout
//
// 어떤 자산을 불러올지 고르는 화면. 실제 마이데이터 연동은 붙이지 않는다.
import { computed, ref } from 'vue'
import { CreditCard, Info, Landmark, TrendingUp } from 'lucide-vue-next'
import characterWorking from '@/assets/images/characters/character-working.png'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import FunnelHeader from '@/components/layout/FunnelHeader.vue'

// 불러올 자산 종류. icon 은 lucide 컴포넌트, tint 는 아이콘 칩 배경색.
const ASSET_TYPES = [
  { key: 'bank', name: '은행', desc: '입출금 · 예적금', icon: Landmark, tint: '#FFF3D6' },
  { key: 'stock', name: '증권', desc: '주식 · 펀드', icon: TrendingUp, tint: '#FFE9E9' },
  { key: 'loan', name: '대출', desc: '대출내역', icon: CreditCard, tint: '#E8EEFF' },
]

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

const authStore = useAuthStore()
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <FunnelHeader :step="4" :fallback-to="{ name: 'signup-cert' }" />

    <!-- pb-10 은 자산 목록과 아래 버튼 사이 여백이다. 이걸 안쪽 빈 div 로 두면 내용이 길어질 때
         flex 가 찌그러뜨려 0 이 된다 — 패딩은 그런 일이 없다. -->
    <div class="flex flex-1 flex-col px-7 pb-10">
      <img :src="characterWorking" alt="" class="mt-1.5 w-32 self-center" />

      <PageTitle class="mt-2" align="center">
        {{ authStore.member?.name ?? '회원' }}님의 자산<br />한 번에 찾아볼게요
      </PageTitle>
      <p class="text-muted mt-2 text-center text-[14px] leading-[1.5]">
        흩어져 있는 금융 정보를 하나로 합쳐<br />완벽한 찰떡궁합 리포트를 만들어요.
      </p>

      <!-- 전체 선택 -->
      <div class="mt-5.5 mb-2 flex items-center">
        <span class="text-[16px] font-extrabold">불러올 자산</span>
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
              <span class="text-muted mt-0.5 block text-[12px]">{{ type.desc }}</span>
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
    </div>

    <div class="flex flex-none flex-col px-7 pb-7">
      <!-- 두 버튼을 한 줄에 둔다. 위아래로 쌓으면 버튼 영역만 130px 을 넘겨 본문이 눌린다. -->
      <div class="flex gap-2.5">
        <BaseButton class="flex-1" :to="{ name: 'signup-asset-linking' }">자산 불러오기</BaseButton>

        <BaseButton class="flex-1" :to="{ name: 'signup-asset-institutions' }" variant="ghost">
          기관 직접 선택
        </BaseButton>
      </div>

      <!-- 고를 항목이 아니라 안내다. 목록 아래에 두면 항목처럼 보여 버튼 아래로 뺐다. -->
      <p
        class="text-muted mt-3 flex items-center justify-center gap-[7px] text-center text-[11px] leading-[1.55]"
      >
        <Info class="h-[13px] w-[13px] flex-none" />
        <span>
          불러온 정보는 <b class="text-ink font-bold">분석 목적으로만</b> 사용되며, 상대방에게는
          궁합 결과만 공유돼요.
        </span>
      </p>
    </div>
  </div>
</template>
