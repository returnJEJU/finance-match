<script setup>
// 회원가입 - 인증서 선택 (3/4) · 레이아웃: BlankLayout
//
// 실제 간편인증 연동은 하지 않는다. 어떤 인증서를 고를지만 화면 안에서 관리하고,
// <b>회원가입 요청은 여기서 보낸다</b> — 1·2단계에서 모아둔 값을 한 번에 보낸다.
//
// 가입 응답의 accessToken 으로 곧바로 로그인 상태가 된다. 다음 단계인 자산연동이 인증을
// 요구하기 때문이다.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMutation } from '@tanstack/vue-query'
import { ChevronRight, LoaderCircle } from 'lucide-vue-next'
import { signup } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useSignupStore } from '@/stores/signup'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import FunnelHeader from '@/components/layout/FunnelHeader.vue'

const router = useRouter()
const signupStore = useSignupStore()
const authStore = useAuthStore()

const signupError = ref('')

// 1·2단계를 건너뛰고 들어온 경우를 막는 일은 라우터의 beforeEnter 가 한다.
// 화면이 뜬 뒤(onMounted)에 이동을 걸면 진행 중인 내비게이션과 충돌해 주소만 바뀌고
// 화면은 그대로 남는다.

const signupMutation = useMutation({
  mutationFn: signup,
  onSuccess: (result) => {
    // 가입 응답의 토큰으로 로그인 상태를 만든다. 자산연동이 인증을 요구한다.
    authStore.applySignup(result)
    // 비밀번호를 메모리에 남겨두지 않는다.
    signupStore.reset()
    router.replace({ name: 'signup-asset' })
  },
  onError: (error) => {
    signupError.value =
      error.code === 'EMAIL_EXISTS'
        ? '이미 가입된 이메일이에요. 처음 화면에서 다른 이메일로 시도해 주세요.'
        : error.message || '회원가입에 실패했어요. 잠시 후 다시 시도해 주세요.'
  },
})

const isSubmitting = computed(() => signupMutation.isPending.value)

function submit() {
  signupError.value = ''
  signupMutation.mutate(signupStore.payload)
}

// 인증서 목록. mark 는 로고 대신 쓰는 머리글자, color 는 각 사의 브랜드 색.
const CERTS = [
  { key: 'kakao', name: '카카오 인증서', mark: 'K', color: '#FAE100', note: '30초만에 인증' },
  { key: 'naver', name: '네이버 인증서', mark: 'N', color: '#03C75A', note: '' },
  { key: 'toss', name: '토스 인증서', mark: 'T', color: '#3182F6', note: '' },
]

const selected = ref('kakao')
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <FunnelHeader :step="3" :fallback-to="{ name: 'signup-agree' }" />

    <div class="flex flex-1 flex-col px-7">
      <PageTitle class="mt-3.5">인증 방법을<br />선택해 주세요.</PageTitle>
      <p class="text-muted mt-4.5 text-[13px] leading-[1.5]">
        평소 쓰던 간편인증으로 30초 만에 끝나요.
      </p>

      <div class="mt-5.5 flex flex-col gap-3.5">
        <button
          v-for="cert in CERTS"
          :key="cert.key"
          type="button"
          class="rounded-card flex cursor-pointer items-center gap-[13px] bg-white px-4 py-[17px] transition-transform duration-100 active:scale-[0.99]"
          :class="
            selected === cert.key ? 'border-brand-deep border-[1.6px]' : 'border-line-card border'
          "
          @click="selected = cert.key"
        >
          <span
            class="flex h-[38px] w-[38px] flex-none items-center justify-center rounded-full text-[17px] font-extrabold text-white"
            :style="{ backgroundColor: cert.color }"
          >
            {{ cert.mark }}
          </span>
          <span class="text-[15px] font-bold">{{ cert.name }}</span>
          <span class="flex-1"></span>
          <span class="text-muted text-[11.5px]">{{ cert.note }}</span>
        </button>
      </div>

      <button
        type="button"
        class="text-muted mt-4.5 cursor-pointer text-center text-[13px] underline underline-offset-[3px]"
      >
        다른 인증서 선택
      </button>

      <div class="flex-1"></div>
    </div>

    <div class="flex flex-none flex-col gap-4.5 px-7 pb-14">
      <button
        type="button"
        class="text-muted flex cursor-pointer items-center px-0.5 text-[12.5px]"
      >
        민간 인증서 개인정보 제3자 제공 필수 동의
        <span class="flex-1"></span>
        <ChevronRight class="h-4 w-4 text-[#C9C9CE]" />
      </button>

      <p v-if="signupError" class="-mt-2 text-[12px] font-medium text-red-500">
        {{ signupError }}
      </p>

      <BaseButton :variant="isSubmitting ? 'disabled' : 'primary'" @click="submit">
        <LoaderCircle v-if="isSubmitting" class="h-[18px] w-[18px] animate-spin" />
        {{ isSubmitting ? '가입하는 중' : '동의하고 진행' }}
      </BaseButton>
    </div>
  </div>
</template>
