<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation } from '@tanstack/vue-query'
import { Heart, LoaderCircle } from 'lucide-vue-next'
import { createCouple } from '@/api/couple'
import characterWorking from '@/assets/images/characters/character-working.png'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import AppHeader from '@/components/layout/AppHeader.vue'

const CODE_LENGTH = 8
const INVITE_CODE_PATTERN = /^[A-Z0-9]{8}$/

const router = useRouter()
const inviteCode = ref('')
const inviteCodeError = ref('')

const codeCharacters = computed(() =>
  Array.from({ length: CODE_LENGTH }, (_, index) => inviteCode.value[index] || '-'),
)

const isInviteCodeValid = computed(() => INVITE_CODE_PATTERN.test(inviteCode.value))

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
</script>

<template>
  <section class="flex min-h-screen flex-col px-5 pt-3 pb-8">
    <AppHeader
      title="커플 연동"
      variant="plain"
      :fallback-to="{ name: 'couple-start' }"
      class="h-8 !px-0 !pt-0 !pb-0 [&_h1]:!text-[20px] [&_h1]:!font-semibold"
    />

    <main class="mt-16 flex flex-1 flex-col">
      <div class="text-center">
        <PageTitle align="center" class="!text-[32px]">초대 코드 입력</PageTitle>
        <p class="text-muted mt-4 text-[16px] leading-[1.55]">
          파트너로부터 받은 8자리 초대 코드를 입력하고<br />
          두 분의 금융 궁합을 확인해 보세요!
        </p>
        <div class="relative mx-auto mt-6 flex h-[190px] w-[190px] items-center justify-center">
          <span
            class="absolute h-[190px] w-[190px] rounded-full bg-[radial-gradient(circle,_rgba(255,244,79,0.32)_0%,_rgba(255,244,79,0.14)_50%,_transparent_74%)]"
            aria-hidden="true"
          ></span>
          <img
            :src="characterWorking"
            alt="초대 코드를 입력하는 찰떡귱합 캐릭터"
            class="relative z-10 h-[170px] w-[170px] object-contain"
          />
        </div>
      </div>

      <div class="h-8 flex-none"></div>

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
    </main>
  </section>
</template>
