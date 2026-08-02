<script setup>
// 회원가입 - 정보 입력 (1/4) · 레이아웃: BlankLayout
//
// 입력값은 화면 안에서만 관리한다. 검증·저장·API 는 인증 도메인이 준비되면 붙인다.
// 첫 단계라 상단에 뒤로가기를 두지 않는다.
import { ref } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import FunnelHeader from '@/components/layout/FunnelHeader.vue'

// gender 는 백엔드 enum 값(F·M)을 그대로 담는다. 화면 문구(여성·남성)와 분리해 두면
// 보낼 때 변환하는 단계가 생기고, 빠뜨리면 INVALID_INPUT 이 난다.
const form = ref({
  name: '',
  gender: '',
  birth: '',
  email: '',
  password: '',
  passwordConfirm: '',
})

// 비밀번호 표시 여부 — 기본은 가림(감은 눈), 누르면 보임(뜬 눈)
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

/**
 * 생년월일을 YYYY-MM-DD 로 보이게 한다.
 *
 * <input type="date"> 는 표시 형식이 브라우저 언어 설정을 따라가서(예: mm/dd/yyyy)
 * 우리가 정할 수 없다. 그래서 일반 입력창을 쓰고 숫자만 받아 직접 하이픈을 넣는다.
 */
function formatBirth(event) {
  const digits = event.target.value.replace(/\D/g, '').slice(0, 8)
  const parts = [digits.slice(0, 4), digits.slice(4, 6), digits.slice(6, 8)]
  form.value.birth = parts.filter(Boolean).join('-')
}
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <FunnelHeader :step="1" :show-back="false" />

    <div class="flex flex-1 flex-col px-7">
      <PageTitle class="mt-3.5">금융 궁합을 보기 위한<br />첫 단계예요</PageTitle>
      <p class="text-muted mt-2 text-[13px] leading-[1.5]">
        두 사람에게 맞는 금융상품까지 찾아드려요
      </p>

      <label class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">이름</label>
      <input
        v-model="form.name"
        type="text"
        placeholder="성함을 입력해 주세요"
        class="border-line-field rounded-field placeholder:text-muted-soft h-12 border bg-white px-3.5 text-[14px] outline-none"
      />

      <span class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">성별</span>
      <div class="flex gap-2">
        <button
          v-for="option in [
            { value: 'M', label: '남성' },
            { value: 'F', label: '여성' },
          ]"
          :key="option.value"
          type="button"
          class="rounded-chip h-11 flex-1 cursor-pointer text-[14px] font-semibold transition duration-100 active:scale-[0.98]"
          :class="form.gender === option.value ? 'bg-brand text-ink' : 'bg-line-soft text-muted'"
          @click="form.gender = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <label class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">생년월일</label>
      <input
        :value="form.birth"
        type="text"
        inputmode="numeric"
        maxlength="10"
        placeholder="YYYY-MM-DD"
        class="border-line-field rounded-field placeholder:text-muted-soft h-12 border bg-white px-3.5 text-[14px] outline-none"
        @input="formatBirth"
      />

      <label class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">이메일(아이디)</label>
      <input
        v-model="form.email"
        type="email"
        inputmode="email"
        autocomplete="email"
        placeholder="example@gmail.com"
        class="border-line-field rounded-field placeholder:text-muted-soft h-12 border bg-white px-3.5 text-[14px] outline-none"
      />

      <label class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">비밀번호</label>
      <div
        class="border-line-field rounded-field flex h-12 items-center gap-2 border bg-white px-3.5"
      >
        <input
          v-model="form.password"
          :type="showPassword ? 'text' : 'password'"
          placeholder="8~16자 영문, 숫자, 특수문자 조합"
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

      <label class="text-ink-sub mt-4 mb-1.5 text-[12px] font-semibold">비밀번호 확인</label>
      <div
        class="border-line-field rounded-field flex h-12 items-center gap-2 border bg-white px-3.5"
      >
        <input
          v-model="form.passwordConfirm"
          :type="showPasswordConfirm ? 'text' : 'password'"
          placeholder="비밀번호를 한번 더 입력해 주세요"
          class="placeholder:text-muted-soft flex-1 bg-transparent text-[14px] outline-none"
        />
        <button
          type="button"
          class="text-muted-soft flex-none cursor-pointer"
          :aria-label="showPasswordConfirm ? '비밀번호 숨기기' : '비밀번호 표시'"
          @click="showPasswordConfirm = !showPasswordConfirm"
        >
          <component :is="showPasswordConfirm ? Eye : EyeOff" class="h-[18px] w-[18px]" />
        </button>
      </div>

      <div class="h-6 flex-1"></div>
    </div>

    <div class="flex flex-none flex-col px-7 pb-14">
      <BaseButton :to="{ name: 'signup-agree' }"> 다음 </BaseButton>
    </div>
  </div>
</template>
