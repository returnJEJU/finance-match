<script setup>
// 자산 연동 중 · 레이아웃: BlankLayout
//
// 실제 연동이 없으므로 진행률·금액은 디자인 값 그대로 둔다.
// 타이머로 가짜 진행을 만들지 않고, 대기 화면이 멈춰 보이지 않게 CSS 애니메이션만 넣는다.
import { useRouter } from 'vue-router'
import { CreditCard, Landmark, Lock, TrendingUp, Wallet, X } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'

// 불러오는 항목. done 이 false 면 금액 대신 스켈레톤을 보여준다.
const ITEMS = [
  {
    key: 'cash',
    name: '계좌·현금',
    icon: Wallet,
    tint: '#FFF3D6',
    done: true,
    amount: '12,630,000원',
  },
  {
    key: 'saving',
    name: '예적금',
    icon: Landmark,
    tint: '#E3F7E8',
    done: true,
    amount: '50,520,000원',
  },
  { key: 'invest', name: '투자', icon: TrendingUp, tint: '#FFE9E9', done: false, skeleton: 96 },
  { key: 'loan', name: '대출', icon: CreditCard, tint: '#E8EEFF', done: false, skeleton: 72 },
]

const router = useRouter()

// 연동을 중단하고 이전 화면으로. 앱 안에 기록이 없으면 자산 불러오기로 보낸다.
function close() {
  if (window.history.state?.back) router.back()
  else router.replace({ name: 'signup-asset' })
}
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <div class="flex flex-none items-center px-7 pt-4 pb-1">
      <button
        type="button"
        class="text-muted flex w-6 flex-none cursor-pointer"
        aria-label="연동 중단"
        @click="close"
      >
        <X class="h-5 w-5" />
      </button>
    </div>

    <div class="flex flex-1 flex-col px-7">
      <PageTitle class="mt-3.5">임민지님의 자산을<br />불러오고 있어요</PageTitle>
      <p class="text-muted mt-2 text-[13px] leading-[1.5]">
        금융보안 규격에 따라 안전하게 연결 중이에요.
      </p>

      <!-- 진행률 — 숫자는 고정, 막대만 처음 한 번 차오른다 -->
      <div class="mt-4.5 flex items-center gap-3">
        <span class="text-[30px] font-extrabold tracking-[-1px]">
          40<span class="text-[19px]">%</span>
        </span>
        <div class="bg-line-card h-1.5 flex-1 overflow-hidden rounded-full">
          <i class="bg-brand-deep progress-fill block h-full rounded-full"></i>
        </div>
      </div>

      <!-- 항목별 진행 상태 -->
      <div class="mt-2">
        <template v-for="(item, index) in ITEMS" :key="item.key">
          <hr v-if="index > 0" class="border-line-soft border-t" />
          <div class="flex items-center gap-3 py-3.5">
            <span
              class="flex h-[38px] w-[38px] flex-none items-center justify-center rounded-[11px]"
              :style="{ backgroundColor: item.tint }"
            >
              <component :is="item.icon" class="text-ink h-[19px] w-[19px]" />
            </span>
            <span>
              <span class="block text-[14px] font-bold">{{ item.name }}</span>
              <span
                class="mt-0.5 block text-[11.5px]"
                :class="item.done ? 'text-ink' : 'text-muted'"
              >
                {{ item.done ? '완료' : '찾는 중' }}
              </span>
            </span>
            <span class="flex-1"></span>
            <span v-if="item.done" class="text-[15px] font-extrabold tracking-[-0.3px]">
              {{ item.amount }}
            </span>
            <span
              v-else
              class="skeleton h-[13px] rounded-full"
              :style="{ width: `${item.skeleton}px` }"
            ></span>
          </div>
        </template>
      </div>

      <div class="flex-1"></div>

      <p class="text-muted flex items-center justify-center gap-[7px] pb-3 text-[11px]">
        <Lock class="h-[13px] w-[13px] flex-none" />
        <span>금융위원회 표준 API 연동 중 · <b class="text-ink font-bold">최대 30초</b></span>
      </p>
    </div>

    <div class="flex flex-none flex-col px-7 pb-14">
      <BaseButton variant="disabled">잠시만 기다려 주세요</BaseButton>
    </div>
  </div>
</template>

<style scoped>
/* 진행 막대가 0 에서 40% 까지 한 번 차오른다. 그 뒤로는 움직이지 않는다. */
.progress-fill {
  width: 40%;
  animation: fill-up 0.9s ease-out;
}

@keyframes fill-up {
  from {
    width: 0;
  }
}

/* 아직 못 불러온 항목 자리. 좌우로 빛이 훑고 지나가 대기 중임을 알린다. */
.skeleton {
  background: linear-gradient(90deg, #efefef, #e4e4e4, #efefef);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

/* 화면 깜빡임을 불편해하는 사용자에게는 애니메이션을 끈다. */
@media (prefers-reduced-motion: reduce) {
  .progress-fill,
  .skeleton {
    animation: none;
  }
}
</style>
