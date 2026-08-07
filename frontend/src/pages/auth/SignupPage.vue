<script setup>
// 회원가입 - 정보 입력 (1/4) · 레이아웃: BlankLayout
//
// 입력값은 signupStore 에 담아 다음 단계로 넘긴다. 실제 가입 요청은 3단계(인증서)에서
// 1·2단계를 합쳐 한 번에 보낸다.
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff } from 'lucide-vue-next'
import { useSignupStore } from '@/stores/signup'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import FunnelHeader from '@/components/layout/FunnelHeader.vue'

/**
 * 비밀번호 규칙 — 8~16자, 영문·숫자·특수문자를 모두 포함.
 *
 * 백엔드는 길이(8~64자)만 보고 조합은 보지 않는다. 그래도 화면에서 조합을 요구해 온 만큼
 * 여기서 지킨다. 프론트가 더 엄격한 쪽이라 백엔드가 거절할 값이 통과할 일은 없다.
 */
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,16}$/

/** 백엔드 제약(최대 50자)에 맞춘다. 입력칸의 maxlength 로도 막아 둔다. */
const NAME_MAX_LENGTH = 50

/** 백엔드 @Email 이 거절할 값을 미리 막는다. 공백 없이 a@b.c 꼴이면 통과. */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const router = useRouter()
const signupStore = useSignupStore()

// gender 는 백엔드 enum 값(F·M)을 그대로 담는다. 화면 문구(여성·남성)와 분리해 두면
// 보낼 때 변환하는 단계가 생기고, 빠뜨리면 INVALID_INPUT 이 난다.
//
// 2단계에서 뒤로 돌아왔을 때 다시 입력하지 않도록 스토어에 있던 값으로 시작한다.
// passwordConfirm 은 스토어에 담지 않으므로(보낼 값이 아니다) 비밀번호로 채워 둔다.
const form = ref({ ...signupStore.form, passwordConfirm: signupStore.form.password })

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
  form.value.birthDate = parts.filter(Boolean).join('-')
}

/**
 * 실제로 있는 날짜이면서 지난 날인지.
 *
 * 자릿수만 세면 9999-99-99 나 내일 날짜가 통과해 3단계(가입 요청)에 가서야 거절당한다.
 */
function isValidBirthDate(value) {
  const matched = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!matched) return false

  const [, year, month, day] = matched.map(Number)
  const date = new Date(year, month - 1, day)

  // new Date(2025, 1, 30) 은 3월 2일로 넘어간다. 넣은 값과 되돌려 비교해 없는 날짜를 걸러낸다.
  const isRealDate =
    date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day

  return isRealDate && date < new Date()
}

/** 앞뒤 공백을 뺀 이름. 공백만 넣고 넘어가는 것을 막는다. */
const trimmedName = computed(() => form.value.name.trim())

/**
 * 생년월일 안내.
 *
 * 다 입력했을 때만 본다. 치는 중(예: '1995')에 빨간 문구가 따라다니면 방해가 된다.
 */
const birthDateInvalid = computed(
  () => form.value.birthDate.length === 10 && !isValidBirthDate(form.value.birthDate),
)

/** 이메일 형식 안내. */
const emailInvalid = computed(
  () => form.value.email !== '' && !EMAIL_PATTERN.test(form.value.email),
)

/**
 * 비밀번호 불일치 안내.
 *
 * 확인란을 아직 건드리지 않았을 때는 띄우지 않는다 — 입력을 시작하기도 전에 빨간 문구가 뜨면
 * 잘못한 것처럼 보인다.
 */
const passwordMismatch = computed(
  () => form.value.passwordConfirm !== '' && form.value.password !== form.value.passwordConfirm,
)

/**
 * 비밀번호 규칙 위반 안내.
 *
 * 불일치 안내와 같은 이유로, 아직 아무것도 입력하지 않았을 때는 띄우지 않는다.
 */
const passwordInvalid = computed(
  () => form.value.password !== '' && !PASSWORD_PATTERN.test(form.value.password),
)

/** 다음 단계로 넘길 수 있는지. 백엔드가 거절할 값을 여기서 미리 막는다. */
const canSubmit = computed(() => {
  const { gender, birthDate, email, password, passwordConfirm } = form.value

  return Boolean(
    trimmedName.value &&
    gender &&
    isValidBirthDate(birthDate) &&
    EMAIL_PATTERN.test(email) &&
    PASSWORD_PATTERN.test(password) &&
    password === passwordConfirm,
  )
})

/**
 * 키보드가 올라올 때 입력칸이 가려지지 않게 한다.
 *
 * 아래쪽 칸(비밀번호 등)을 누르면 키보드가 그 칸을 덮은 채로 올라와, 사용자가 다시 스크롤을
 * 내려야 했다. 브라우저가 자동으로 밀어 올려 주기도 하지만 이 화면에서는 그러지 않는다.
 *
 * 키보드 높이는 알 수 없다. 대신 키보드가 올라오면 <b>보이는 영역(visualViewport)이 줄어드는</b>
 * 것을 신호로 삼아, 그 순간 입력 중인 칸을 화면 가운데로 끌어온다. 타이머로 시간을 재면 기기마다
 * 키보드 속도가 달라 어긋난다.
 */
function bringFocusedFieldIntoView() {
  const focused = document.activeElement
  if (!(focused instanceof HTMLInputElement)) return

  // 키보드에 가려지지 않고 실제로 보이는 높이. visualViewport 가 없는 브라우저는 창 높이로 본다.
  const visibleHeight = window.visualViewport?.height ?? window.innerHeight

  // 이미 보이면 그냥 둔다. 화면 크기가 바뀔 때마다 스크롤이 튀면 그게 더 거슬린다.
  if (focused.getBoundingClientRect().bottom <= visibleHeight) return

  // 부드럽게 굴리지 않는다. 키보드가 올라오는 중에 스크롤까지 따로 움직이면 화면이 두 번 흔들린다.
  focused.scrollIntoView({ block: 'center' })
}

// iOS 는 키보드가 올라와도 창 높이(window)가 그대로라 visualViewport 로만 알 수 있고,
// 안드로이드는 창 자체가 줄어든다. 기기마다 오는 신호가 달라 둘 다 받는다.
onMounted(() => {
  window.visualViewport?.addEventListener('resize', bringFocusedFieldIntoView)
  window.addEventListener('resize', bringFocusedFieldIntoView)
})

onUnmounted(() => {
  window.visualViewport?.removeEventListener('resize', bringFocusedFieldIntoView)
  window.removeEventListener('resize', bringFocusedFieldIntoView)
})

/** passwordConfirm 은 화면에서만 쓰는 값이라 스토어에 담지 않는다. */
function goNext() {
  const { gender, birthDate, email, password } = form.value

  signupStore.setForm({ name: trimmedName.value, gender, birthDate, email, password })
  router.push({ name: 'signup-agree' })
}
</script>

<template>
  <!--
    <b>[다음] 버튼을 화면에 고정하지 않는다.</b> 고정하면 키보드가 올라올 때 버튼이 키보드 위로
    따라 올라와 입력칸을 가린다(높이를 화면에 묶으면 dvh·svh 어느 쪽으로도 막을 수 없었다).
    그래서 내용만큼 늘어나게 두고, 버튼은 폼 끝에 두어 <b>스크롤해 내려야 보이게</b> 한다.
  -->
  <div class="flex min-h-dvh flex-col">
    <!-- 앱 안에 뒤로 갈 기록이 없으면(주소 직접 입력·새로고침) 온보딩으로 보낸다. -->
    <FunnelHeader :step="1" :fallback-to="{ name: 'onboarding' }" />

    <!-- pb-8 은 마지막 입력칸과 [다음] 버튼 사이 여백이다. 이걸 안쪽 빈 div 로 두면 내용이
         길어질 때 flex 가 찌그러뜨려 0 이 된다 — 패딩은 그런 일이 없다. -->
    <div class="flex flex-1 flex-col px-7 pb-8">
      <PageTitle class="mt-3.5">금융 궁합을 보기 위한<br />첫 단계예요</PageTitle>
      <p class="text-muted mt-2 text-[13px] leading-[1.5]">
        두 사람에게 맞는 금융상품까지 찾아드려요
      </p>

      <label class="text-ink-sub mt-6 mb-1.5 text-[14px] font-semibold">이름</label>
      <input
        v-model="form.name"
        type="text"
        :maxlength="NAME_MAX_LENGTH"
        placeholder="성함을 입력해 주세요"
        class="border-line-field rounded-field placeholder:text-muted-soft h-12 border bg-white px-3.5 text-[14px] outline-none"
      />

      <span class="text-ink-sub mt-6 mb-1.5 text-[14px] font-semibold">성별</span>
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

      <label class="text-ink-sub mt-6 mb-1.5 text-[14px] font-semibold">생년월일</label>
      <input
        :value="form.birthDate"
        type="text"
        inputmode="numeric"
        maxlength="10"
        placeholder="YYYY-MM-DD"
        class="border-line-field rounded-field placeholder:text-muted-soft h-12 border bg-white px-3.5 text-[14px] outline-none"
        @input="formatBirth"
      />

      <p v-if="birthDateInvalid" class="mt-2 text-[12px] font-medium text-red-500">
        생년월일이 올바르지 않습니다.
      </p>

      <label class="text-ink-sub mt-6 mb-1.5 text-[14px] font-semibold">이메일(아이디)</label>
      <input
        v-model="form.email"
        type="email"
        inputmode="email"
        autocomplete="email"
        placeholder="example@gmail.com"
        class="border-line-field rounded-field placeholder:text-muted-soft h-12 border bg-white px-3.5 text-[14px] outline-none"
      />

      <p v-if="emailInvalid" class="mt-2 text-[12px] font-medium text-red-500">
        이메일 형식이 올바르지 않습니다.
      </p>

      <label class="text-ink-sub mt-6 mb-1.5 text-[14px] font-semibold">비밀번호</label>
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

      <p v-if="passwordInvalid" class="mt-2 text-[12px] font-medium text-red-500">
        8~16자로 영문·숫자·특수문자를 모두 포함해 주세요.
      </p>

      <label class="text-ink-sub mt-6 mb-1.5 text-[14px] font-semibold">비밀번호 확인</label>
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

      <p v-if="passwordMismatch" class="mt-2 text-[12px] font-medium text-red-500">
        비밀번호가 일치하지 않습니다.
      </p>
    </div>

    <div class="flex flex-none flex-col px-7 pb-7">
      <BaseButton :variant="canSubmit ? 'primary' : 'disabled'" @click="goNext"> 다음 </BaseButton>
    </div>
  </div>
</template>
