<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { ArrowRight, Check, ClipboardList, Copy, KeyRound, LoaderCircle } from 'lucide-vue-next'
import { getCommonSurvey, getInvitation } from '@/api/invitation'
import { getOnboardingStatus } from '@/api/onboarding'
import BaseButton from '@/components/ui/BaseButton.vue'
import characterExcited from '@/assets/images/characters/character-excited.png'

const route = useRoute()
const router = useRouter()
const copied = ref(false)

const goalTypeLabels = {
  INVESTMENT: '여유 자금 투자',
  RETIREMENT: '노후 자금 마련',
  MARRIAGE: '결혼 자금 마련',
  HOUSING: '부동산 자금 마련',
  SHORT_TERM: '단기 자금 운용',
}

const loanPurposeLabels = {
  NONE: '계획 없음',
  JEONSE: '전세 자금',
  HOUSING: '주택 구입',
  CAR: '자동차 구입',
  BUSINESS: '사업/창업',
}

const routeInviteCode = computed(() => {
  const inviteCode = route.query.inviteCode
  return typeof inviteCode === 'string' ? inviteCode : ''
})

const {
  data: invitation,
  isLoading: isInvitationLoading,
  isError: isInvitationError,
} = useQuery({
  queryKey: ['invitation'],
  queryFn: getInvitation,
})

const {
  data: commonSurvey,
  isLoading: isCommonSurveyLoading,
  isError: isCommonSurveyError,
} = useQuery({
  queryKey: ['commonSurvey'],
  queryFn: getCommonSurvey,
})

// 진행 상태는 두 곳에 쓰인다 — 아래 '진행 상태' 목록과, 마지막 버튼이 어디로 갈지.
const { data: onboardingStatus, isLoading: isOnboardingStatusLoading } = useQuery({
  queryKey: ['onboardingStatus'],
  queryFn: getOnboardingStatus,
})

const personalSurveyCompleted = computed(
  () => onboardingStatus.value?.personalSurveyCompleted === true,
)
const coupleConnected = computed(() => onboardingStatus.value?.coupleConnected === true)

const inviteCode = computed(() => routeInviteCode.value || invitation.value?.inviteCode || '')

const isLoading = computed(
  () => isInvitationLoading.value || isCommonSurveyLoading.value || isOnboardingStatusLoading.value,
)
const hasLoadError = computed(() => isInvitationError.value || isCommonSurveyError.value)

const summaryItems = computed(() => [
  {
    label: '목표',
    value: goalTypeLabels[commonSurvey.value?.goalType1] || '-',
  },
  {
    label: '목표 금액',
    value: formatWon(commonSurvey.value?.targetAmount),
  },
  {
    label: '기간',
    value: commonSurvey.value?.targetPeriodMonths
      ? `${commonSurvey.value.targetPeriodMonths}개월`
      : '-',
  },
  {
    label: '대출 계획',
    value: loanPurposeLabels[commonSurvey.value?.loanPurpose] || '-',
  },
])

const progressSteps = computed(() => [
  { label: '공동 설문 완료', done: true },
  { label: '내 개인 설문', done: personalSurveyCompleted.value },
  { label: '파트너 연결', done: coupleConnected.value },
])

function formatWon(amount) {
  if (!amount) return '-'

  const won = Number(amount)
  const eok = Math.floor(won / 100000000)
  const man = Math.floor((won % 100000000) / 10000)

  if (eok && man) return `${eok}억 ${man.toLocaleString('ko-KR')}만 원`
  if (eok) return `${eok}억 원`
  if (man) return `${man.toLocaleString('ko-KR')}만 원`
  return `${won.toLocaleString('ko-KR')}원`
}

async function copyInviteCode() {
  if (!inviteCode.value) return

  await navigator.clipboard.writeText(inviteCode.value)
  copied.value = true
  window.setTimeout(() => {
    copied.value = false
  }, 1600)
}

/**
 * 마지막 버튼.
 *
 * 개인설문 전이라면 그것이 다음 할 일이다. 이미 마쳤다면(대기 화면에서 코드를 다시 보러 온
 * 경우) 설문을 또 시킬 수 없으니 대기 화면으로 돌려보낸다.
 */
function goToNextStep() {
  router.push(
    personalSurveyCompleted.value ? { name: 'dashboard-waiting' } : { name: 'survey-personal' },
  )
}

// 코드를 만들어 두고도 파트너가 먼저 코드를 보낸 경우가 있다. 그때 입력할 화면으로 갈 길을 준다.
function goToInviteCodeInput() {
  router.push({ name: 'couple-invite' })
}
</script>

<template>
  <section class="flex min-h-screen flex-col bg-canvas px-4 pt-4 pb-7">
    <header class="flex justify-center">
      <span class="text-[17px] font-extrabold text-good">찰떡궁합</span>
    </header>

    <main class="mt-5 flex flex-1 flex-col">
      <div class="text-center">
        <img :src="characterExcited" alt="" class="mx-auto h-[104px] w-[104px] object-contain" />

        <h1 class="mt-5 text-[24px] leading-[1.35] font-extrabold text-ink">
          공동 목표가 설정됐어요!
        </h1>
        <p class="text-muted mt-2 text-[13px] font-medium">
          두 분의 소중한 미래를 위한 첫 걸음입니다.
        </p>
      </div>

      <div v-if="isLoading" class="mt-7 flex items-center justify-center py-12 text-muted">
        <LoaderCircle :size="22" class="animate-spin" />
      </div>

      <template v-else>
        <p v-if="hasLoadError" class="mt-5 text-center text-[12px] font-semibold text-red-500">
          공동 목표 정보를 불러오지 못했어요.
        </p>

        <section class="rounded-card border-line-card mt-7 border bg-white px-5 py-5 shadow-sm">
          <h2 class="flex items-center gap-2 text-[16px] font-extrabold text-ink">
            <ClipboardList :size="18" class="text-brand-ink" />
            우리의 공동 목표 요약
          </h2>

          <dl class="mt-4 grid grid-cols-2 gap-3">
            <div
              v-for="item in summaryItems"
              :key="item.label"
              class="rounded-field bg-surface-muted px-4 py-4"
            >
              <dt class="text-[12px] font-bold text-muted">{{ item.label }}</dt>
              <dd class="mt-2 break-keep text-[14px] leading-[1.35] font-extrabold text-brand-ink">
                {{ item.value }}
              </dd>
            </div>
          </dl>
        </section>

        <section class="rounded-card mt-5 bg-brand px-5 py-5">
          <p class="text-center text-[14px] font-extrabold text-ink">
            아래 코드를 파트너에게 보내주세요.
          </p>

          <div class="mt-4 rounded-[8px] border-2 border-dashed border-white bg-white px-4 py-4">
            <p
              class="text-center font-mono text-[32px] leading-none font-extrabold tracking-[0.08em] text-ink"
            >
              {{ inviteCode || '--------' }}
            </p>
          </div>

          <button
            type="button"
            class="mt-3 flex h-12 w-full items-center justify-center gap-2 rounded-card bg-mint text-[14px] font-extrabold text-ink transition active:scale-[0.98]"
            :disabled="!inviteCode"
            @click="copyInviteCode"
          >
            <Check v-if="copied" :size="18" :stroke-width="2.4" />
            <Copy v-else :size="17" :stroke-width="2.2" />
            {{ copied ? '복사 완료' : '코드 복사하기' }}
          </button>
        </section>

        <section class="rounded-card border-line-card mt-5 border bg-white px-5 py-5 shadow-sm">
          <h2 class="text-[15px] font-extrabold text-ink">진행 상태</h2>

          <ol class="mt-4 space-y-4">
            <li
              v-for="(step, index) in progressSteps"
              :key="step.label"
              class="relative flex gap-3"
            >
              <span
                v-if="index < progressSteps.length - 1"
                class="absolute top-6 left-[9px] h-7 w-px bg-line-card"
              ></span>
              <span
                class="relative z-10 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[11px] font-bold"
                :class="step.done ? 'bg-mint text-good' : 'bg-line-card text-muted'"
              >
                <Check v-if="step.done" :size="13" :stroke-width="3" />
                <span v-else class="h-1.5 w-1.5 rounded-full bg-muted"></span>
              </span>
              <span class="pt-0.5 text-[13px] font-bold text-ink">{{ step.label }}</span>
            </li>
          </ol>
        </section>
      </template>
    </main>

    <div class="mt-7 flex flex-none flex-col gap-3">
      <!-- 진행 상태를 불러오기 전에는 어디로 갈지 정할 수 없어 누를 수 없게 둔다 -->
      <BaseButton
        class="shadow-[0_8px_18px_rgba(250,230,77,0.28)]"
        :variant="isLoading ? 'disabled' : 'primary'"
        @click="goToNextStep"
      >
        {{ personalSurveyCompleted ? '파트너 기다리기' : '개인 설문 시작하기' }}
        <ArrowRight :size="18" :stroke-width="2.4" />
      </BaseButton>

      <BaseButton v-if="!coupleConnected" variant="ghost" @click="goToInviteCodeInput">
        <KeyRound :size="17" :stroke-width="2.2" />
        파트너 코드 입력하기
      </BaseButton>
    </div>
  </section>
</template>
