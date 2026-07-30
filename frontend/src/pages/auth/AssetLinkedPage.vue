<script setup>
// 자산 연동 완료 · 레이아웃: BlankLayout
//
// 금액·기관 수는 모두 디자인 값 그대로다. 실제 연동이 붙으면 서버 응답으로 바꾼다.
import { ChevronRight, CreditCard, Landmark, Lock, TrendingUp, Wallet } from 'lucide-vue-next'
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterExcited from '@/assets/images/characters/character-excited.png'
import BaseButton from '@/components/ui/BaseButton.vue'

// 자산 구성 막대와 범례. percent 합은 100.
const COMPOSITION = [
  { key: 'saving', label: '예적금', percent: 60, bar: '#7FD8D3', dot: '#5FC4BE' },
  { key: 'invest', label: '투자', percent: 25, bar: '#FAE64D', dot: '#C9A800' },
  { key: 'cash', label: '계좌·현금', percent: 15, bar: '#F5A8C0', dot: '#E58AA8' },
]

// 불러온 내역
const ITEMS = [
  {
    key: 'cash',
    name: '계좌·현금',
    icon: Wallet,
    tint: '#FFF3D6',
    desc: '토스뱅크 통장 외 10개',
    amount: '12,630,000원',
  },
  {
    key: 'saving',
    name: '예적금',
    icon: Landmark,
    tint: '#E3F7E8',
    desc: '주택청약종합저축 외 2개',
    amount: '50,520,000원',
  },
  {
    key: 'invest',
    name: '투자',
    icon: TrendingUp,
    tint: '#FFE9E9',
    desc: '삼성전기 외 2개',
    amount: '21,050,000원',
  },
  {
    key: 'loan',
    name: '대출',
    icon: CreditCard,
    tint: '#E8EEFF',
    desc: '연결된 대출이 없어요',
    amount: '0원',
  },
]
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <div class="flex flex-none justify-center px-7 pt-4 pb-3">
      <img :src="logoWordmark" alt="찰떡귱합" class="w-24" />
    </div>

    <div class="flex flex-1 flex-col px-7">
      <img :src="characterExcited" alt="" class="mt-6 w-[136px] self-center" />

      <h1 class="mt-0.5 text-center text-[21px] leading-[1.38] font-extrabold tracking-[-0.4px]">
        자산 연동 완료!
      </h1>
      <p class="text-muted mt-1.5 text-center text-[13px] leading-[1.5]">
        <b class="text-ink font-bold">17개 기관</b>을 한 번에 불러왔어요
      </p>

      <!-- 총 자산 -->
      <div class="border-line-card rounded-card mt-6 border bg-white px-4 py-4">
        <div class="flex items-center">
          <span class="text-ink-sub rounded-md bg-[#F2F2F2] px-2.5 py-1 text-[11px] font-bold">
            총 자산
          </span>
          <span class="flex-1"></span>
          <span class="text-[25px] font-extrabold tracking-[-0.8px]">
            84,200,000<span class="text-[17px]">원</span>
          </span>
        </div>

        <!-- 구성 비율 막대 -->
        <div class="mt-3.5 flex h-2 overflow-hidden rounded-full">
          <i
            v-for="part in COMPOSITION"
            :key="part.key"
            class="block h-full"
            :style="{ width: `${part.percent}%`, backgroundColor: part.bar }"
          ></i>
        </div>

        <div class="text-ink-sub mt-2.5 flex gap-3.5 text-[11px]">
          <span v-for="part in COMPOSITION" :key="part.key" class="flex items-center gap-1">
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
        <template v-for="(item, index) in ITEMS" :key="item.key">
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
              <span class="text-muted mt-0.5 block truncate text-[11.5px]">{{ item.desc }}</span>
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
      <BaseButton :to="{ name: 'couple-start' }">
        찰떡귱합 시작하기
        <ChevronRight class="h-[18px] w-[18px]" />
      </BaseButton>
    </div>
  </div>
</template>
