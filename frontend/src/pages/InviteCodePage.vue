<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation, useQuery } from '@tanstack/vue-query'
import { ChevronRight, CirclePlus, Heart, LoaderCircle, Ticket } from 'lucide-vue-next'
import { createCouple } from '@/api/couple'
import { getOnboardingStatus } from '@/api/onboarding'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'

const CODE_LENGTH = 8
const INVITE_CODE_PATTERN = /^[A-Z0-9]{8}$/

const router = useRouter()
const inviteCode = ref('')
const inviteCodeError = ref('')

const { data: onboardingStatus } = useQuery({
  queryKey: ['onboardingStatus'],
  queryFn: getOnboardingStatus,
})

const codeCharacters = computed(() =>
  Array.from({ length: CODE_LENGTH }, (_, index) => inviteCode.value[index] || '-'),
)

const isInviteCodeValid = computed(() => INVITE_CODE_PATTERN.test(inviteCode.value))
const hasCreatedInvitation = computed(() => onboardingStatus.value?.hasInvitation === true)

const createCoupleMutation = useMutation({
  mutationFn: createCouple,
  onSuccess: (response) => {
    inviteCodeError.value = ''
    router.push({
      name: 'couple-connected',
      query: { partnerName: response.partnerName },
    })
  },
  onError: (error) => {
    inviteCodeError.value = error.message || '초대 코드 확인에 실패했어요.'
  },
})

const isConnecting = computed(() => createCoupleMutation.isPending.value)

const normalizeInviteCode = (value) =>
  value
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, CODE_LENGTH)

const handleInviteCodeInput = (event) => {
  inviteCode.value = normalizeInviteCode(event.target.value)
  inviteCodeError.value = ''
}

const submitInviteCode = () => {
  if (isConnecting.value) return

  if (!isInviteCodeValid.value) {
    inviteCodeError.value = '8자리 초대 코드를 입력해 주세요.'
    return
  }

  createCoupleMutation.mutate(inviteCode.value)
}

const focusInviteCodeInput = () => {
  document.getElementById('invite-code')?.focus()
}

const goToCoupleSurvey = () => {
  router.push({ name: 'survey-couple' })
}

const goToCreatedInviteCode = () => {
  router.push({ name: 'couple-invite-created' })
}
</script>

<template>
  <section class="flex min-h-screen flex-col px-5 pt-3 pb-8">
    <header class="flex h-8 flex-none items-center justify-center">
      <h1 class="text-center text-[20px] font-semibold text-ink">커플 연동</h1>
    </header>

    <main class="mt-16 flex flex-1 flex-col">
      <div class="text-center">
        <PageTitle align="center" class="!text-[32px]">초대 코드 입력</PageTitle>

        <p class="text-muted mt-4 text-[16px] leading-[1.55]">
          파트너로부터 받은 8자리 초대 코드를 입력하고<br />
          두 분의 금융 궁합을 확인해 보세요!
        </p>
      </div>

      <div class="h-12 flex-none"></div>

      <div>
        <div
          class="rounded-card relative flex h-[81px] items-center border bg-white px-3 shadow-sm transition"
          :class="
            inviteCodeError ? 'border-red-300' : 'border-line-field focus-within:border-brand-deep'
          "
          @click="focusInviteCodeInput"
        >
          <input
            id="invite-code"
            :value="inviteCode"
            type="text"
            inputmode="text"
            autocomplete="one-time-code"
            maxlength="8"
            :disabled="isConnecting"
            class="absolute inset-0 h-full w-full cursor-text bg-transparent text-transparent caret-transparent outline-none disabled:cursor-not-allowed"
            @input="handleInviteCodeInput"
            @keydown.enter.prevent="submitInviteCode"
          />

          <div class="pointer-events-none grid w-full grid-cols-8 gap-1.5">
            <span
              v-for="(character, index) in codeCharacters"
              :key="index"
              class="flex h-[54px] items-center justify-center rounded-[7px] border border-line-soft bg-surface-muted text-center font-mono text-[20px] font-extrabold text-ink-sub"
              :class="character !== '-' ? 'border-brand-deep bg-white text-ink' : ''"
            >
              {{ character }}
            </span>
          </div>
        </div>

        <p v-if="inviteCodeError" class="mt-3 text-center text-[16px] font-medium text-warn">
          {{ inviteCodeError }}
        </p>
      </div>

      <BaseButton
        class="mt-8 text-[20px]"
        :variant="isConnecting || !isInviteCodeValid ? 'disabled' : 'primary'"
        @click="submitInviteCode"
      >
        <LoaderCircle v-if="isConnecting" :size="18" class="animate-spin" />
        <Heart v-else :size="18" fill="currentColor" :stroke-width="2.2" />
        {{ isConnecting ? '커플 연동 중' : '커플 연동하기' }}
      </BaseButton>

      <div class="mt-6 flex items-center gap-3">
        <div class="bg-line-soft h-px flex-1"></div>
        <span class="text-muted text-[16px] font-semibold">OR</span>
        <div class="bg-line-soft h-px flex-1"></div>
      </div>

      <div class="mt-6 space-y-3">
        <button
          v-if="!hasCreatedInvitation"
          type="button"
          class="border-line-card rounded-card flex w-full items-center border bg-white px-3.5 py-3.5 text-left shadow-sm transition hover:bg-gray-50"
          @click="goToCoupleSurvey"
        >
          <span
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-orange-50 text-orange-500"
          >
            <CirclePlus :size="18" :stroke-width="2" />
          </span>

          <span class="ml-3 min-w-0 flex-1">
            <span class="block text-[18px] font-extrabold text-gray-900">
              공동 목표 설정하고 초대 코드 만들기
            </span>
            <span class="mt-0.5 block text-[16px] font-medium text-gray-500">
              아직 코드가 없다면 새로 생성
            </span>
          </span>

          <ChevronRight :size="19" :stroke-width="1.8" class="text-gray-500" />
        </button>

        <button
          v-else
          type="button"
          class="border-line-card rounded-card flex w-full items-center border bg-white px-3.5 py-3.5 text-left shadow-sm transition hover:bg-gray-50"
          @click="goToCreatedInviteCode"
        >
          <span
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gray-100 text-gray-500"
          >
            <Ticket :size="17" :stroke-width="2" />
          </span>

          <span class="ml-3 min-w-0 flex-1">
            <span class="block text-[18px] font-extrabold text-gray-900">
              내가 만든 초대 코드 확인하기
            </span>
            <span class="mt-0.5 block text-[16px] font-medium text-gray-500">
              이미 생성한 내 코드를 공유하기
            </span>
          </span>

          <ChevronRight :size="19" :stroke-width="1.8" class="text-gray-500" />
        </button>
      </div>
    </main>
  </section>
</template>
