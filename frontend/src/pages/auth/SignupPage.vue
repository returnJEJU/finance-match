<script setup>
// 회원가입 - 정보 입력 (1/4) · 레이아웃: BlankLayout
//
// 입력값은 화면 안에서만 관리한다. 검증·저장·API 는 인증 도메인이 준비되면 붙인다.
// 첫 단계라 상단에 뒤로가기를 두지 않는다.
import { nextTick, ref } from 'vue'
import { ChevronDown, Eye, EyeOff, X } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import FunnelHeader from '@/components/layout/FunnelHeader.vue'

const form = ref({
  name: '',
  gender: '',
  birth: '',
  emailLocal: '', // @ 앞
  emailDomain: '', // @ 뒤 — 목록에서 고른 값 ('custom' 이면 직접 입력)
  emailDomainCustom: '', // 직접 입력한 도메인
  password: '',
  passwordConfirm: '',
})

// 비밀번호 표시 여부 — 기본은 가림(감은 눈), 누르면 보임(뜬 눈)
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

// 이메일 도메인 목록. '직접 입력' 을 고르면 같은 자리가 입력창으로 바뀐다.
const EMAIL_DOMAINS = ['gmail.com', 'naver.com']

const domainInput = ref(null)

// 목록에서 '직접 입력' 을 고르면 곧바로 타이핑할 수 있게 커서를 옮긴다.
async function onDomainChange() {
  if (form.value.emailDomain !== 'custom') return
  await nextTick()
  domainInput.value?.focus()
}

// 직접 입력을 취소하고 다시 목록으로 되돌린다.
function resetDomain() {
  form.value.emailDomain = ''
  form.value.emailDomainCustom = ''
}

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
            { value: 'male', label: '남성' },
            { value: 'female', label: '여성' },
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
      <div class="flex items-center gap-2">
        <input
          v-model="form.emailLocal"
          type="text"
          placeholder="example"
          class="border-line-field rounded-field placeholder:text-muted-soft h-12 min-w-0 flex-1 border bg-white px-3.5 text-[14px] outline-none"
        />
        <span class="text-muted flex-none text-[14px]">@</span>
        <div class="relative min-w-0 flex-1">
          <!-- 목록에서 고르는 상태 -->
          <template v-if="form.emailDomain !== 'custom'">
            <select
              v-model="form.emailDomain"
              class="border-line-field rounded-field h-12 w-full cursor-pointer appearance-none border bg-white pr-9 pl-3.5 text-[14px] outline-none"
              :class="form.emailDomain === '' ? 'text-muted-soft' : 'text-ink'"
              @change="onDomainChange"
            >
              <option value="" disabled>선택</option>
              <option v-for="domain in EMAIL_DOMAINS" :key="domain" :value="domain">
                {{ domain }}
              </option>
              <option value="custom">직접 입력</option>
            </select>
            <ChevronDown
              class="text-muted-soft pointer-events-none absolute top-1/2 right-3 h-4 w-4 -translate-y-1/2"
            />
          </template>

          <!-- 직접 입력하는 상태 — 같은 자리가 입력창으로 바뀐다 -->
          <template v-else>
            <input
              ref="domainInput"
              v-model="form.emailDomainCustom"
              type="text"
              placeholder="직접 입력"
              class="border-line-field rounded-field placeholder:text-muted-soft h-12 w-full border bg-white pr-9 pl-3.5 text-[14px] outline-none"
            />
            <button
              type="button"
              class="text-muted-soft absolute top-1/2 right-3 -translate-y-1/2 cursor-pointer"
              aria-label="목록에서 고르기"
              @click="resetDomain"
            >
              <X class="h-4 w-4" />
            </button>
          </template>
        </div>
      </div>

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
