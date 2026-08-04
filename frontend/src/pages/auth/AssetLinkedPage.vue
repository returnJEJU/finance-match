<script setup>
// 자산 연동 완료 · 레이아웃: BlankLayout
//
// 금액·구성비는 연동 응답에서 그린다. 응답은 assetLinkStore 가 앞 화면에서 넘겨준다 —
// 자산 조회 API 가 없고(연동·갱신뿐), 카테고리별 금액은 응답을 만들 때만 쪼개 주는 값이라
// DB 에도 남지 않아서 이 화면이 스스로 다시 불러올 수 없다.
import { computed } from 'vue'
import { ChevronRight, CreditCard, Landmark, Lock, TrendingUp, Wallet } from 'lucide-vue-next'
import { useAssetLinkStore } from '@/stores/assetLink'
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterExcited from '@/assets/images/characters/character-excited.png'
import BaseButton from '@/components/ui/BaseButton.vue'

/** 카테고리별 표시 정보. 순서가 화면에 나오는 순서다. */
const ASSET_ROWS = [
  {
    key: 'BANK_CHECKING',
    name: '계좌·현금',
    icon: Wallet,
    tint: '#FFF3D6',
    bar: '#F5A8C0',
    dot: '#E58AA8',
  },
  {
    key: 'BANK_SAVINGS',
    name: '예적금',
    icon: Landmark,
    tint: '#E3F7E8',
    bar: '#7FD8D3',
    dot: '#5FC4BE',
  },
  {
    key: 'SECURITIES',
    name: '투자',
    icon: TrendingUp,
    tint: '#FFE9E9',
    bar: '#FAE64D',
    dot: '#C9A800',
  },
  {
    key: 'INSURANCE',
    name: '보험',
    icon: Landmark,
    tint: '#EDE9FE',
    bar: '#C4B5FD',
    dot: '#A78BFA',
  },
]

const assetLinkStore = useAssetLinkStore()

// 스토어가 빈 채로 들어오는 경우는 라우터의 beforeEnter 가 막는다.
// 화면이 뜬 뒤(onMounted)에 이동을 걸면 진행 중인 내비게이션과 충돌해 주소만 바뀐다.

const result = computed(() => assetLinkStore.result)

const won = (amount) => `${Number(amount).toLocaleString('ko-KR')}원`

/** 카테고리 → 금액. 응답에 없는 카테고리는 0 으로 둔다. */
const amountByCategory = computed(() => {
  const map = {}
  for (const row of ASSET_ROWS) map[row.key] = 0
  for (const item of result.value?.summary ?? []) map[item.category] = item.amount
  return map
})

/**
 * 자산 구성 막대·범례.
 *
 * 금액이 0 인 카테고리는 빼서 0% 범례가 늘어서지 않게 한다.
 */
const composition = computed(() => {
  const total = result.value?.totalAsset ?? 0
  if (total <= 0) return []

  return ASSET_ROWS.filter((row) => amountByCategory.value[row.key] > 0).map((row) => ({
    ...row,
    label: row.name,
    percent: Math.round((amountByCategory.value[row.key] / total) * 1000) / 10,
  }))
})

/** 불러온 내역. 자산 4종 + 대출 한 줄. */
const items = computed(() => [
  ...ASSET_ROWS.filter((row) => amountByCategory.value[row.key] > 0).map((row) => ({
    key: row.key,
    name: row.name,
    icon: row.icon,
    tint: row.tint,
    amount: won(amountByCategory.value[row.key]),
  })),
  {
    key: 'loan',
    name: '대출',
    icon: CreditCard,
    tint: '#E8EEFF',
    amount: won(result.value?.totalDebt ?? 0),
  },
])
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <div class="flex flex-none justify-center px-7 pt-4 pb-3">
      <img :src="logoWordmark" alt="찰떡귱합" class="w-24" />
    </div>

    <div class="flex flex-1 flex-col px-7">
      <img :src="characterExcited" alt="" class="mt-6 w-[136px] self-center" />

      <h1 class="mt-0.5 text-center text-[21px] leading-[1.38] font-extrabold tracking-[-0.4px]">
        자산 연동 완료!
      </h1>
      <p class="text-muted mt-1.5 text-center text-[13px] leading-[1.5]">
        <b class="text-ink font-bold">계좌 {{ result?.assetCount ?? 0 }}개</b>를 한 번에 불러왔어요
      </p>

      <!-- 총 자산 -->
      <div class="border-line-card rounded-card mt-6 border bg-white px-4 py-4">
        <div class="flex items-center">
          <span class="text-ink-sub rounded-md bg-[#F2F2F2] px-2.5 py-1 text-[11px] font-bold">
            총 자산
          </span>
          <span class="flex-1"></span>
          <span class="text-[25px] font-extrabold tracking-[-0.8px]">
            {{ Number(result?.totalAsset ?? 0).toLocaleString('ko-KR')
            }}<span class="text-[17px]">원</span>
          </span>
        </div>

        <!-- 구성 비율 막대 -->
        <div class="mt-3.5 flex h-2 overflow-hidden rounded-full">
          <i
            v-for="part in composition"
            :key="part.key"
            class="block h-full"
            :style="{ width: `${part.percent}%`, backgroundColor: part.bar }"
          ></i>
        </div>

        <div class="text-ink-sub mt-2.5 flex gap-3.5 text-[11px]">
          <span v-for="part in composition" :key="part.key" class="flex items-center gap-1">
            <i class="h-1.5 w-1.5 rounded-full" :style="{ backgroundColor: part.dot }"></i>
            {{ part.label }}
            <b class="text-ink font-bold">{{ part.percent }}%</b>
          </span>
        </div>
      </div>

      <!-- 불러온 내역 -->
      <div class="mt-6.5 mb-0.5 flex items-center">
        <span class="text-[13px] font-extrabold">불러온 내역</span>
        <span class="flex-1"></span>
        <button type="button" class="text-muted flex cursor-pointer items-center text-[11.5px]">
          전체 보기
          <ChevronRight class="h-3.5 w-3.5" />
        </button>
      </div>

      <!-- 항목이 늘어나도 화면이 길어지지 않도록 이 영역만 스크롤한다 -->
      <div class="max-h-[180px] overflow-y-auto pr-3">
        <template v-for="(item, index) in items" :key="item.key">
          <hr v-if="index > 0" class="border-line-soft border-t" />
          <div class="flex items-center gap-3 py-[11px]">
            <span
              class="flex h-[38px] w-[38px] flex-none items-center justify-center rounded-[11px]"
              :style="{ backgroundColor: item.tint }"
            >
              <component :is="item.icon" class="text-ink h-[19px] w-[19px]" />
            </span>
            <span class="min-w-0">
              <span class="block text-[14px] font-bold">{{ item.name }}</span>
            </span>
            <span class="flex-1"></span>
            <span class="text-[15px] font-extrabold tracking-[-0.3px]">{{ item.amount }}</span>
          </div>
        </template>
      </div>

      <div class="flex-1"></div>

      <p class="text-muted flex gap-[7px] pb-3 text-[11px] leading-[1.55]">
        <Lock class="mt-px h-[13px] w-[13px] flex-none" />
        <span>
          연동된 정보는 파트너와 함께 실시간으로 업데이트되며,
          <b class="text-ink font-bold">개인 식별 정보는 암호화</b>되어 안전하게 관리됩니다.
        </span>
      </p>
    </div>

    <div class="flex flex-none flex-col px-7 pb-14">
      <!--
        여기가 회원가입 퍼널의 끝이다. 다음은 로그인으로 보낸다 — 어느 화면으로 갈지 정하는 재료
        (isFirstLogin·progress)가 로그인 응답에만 있어서, 로그인을 거쳐야 첫 로그인으로 인식되어
        서비스 소개부터 흐른다.

        가입 때 받은 토큰은 지우지 않는다. 로그인하면 어차피 교체되고, 지우면 뒤로 가기로 이
        화면을 다시 볼 수 없게 된다.
      -->
      <BaseButton :to="{ name: 'login' }">
        찰떡귱합 시작하기
        <ChevronRight class="h-[18px] w-[18px]" />
      </BaseButton>
    </div>
  </div>
</template>
