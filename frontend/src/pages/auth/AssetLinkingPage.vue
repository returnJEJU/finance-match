<script setup>
// 자산 연동 중 · 레이아웃: BlankLayout
//
// 화면에 들어오면 자산 연동 API 를 부르고, 응답이 오면 완료 화면으로 넘어간다.
//
// 항목이 하나씩 채워지는 것처럼 보이지만 서버는 그렇게 주지 않는다 — 한 번의 호출로 전부 한꺼번에
// 온다. 그래서 없는 진행 상황을 지어내지 않고, 응답 전에는 모두 '찾는 중'으로 둔다.
// 응답이 너무 빨리 와도 화면이 번쩍 지나가지 않도록 최소 표시 시간을 둔다(MatchCalculatingPage 와 같은 방식).
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CreditCard, Landmark, Lock, TrendingUp, Wallet, X } from 'lucide-vue-next'
import { linkAssets } from '@/api/asset'
import { useAssetLinkStore } from '@/stores/assetLink'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'

/** 응답이 빨라도 이만큼은 화면을 보여준다. */
const MINIMUM_DISPLAY_MS = 2000

// 불러오는 항목. 금액은 응답이 와야 알 수 있으므로 여기서는 이름·아이콘만 정한다.
const ITEMS = [
  { key: 'cash', name: '계좌·현금', icon: Wallet, tint: '#FFF3D6', skeleton: 96 },
  { key: 'saving', name: '예적금', icon: Landmark, tint: '#E3F7E8', skeleton: 88 },
  { key: 'invest', name: '투자', icon: TrendingUp, tint: '#FFE9E9', skeleton: 96 },
  { key: 'loan', name: '대출', icon: CreditCard, tint: '#E8EEFF', skeleton: 72 },
]

const router = useRouter()
const assetLinkStore = useAssetLinkStore()
const authStore = useAuthStore()

const linkError = ref('')

const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms))

onMounted(async () => {
  try {
    const [result] = await Promise.all([linkAssets(), wait(MINIMUM_DISPLAY_MS)])

    assetLinkStore.setResult(result)
    router.replace({ name: 'signup-asset-done' })
  } catch (error) {
    // 이미 연동한 회원이 뒤로 가기·새로고침으로 다시 들어온 경우. 연동 자체는 끝나 있으므로
    // 막지 않고 다음 단계로 보낸다. 완료 화면은 응답이 있어야 그릴 수 있어 건너뛴다.
    if (error.code === 'ASSET_ALREADY_LINKED') {
      router.replace({ name: 'couple-start' })
      return
    }
    linkError.value = error.message || '자산을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
  }
})

// 연동을 중단하고 이전 화면으로. 앱 안에 기록이 없으면 자산 불러오기로 보낸다.
function close() {
  if (window.history.state?.back) router.back()
  else router.replace({ name: 'signup-asset' })
}

function retry() {
  router.replace({ name: 'signup-asset' })
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
      <PageTitle class="mt-3.5"
        >{{ authStore.member?.name ?? '회원' }}님의 자산을<br />불러오고 있어요</PageTitle
      >
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
              <span class="text-muted mt-0.5 block text-[11.5px]">찾는 중</span>
            </span>
            <span class="flex-1"></span>
            <span
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

    <div class="flex flex-none flex-col gap-3 px-7 pb-14">
      <p v-if="linkError" class="text-center text-[12px] font-medium text-red-500">
        {{ linkError }}
      </p>

      <BaseButton v-if="linkError" @click="retry">다시 시도하기</BaseButton>
      <BaseButton v-else variant="disabled">잠시만 기다려 주세요</BaseButton>
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
