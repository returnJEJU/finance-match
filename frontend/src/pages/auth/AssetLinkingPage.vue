<script setup>
// 자산 연동 중 · 레이아웃: BlankLayout
//
// 화면에 들어오면 자산 연동 API 를 부르고, 받은 금액을 위에서부터 한 줄씩 드러낸다.
//
// 진행바는 <b>멈추지 않고 계속 차오른다</b>. 다음 단계까지 걸리는 시간을 그대로 전환 시간으로 주고
// 등속(linear)으로 움직여서, 바가 목표에 <b>도착하는 순간</b> 그 줄의 금액이 나타난다. 짧게 슥
// 움직인 뒤 멈춰 서 있으면 끊겨 보인다.
//
// 다 되면 자동으로 넘어가지 않고 '다음' 버튼을 활성화한다 — 보고 있는 도중에 화면이 바뀌면
// 급하게 넘어가는 느낌이 든다.
//
// 서버는 한 번의 호출로 전부 한꺼번에 준다. 그러니 이 순차 공개는 <b>연출</b>이다 — 다만
// 보여주는 금액은 전부 응답에서 온 실제 값이라 지어낸 정보는 없다. 응답이 오기 전에는 금액 자리를
// 비워 둔다.
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CreditCard, Landmark, Lock, TrendingUp, Wallet, X } from 'lucide-vue-next'
import { linkAssets } from '@/api/asset'
import { useAssetLinkStore } from '@/stores/assetLink'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'

/**
 * 첫 줄이 드러나기까지의 시간.
 *
 * 뒤 간격보다 길게 둔다 — 화면에 들어오자마자 값이 튀어나오면 "불러오는 중"으로 보이지 않고,
 * 실제로 서버 응답을 기다리는 구간도 여기이기 때문이다.
 */
const FIRST_REVEAL_MS = 1600

/** 두 번째 줄부터의 간격. 앞은 여유 있게, 뒤는 경쾌하게. */
const REVEAL_STEP_MS = 800

/**
 * 불러오는 항목. `category` 는 응답의 카테고리와 이어진다.
 * 대출은 카테고리가 아니라 `totalDebt` 에서 온다.
 */
const ITEMS = [
  { key: 'cash', name: '계좌·현금', icon: Wallet, tint: '#FFF3D6', category: 'BANK_CHECKING' },
  { key: 'saving', name: '예적금', icon: Landmark, tint: '#E3F7E8', category: 'BANK_SAVINGS' },
  { key: 'invest', name: '투자', icon: TrendingUp, tint: '#FFE9E9', category: 'SECURITIES' },
  { key: 'loan', name: '대출', icon: CreditCard, tint: '#E8EEFF', category: null },
]

const router = useRouter()
const assetLinkStore = useAssetLinkStore()
const authStore = useAuthStore()

const linkError = ref('')

/** 한 줄이 드러날 때마다 오르는 폭. 네 줄이면 80% 가 되고 마무리에서 100% 가 된다. */
const PROGRESS_PER_ITEM = 20

/** 몇 줄까지 드러났는지. 진행바가 그 줄에 도착한 뒤에 올라간다. */
const revealedCount = ref(0)

/**
 * 진행바가 향하는 값. 줄이 드러나기 <b>전에</b> 먼저 올려서, 바가 그 시간 동안 천천히 차오른다.
 * 그래서 revealedCount 와 따로 둔다.
 */
const progress = ref(0)

/** 지금 진행 중인 전환에 걸리는 시간. 기다리는 시간과 같게 맞춰 바가 쉬지 않게 한다. */
const progressDurationMs = ref(0)

/** 다 불러와 '다음' 을 누를 수 있는 상태인지. */
const finished = ref(false)

/** 응답. 도착 전에는 null 이라 금액 자리가 비어 있다. */
const result = ref(null)

const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms))

const won = (amount) => `${Number(amount).toLocaleString('ko-KR')}원`

/** 항목별 금액. 자산은 카테고리 합계에서, 대출은 totalDebt 에서 가져온다. */
function amountOf(item) {
  if (!result.value) return null
  if (item.category === null) return won(result.value.totalDebt)

  const found = result.value.summary.find((entry) => entry.category === item.category)
  return won(found?.amount ?? 0)
}

/**
 * 진행바를 다음 눈금까지 천천히 올리고, 도착하면 그 줄을 드러낸다.
 *
 * 전환 시간과 기다리는 시간을 같게 두는 것이 핵심이다. 짧게 움직이고 남은 시간을 서 있으면
 * 딱딱해 보인다.
 */
async function advanceTo(nextProgress, durationMs) {
  progressDurationMs.value = durationMs
  progress.value = nextProgress
  await wait(durationMs)
}

onMounted(async () => {
  try {
    result.value = await linkAssets()

    // 받은 금액을 한 줄씩 드러낸다. 20% → 40% → 60% → 80%
    for (let index = 0; index < ITEMS.length; index += 1) {
      await advanceTo(
        (index + 1) * PROGRESS_PER_ITEM,
        index === 0 ? FIRST_REVEAL_MS : REVEAL_STEP_MS,
      )
      revealedCount.value = index + 1
    }

    // 마무리 — 100% 까지 차오르면 '다음' 을 누를 수 있게 한다. 자동으로 넘어가지 않는다.
    await advanceTo(100, REVEAL_STEP_MS)
    finished.value = true
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

/** 다 불러온 뒤 사용자가 직접 눌러 넘어간다. */
function goNext() {
  assetLinkStore.setResult(result.value)
  router.replace({ name: 'signup-asset-done' })
}
</script>

<template>
  <div class="flex min-h-dvh flex-col">
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

      <!-- 진행률 — 한 줄이 드러날 때마다 20%씩 오른다 -->
      <div class="mt-4.5 flex items-center gap-3">
        <span class="text-[30px] font-extrabold tracking-[-1px]">
          {{ progress }}<span class="text-[19px]">%</span>
        </span>
        <div class="bg-line-card h-1.5 flex-1 overflow-hidden rounded-full">
          <!-- 전환 시간을 다음 단계까지의 대기 시간과 같게 주고 등속으로 움직여, 바가 멈추지 않는다 -->
          <i
            class="bg-brand-deep block h-full rounded-full ease-linear"
            :style="{
              width: `${progress}%`,
              transitionProperty: 'width',
              transitionDuration: `${progressDurationMs}ms`,
            }"
          ></i>
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
                class="mt-0.5 block text-[12px]"
                :class="index < revealedCount ? 'text-ink' : 'text-muted'"
              >
                {{ index < revealedCount ? '완료' : '찾는 중' }}
              </span>
            </span>
            <span class="flex-1"></span>
            <span
              v-if="index < revealedCount"
              class="reveal text-[16px] font-extrabold tracking-[-0.3px]"
            >
              {{ amountOf(item) }}
            </span>
            <span v-else class="skeleton h-[13px] w-[88px] rounded-full"></span>
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
      <BaseButton v-else-if="finished" @click="goNext">다음</BaseButton>
      <BaseButton v-else variant="disabled">잠시만 기다려 주세요</BaseButton>
    </div>
  </div>
</template>

<style scoped>
/* 금액이 툭 튀어나오지 않게 살짝 떠오르며 나타난다. */
.reveal {
  animation: reveal-up 0.32s ease-out;
}

@keyframes reveal-up {
  from {
    opacity: 0;
    transform: translateY(4px);
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
  .skeleton,
  .reveal {
    animation: none;
  }
}
</style>
