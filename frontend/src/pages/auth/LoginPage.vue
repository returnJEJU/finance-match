<script setup>
// 로그인 · 레이아웃: BlankLayout
//
// 입력값은 화면 안에서만 관리한다. 검증·저장·API 는 인증 도메인이 준비되면 붙인다.
import { ref } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterMarried from '@/assets/images/characters/character-married.png'
import BaseButton from '@/components/ui/BaseButton.vue'
import AppHeader from '@/components/layout/AppHeader.vue'

const form = ref({
  email: '',
  password: '',
})

// 비밀번호 표시 여부 — 기본은 가림(감은 눈), 누르면 보임(뜬 눈)
const showPassword = ref(false)
</script>

<template>
  <div class="flex min-h-screen flex-col">
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

      <!-- TODO(인증): 로그인 API 연동 후 성공했을 때만 이동.
           갈 곳은 설문·커플 연동 상태에 따라 달라진다 — 둘 다 끝났으면 대시보드. -->
      <BaseButton :to="{ name: 'dashboard' }" class="mt-5">로그인</BaseButton>

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
