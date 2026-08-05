<script setup>
import { computed, ref, watch } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { ArrowRight, CreditCard, Heart, LoaderCircle, LockKeyhole } from 'lucide-vue-next'

import { getInvitation } from '@/api/invitation'
import { getOnboardingStatus } from '@/api/onboarding'
import BaseButton from '@/components/ui/BaseButton.vue'

const POLLING_INTERVAL = 5000 // 폴링 간격 5초

const isConnectionModalOpen = ref(false) // 연결 완료 팝업이 열려 있는지
const connectionBaseline = ref(null) // 이전에 확인한 coupleConnected 값

// 금융 궁합 계산 준비 상태 판단
const isMatchReady = (value) => {
  return (
    value?.coupleConnected === true &&
    value?.personalSurveyCompleted === true &&
    value?.partnerPersonalSurveyCompleted === true
  )
}

const {
  data: status, // TanStack Query가 반환하는 data를 이 페이지에서는 status라는 이름으로 사용
  dataUpdatedAt, // API 조회가 성공해 데이터가 갱신된 시각 (이 값을 watch => 새로운 응답 도착 감지)
  isFetching: isStatusFetching,
  isPending: isStatusPending,
} = useQuery({
  queryKey: ['onboardingStatus'],
  queryFn: getOnboardingStatus,
  refetchInterval: (query) => (isMatchReady(query.state.data) ? false : POLLING_INTERVAL),
  // 아직 준비되지 않았으면 5초마다 조회, 양쪽 모두 설문 완료했으면 false로 폴링 중지
  refetchIntervalInBackground: false,
  refetchOnMount: 'always',
  refetchOnWindowFocus: 'always',
})

// 캐시에 남아 있던 값이 아니라, 이 화면이 열린 뒤 완료된 첫 조회를 연결 상태의 기준으로 삼는다.
// 이후 조회에서 false → true로 바뀐 경우에만 연결 완료 팝업을 연다.
const lastHandledUpdateAt = ref(dataUpdatedAt.value)

watch(dataUpdatedAt, (updatedAt) => {
  if (updatedAt === lastHandledUpdateAt.value || !status.value) {
    // 이미 처리한 갱신 시각과 같거나 아직 실제 상태 데이터가 없는 경우
    return
  }

  lastHandledUpdateAt.value = updatedAt

  const currentCoupleConnected = status.value.coupleConnected

  // 첫 응답을 기준값으로 저장
  if (connectionBaseline.value === null) {
    connectionBaseline.value = currentCoupleConnected
    return
  }

  // false => true인 경우에만 팝업 표시
  if (connectionBaseline.value === false && currentCoupleConnected === true) {
    isConnectionModalOpen.value = true
  }

  // 현재 값을 다음 비교 기준으로 저장 (그래야 한 번 연결된 이후에는 true => true이므로 팝업이 다시 열리지 않음)
  connectionBaseline.value = currentCoupleConnected
})

// 최초 로딩 화면 표시: 데이터가 전혀 없거나, 캐시는 있지만 최신 응답을 확인 중인 상태
const loading = computed(
  () => isStatusPending.value || (connectionBaseline.value === null && isStatusFetching.value),
)

// 초대코드는 생성됐지만 아직 커플 연동이 되지 않은 상태
const showInvitationCode = computed(() => {
  return (
    status.value?.hasInvitation === true &&
    status.value?.coupleConnected === false &&
    status.value?.personalSurveyCompleted === true
  )
})

// 커플 연동은 됐지만 상대방의 개인 설문이 끝나지 않은 상태
const showPartnerSurveyWaiting = computed(() => {
  return (
    status.value?.coupleConnected === true &&
    status.value?.personalSurveyCompleted === true &&
    status.value?.partnerPersonalSurveyCompleted !== true
  )
})

// 내 개인 설문이 완료되지 않은 상태
// 정상 온보딩 흐름에서는 개인 설문 완료 후에만 이 화면에 오지만,
// 로그인 사용자가 /dashboard/waiting 주소로 직접 접근한 경우를 대비한 방어 상태
const showMySurveyWaiting = computed(() => {
  return status.value != null && status.value.personalSurveyCompleted === false
})

// 궁합 계산을 시작할 수 있는 상태
const isReadyForMatch = computed(() => {
  return isMatchReady(status.value)
})

// 초대코드는 커플 연결 전까지 바뀌지 않으므로 5초마다 다시 조회할 이유가 없음 => 온보딩 폴링과 분리
const shouldLoadInvitation = computed(() => showInvitationCode.value)

const { data: invitation, isFetching: isInvitationFetching } = useQuery({
  queryKey: ['invitation'],
  queryFn: getInvitation,
  enabled: shouldLoadInvitation,
  refetchOnMount: 'always',
  refetchOnWindowFocus: false,
})

const inviteCode = computed(() => invitation.value?.inviteCode || '')

const closeConnectionModal = () => {
  isConnectionModalOpen.value = false
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
      class="mt-5 flex flex-1 flex-col items-center rounded-[30px] bg-white px-6 pt-6 pb-8 shadow-[0_4px_20px_rgba(0,0,0,0.04)]"
    >
      <!-- 최초 상태 조회 -->
      <div v-if="loading" class="flex flex-1 flex-col items-center justify-center">
        <LoaderCircle class="animate-spin text-brand-ink" :size="36" :stroke-width="2" />

        <p class="mt-4 text-[14px] font-semibold text-ink-sub">진행 상태를 확인하고 있어요.</p>
      </div>

      <template v-else>
        <div class="flex w-full flex-1 flex-col">
          <!-- 버튼 유무와 관계없이 자물쇠와 안내 문구를 남은 영역의 가운데에 배치 -->
          <div class="flex flex-1 flex-col items-center justify-center py-4">
            <!-- 자물쇠 영역 -->
            <div class="relative flex h-[230px] w-[230px] shrink-0 items-center justify-center">
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

            <!-- 상태별 안내 영역 -->
            <div class="mt-8 min-h-[94px] text-center">
              <!-- 상태를 조회하지 못한 경우 -->
              <template v-if="!status">
                <h1 class="text-[18px] font-bold text-ink">연결 상태를 확인하지 못했어요</h1>

                <p class="mt-3 text-[15px] leading-6 text-ink-sub">
                  잠시 후 자동으로<br />
                  다시 확인할게요.
                </p>
              </template>

              <!-- 양쪽 개인 설문 완료 -->
              <template v-else-if="isReadyForMatch">
                <h1 class="text-[18px] font-bold text-ink">두 분 모두 개인 설문을 완료했어요!</h1>

                <p class="mt-3 text-[15px] leading-6 text-ink-sub">
                  이제 두 분의 금융 궁합 결과를<br />
                  확인해보세요.
                </p>
              </template>

              <!-- 초대코드는 있지만 커플 연동 전 -->
              <template v-else-if="showInvitationCode">
                <p class="text-[15px] font-medium text-ink-sub">
                  나의 코드를 상대방에게 보내주세요
                </p>

                <h1
                  class="mt-3 font-mono text-[32px] leading-none font-extrabold tracking-[0.12em] text-ink"
                >
                  {{ isInvitationFetching ? '--------' : inviteCode || '--------' }}
                </h1>
              </template>

              <!-- 커플 연동 후 상대방 개인 설문 대기 -->
              <template v-else-if="showPartnerSurveyWaiting">
                <h1 class="text-[18px] font-bold text-ink">파트너와 연결됐어요!</h1>

                <p class="mt-3 text-[15px] leading-6 text-ink-sub">
                  파트너가 개인 설문을 완료하면<br />
                  금융 궁합 결과를 확인할 수 있어요.
                </p>
              </template>

              <!-- 정상 흐름에서는 발생하지 않으며, 내 설문 미완료 상태로 직접 접근한 경우의 fallback -->
              <template v-else-if="showMySurveyWaiting">
                <h1 class="text-[18px] font-bold text-ink">내 개인 설문이 완료되지 않았습니다</h1>

                <p class="mt-3 text-[15px] leading-6 text-ink-sub">
                  개인 설문을 완료하면<br />
                  금융 궁합 결과를 확인할 수 있어요.
                </p>
              </template>

              <!-- 정상 흐름에서는 접근할 수 없으며, 주소로 직접 접근한 예외 상태의 fallback -->
              <template v-else>
                <h1 class="text-[18px] font-bold text-ink">아직 궁합을 계산할 수 없습니다</h1>

                <p class="mt-3 text-[15px] leading-6 text-ink-sub">
                  파트너와 연결되면<br />
                  화면에서 바로 알려드릴게요.
                </p>
              </template>
            </div>
          </div>

          <!-- 버튼 유무가 위 콘텐츠 위치에 영향을 주지 않도록 하단 높이를 항상 확보 -->
          <div class="mt-6 h-[54px] w-full shrink-0">
            <BaseButton
              v-if="isReadyForMatch"
              :to="{ name: 'match-calculating' }"
              class="w-full shadow-[0_6px_14px_rgba(250,230,77,0.2)]"
            >
              금융 궁합도 확인하러 가기
              <ArrowRight class="h-[19px] w-[19px]" :stroke-width="2.2" aria-hidden="true" />
            </BaseButton>
          </div>
        </div>
      </template>
    </div>
  </section>

  <!-- 대기 화면에 머무는 동안 커플 연결을 처음 감지했을 때만 표시 -->
  <Teleport to="body">
    <div
      v-if="isConnectionModalOpen"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 px-5"
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="connection-complete-title"
        class="w-full max-w-[360px] rounded-[26px] bg-white px-7 pt-8 pb-7 text-center shadow-[0_16px_40px_rgba(0,0,0,0.18)]"
      >
        <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-brand-soft">
          <Heart class="text-brand-ink" :size="34" :stroke-width="2.2" fill="currentColor" />
        </div>

        <h2 id="connection-complete-title" class="mt-5 text-[20px] font-bold text-ink">
          파트너와 연결됐어요!
        </h2>

        <p class="mt-3 text-[15px] leading-6 text-ink-sub">
          파트너의 개인 설문이 완료되면<br />
          금융 궁합 결과를 확인할 수 있어요.
        </p>

        <button
          type="button"
          class="mt-7 h-12 w-full rounded-full bg-brand text-[16px] font-bold text-ink transition active:scale-[0.98]"
          @click="closeConnectionModal"
        >
          확인
        </button>
      </section>
    </div>
  </Teleport>
</template>
