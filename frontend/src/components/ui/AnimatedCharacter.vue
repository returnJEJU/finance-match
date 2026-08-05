<script setup>
// 투자성향 캐릭터 이미지 공용 래퍼 — characters/types/ 안의 어떤 PNG든 src로 넘기면 된다.
//
// 등장(페이지 진입 시 아래서 위로 살짝 튕기며 나타남) → idle(잔잔하게 위아래로 반복)
// 두 단계를 gsap timeline 하나로 묶어서 재생한다. prefers-reduced-motion이면 애니메이션 없이
// 제자리에 바로 보여준다.
import { onMounted, onUnmounted, ref } from 'vue'
import gsap from 'gsap'

const props = defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  // 이미지 자체 크기·object-fit 등. 컨테이너(위치·배경 원 등)는 호출부에서 감싸 쓴다.
  imgClass: { type: String, default: '' },
  // idle 루프 시작을 이만큼(초) 늦춘다. 캐릭터 두 개를 나란히 쓸 때 서로 다른 값을 주면 똑같은
  // 박자로 동시에 떠다니지 않고 살짝 엇갈려 움직인다.
  delay: { type: Number, default: 0 },
})

const el = ref(null)
let ctx

onMounted(() => {
  if (!el.value) return

  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (prefersReducedMotion) return

  ctx = gsap.context(() => {
    const timeline = gsap.timeline({ delay: props.delay })

    // 등장: 아래서 위로 올라오며 튕김
    timeline.from(el.value, {
      y: 40,
      opacity: 0,
      duration: 0.7,
      ease: 'back.out(1.7)',
    })

    // idle: 등장이 끝난 자리를 기준으로 잔잔하게 위아래 반복
    timeline.to(el.value, {
      y: -8,
      duration: 1.6,
      ease: 'sine.inOut',
      repeat: -1,
      yoyo: true,
    })
  }, el.value)
})

onUnmounted(() => {
  ctx?.revert()
})
</script>

<template>
  <img ref="el" :src="src" :alt="alt" :class="imgClass" />
</template>
