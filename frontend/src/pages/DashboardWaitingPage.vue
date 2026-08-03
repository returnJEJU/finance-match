<script setup>
// 대기 화면 · 레이아웃: DefaultLayout(navLocked)
//
// 지금 할 수 있는 일이 없는 사람이 떨어지는 곳이라 <b>기다리는 이유가 두 가지</b>다.
//   - 커플은 연결됐고 상대의 개인설문만 남음  → 기다리면 된다
//   - 아직 커플이 연결되지 않음(초대코드를 보내고 내 설문까지 끝낸 상태)
//     → 기다린다고 되는 일이 아니다. 코드를 전달해야 한다.
// 그래서 연동 여부에 따라 문구를 바꾸고, 연동 전이라면 내 초대코드로 가는 길을 준다.
// 이 화면은 하단 탭이 잠겨 있어(navLocked) 그 길이 없으면 빠져나갈 방법이 없다.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { CreditCard, Heart, LockKeyhole, RefreshCw, Ticket } from 'lucide-vue-next'

import { getOnboardingStatus } from '@/api/onboarding'

const router = useRouter()

const refreshing = ref(false)
const refreshMessage = ref('')

const { data: status, refetch: refetchStatus } = useQuery({
  queryKey: ['onboardingStatus'],
  queryFn: getOnboardingStatus,
})

// 불러오기 전에는 알 수 없다. 아직 모를 때는 "기다리는 중"으로 두어 초대코드 안내가 번쩍이지 않게 한다.
const coupleConnected = computed(() => status.value?.coupleConnected !== false)

const handleRefresh = async () => {
  if (refreshing.value) {
    return
  }

  refreshing.value = true
  refreshMessage.value = ''

  try {
    // 화면 문구도 이 결과를 보고 있으므로 다시 불러오면 함께 갱신된다.
    const result = await refetchStatus()
    if (result.isError) throw result.error

    const next = result.data

    // 문구는 한 줄로 끝맺는다 — 기다리면 되는 상황과 그렇지 않은 상황의 안내가 달라서다.
    if (!next.coupleConnected) {
      refreshMessage.value = '아직 연결되지 않았습니다. 파트너에게 초대 코드를 보내주세요.'
      return
    }

    if (!next.personalSurveyCompleted) {
      refreshMessage.value = '아직 내 개인설문이 완료되지 않았습니다.'
      return
    }

    if (next.partnerPersonalSurveyCompleted !== true) {
      refreshMessage.value = '상대방의 개인설문이 완료되지 않았습니다. 잠시만 기다려주세요.'
      return
    }

    router.push('/match/calculating')
  } catch (error) {
    refreshMessage.value =
      error?.message || '상태를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    refreshing.value = false
  }
}
</script>

<template>
  <section class="flex min-h-[calc(100vh-108px)] flex-col px-5 py-4">
    <!-- 상단 안내 문구 -->
    <p class="text-center text-[15px] font-semibold text-brand-ink">
      함께 만드는 안정적인 우리의 금융 미래 💛
    </p>

    <!-- 흰색 메인 카드 -->
    <div
      class="mt-5 flex flex-1 flex-col items-center rounded-[30px] bg-white px-6 pt-12 pb-10 shadow-[0_4px_20px_rgba(0,0,0,0.04)]"
    >
      <!-- 자물쇠 영역 -->
      <div class="relative flex h-[230px] w-[230px] items-center justify-center">
        <!-- 연한 노란색 원 -->
        <div class="absolute inset-0 rounded-full bg-brand-soft" />

        <!-- 원 안쪽 장식 -->
        <div class="absolute inset-[18px] rounded-full border-[10px] border-white/50" />

        <!-- 왼쪽 하트 -->
        <Heart class="absolute top-1 left-0 text-[#ebe9dc]" :size="43" :stroke-width="1.8" />

        <!-- 오른쪽 카드 장식 -->
        <CreditCard
          class="absolute right-0 bottom-8 rotate-12 text-[#d7d3c0]"
          :size="36"
          :stroke-width="1.5"
        />

        <!-- 가운데 흰색 원 -->
        <div
          class="relative flex h-[112px] w-[112px] items-center justify-center rounded-full bg-white shadow-[0_8px_20px_rgba(90,85,30,0.16)]"
        >
          <LockKeyhole class="text-brand-ink" :size="54" :stroke-width="2.4" />
        </div>
      </div>

      <!-- 안내 문구 — 기다리는 이유에 따라 달라진다 -->
      <div class="mt-10 text-center">
        <h1 class="text-[18px] font-bold">
          {{
            coupleConnected ? '아직 궁합을 계산할 수 없습니다' : '아직 파트너와 연결되지 않았습니다'
          }}
        </h1>

        <p v-if="coupleConnected" class="mt-3 text-[15px] leading-6 text-ink-sub">
          상대방이 개인 설문을 완료하면<br />
          새로고침 버튼을 눌러 확인해주세요.
        </p>

        <p v-else class="mt-3 text-[15px] leading-6 text-ink-sub">
          내 초대 코드를 파트너에게 보내주세요.<br />
          파트너가 코드를 입력하면 연결됩니다.
        </p>
      </div>

      <div class="mt-12 flex flex-col items-center gap-3">
        <!-- 연동 전이라면 기다린다고 되는 일이 아니다. 코드를 다시 볼 수 있게 한다. -->
        <RouterLink
          v-if="!coupleConnected"
          :to="{ name: 'couple-invite-created' }"
          class="flex h-12 w-[200px] items-center justify-center gap-2 rounded-full bg-brand text-[15px] font-bold text-ink transition active:scale-[0.98]"
        >
          <Ticket :size="17" :stroke-width="2.2" />
          내 초대 코드 보기
        </RouterLink>

        <!-- 새로고침 버튼 -->
        <button
          type="button"
          class="flex h-12 w-[200px] items-center justify-center gap-2 rounded-full border-2 border-brand bg-white text-[15px] font-semibold transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="refreshing"
          @click="handleRefresh"
        >
          <RefreshCw :size="17" :stroke-width="2.2" :class="{ 'animate-spin': refreshing }" />

          {{ refreshing ? '확인 중...' : '새로고침' }}
        </button>
      </div>

      <!-- 상태 안내 문구 -->
      <p
        v-if="refreshMessage"
        role="alert"
        class="mt-4 text-center text-[12px] leading-5 font-semibold text-red-500"
      >
        {{ refreshMessage }}
      </p>
    </div>
  </section>
</template>
