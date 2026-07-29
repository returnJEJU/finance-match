<script setup>
// 화면 하단의 주 버튼. 높이·모서리·누를 때 반응이 모든 화면에서 같아야 해서 컴포넌트로 뺐다.
//
//   <BaseButton :to="{ name: 'signup' }">시작하기</BaseButton>   → 링크로 렌더
//   <BaseButton @click="...">확인</BaseButton>                    → 버튼으로 렌더
import { computed } from 'vue'

const props = defineProps({
  // 넘기면 RouterLink 로, 없으면 <button> 으로 렌더된다.
  to: { type: [String, Object], default: null },
  // primary 기본 노랑 · kakao 카카오 공식색 · ghost 흰 배경 테두리 · disabled 회색(누를 수 없음)
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'kakao', 'ghost', 'disabled'].includes(v),
  },
})

const VARIANT_CLASS = {
  primary: 'bg-brand text-ink font-bold',
  kakao: 'bg-kakao text-kakao-ink font-bold',
  ghost: 'border-line-field border bg-white font-semibold text-[#444]',
  disabled: 'bg-line-card text-muted-soft font-bold',
}

const isDisabled = computed(() => props.variant === 'disabled')

// 눌리는 버튼에만 손가락 커서와 눌림 효과를 준다.
const interactionClass = computed(() =>
  isDisabled.value ? '' : 'cursor-pointer transition-transform duration-100 active:scale-[0.98]',
)

const tag = computed(() => {
  if (isDisabled.value) return 'button'
  return props.to ? 'RouterLink' : 'button'
})
</script>

<template>
  <component
    :is="tag"
    :to="tag === 'RouterLink' ? to : undefined"
    :type="tag === 'button' ? 'button' : undefined"
    :disabled="isDisabled || undefined"
    class="rounded-card flex h-[54px] items-center justify-center gap-[7px] text-[16px]"
    :class="[VARIANT_CLASS[variant], interactionClass]"
  >
    <slot />
  </component>
</template>
