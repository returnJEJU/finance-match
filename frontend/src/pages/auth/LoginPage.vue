<script setup>
// 로그인 · 레이아웃: BlankLayout
//
// 로그인에 성공하면 토큰은 authStore 가 저장하고, 어느 화면으로 갈지는 resolveNextRoute 가 정한다.
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { Eye, EyeOff, LoaderCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { resolveNextRoute } from '@/router/resolveNextRoute'
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterMarried from '@/assets/images/characters/character-married.png'
import BaseButton from '@/components/ui/BaseButton.vue'
import AppHeader from '@/components/layout/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const authStore = useAuthStore()

/**
 * 인증 가드가 붙여 보낸 원래 목적지.
 *
 * 앱 안의 경로만 받는다 — 주소창으로 외부 주소를 넣어 다른 사이트로 튕기게 하는 것을 막는다.
 */
const redirectTo = computed(() => {
  const value = route.query.redirect
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
    ? value
    : null
})

const form = ref({
  email: '',
  password: '',
})

// 비밀번호 표시 여부 — 기본은 가림(감은 눈), 누르면 보임(뜬 눈)
const showPassword = ref(false)

const loginError = ref('')

const loginMutation = useMutation({
  mutationFn: () => authStore.login(form.value),
  onSuccess: (result) => {
    queryClient.clear()

    // 로그인 화면을 히스토리에 남기지 않는다. 다음 화면에서 뒤로 가면 로그인으로 돌아오는 대신
    // 그 이전(온보딩)으로 나가야 한다.
    //
    // redirect 는 온보딩을 다 끝낸 사람에게만 적용된다(resolveNextRoute 안에서 판단). 퍼널 중간에
    // 있는 사람을 원하는 곳으로 보내면 볼 것이 없는 화면에 떨어진다.
    router.replace(resolveNextRoute(result, redirectTo.value))
  },
  onError: (error) => {
    // 백엔드는 "없는 이메일"·"비밀번호 불일치"·"탈퇴 회원"을 INVALID_CREDENTIALS 하나로 응답한다
    // (계정 열거 방지). 그래서 화면에서도 사유를 나눠 안내할 수 없다.
    loginError.value = error.message || '로그인에 실패했어요. 잠시 후 다시 시도해 주세요.'
  },
})

const isSubmitting = computed(() => loginMutation.isPending.value)

// 형식 검사는 하지 않는다. 백엔드가 형식 오류든 값 오류든 같은 코드로 답하므로 여기서 미리
// 걸러도 사용자에게 더 알려줄 수 있는 것이 없다. 빈 값만 막는다.
const canSubmit = computed(() => Boolean(form.value.email && form.value.password))

function submit() {
  loginError.value = ''
  loginMutation.mutate()
}
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <AppHeader variant="plain" title="로그인" :fallback-to="{ name: 'onboarding' }" />

    <div class="flex flex-1 flex-col px-7">
      <div class="mt-1.5 text-center">
        <img :src="characterMarried" alt="" class="mx-auto w-[150px]" />
        <img :src="logoWordmark" alt="찰떡귱합" class="mx-auto w-[92px]" />
        <p class="text-muted mt-2.5 text-[13px] leading-[1.5]">
          두 사람의 금융 궁합을 확인해보세요
        </p>
      </div>

      <label class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">이메일 (아이디)</label>
      <input
        v-model="form.email"
        type="email"
        autocomplete="username"
        placeholder="이메일을 입력하세요"
        class="border-line-field rounded-field placeholder:text-muted-soft h-12 border bg-white px-3.5 text-[14px] outline-none"
      />

      <label class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">비밀번호</label>
      <div
        class="border-line-field rounded-field flex h-12 items-center gap-2 border bg-white px-3.5"
      >
        <input
          v-model="form.password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          placeholder="비밀번호를 입력하세요"
          class="placeholder:text-muted-soft flex-1 bg-transparent text-[14px] outline-none"
        />
        <button
          type="button"
          class="text-muted-soft flex-none cursor-pointer"
          :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 표시'"
          @click="showPassword = !showPassword"
        >
          <component :is="showPassword ? Eye : EyeOff" class="h-[18px] w-[18px]" />
        </button>
      </div>

      <!-- 비밀번호 찾기는 만들지 않는다. 누를 수 없는 안내 문구로만 둔다. -->
      <p class="text-muted mt-[15px] text-center text-[12.5px]">비밀번호를 잊으셨나요?</p>

      <p v-if="loginError" class="mt-3 text-center text-[12px] font-medium text-red-500">
        {{ loginError }}
      </p>

      <BaseButton
        :variant="canSubmit && !isSubmitting ? 'primary' : 'disabled'"
        class="mt-5"
        @click="submit"
      >
        <LoaderCircle v-if="isSubmitting" class="h-[18px] w-[18px] animate-spin" />
        {{ isSubmitting ? '로그인하는 중' : '로그인' }}
      </BaseButton>

      <div class="my-3.5 flex items-center gap-3">
        <span class="bg-line-card h-px flex-1"></span>
        <span class="text-muted-soft text-[12px]">또는</span>
        <span class="bg-line-card h-px flex-1"></span>
      </div>

      <!-- TODO(인증): 카카오 로그인 연동 후 클릭 시 카카오 동의 화면으로 이동 -->
      <BaseButton variant="kakao">카카오로 계속하기</BaseButton>

      <div class="flex-1"></div>

      <!-- '회원가입' 만 링크. 앞 문구는 일반 텍스트라 눌러도 흐려지지 않는다. -->
      <p class="text-muted pt-6 pb-[26px] text-center text-[12.5px]">
        아직 회원이 아니신가요?
        <RouterLink
          :to="{ name: 'signup' }"
          class="text-ink-sub ml-3 font-bold transition-opacity active:opacity-60"
        >
          회원가입
        </RouterLink>
      </p>
    </div>
  </div>
</template>
