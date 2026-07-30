<script setup>
// 화면 제목(h1). 글자 크기·굵기·자간이 화면마다 흔들리지 않게 한곳에 모았다.
//
//   <PageTitle class="mt-3.5">금융 궁합을 보기 위한<br />첫 단계예요</PageTitle>
//   <PageTitle class="mt-2" align="center">임민지님의 자산<br />한 번에 찾아볼게요</PageTitle>
//
// 위 여백(mt-*)은 화면마다 달라서 쓰는 쪽에서 class 로 넘긴다.
// 부제는 화면마다 제목과의 간격이 달라(2px~18px) 여기에 넣지 않았다 — 각 화면에서 <p> 로 둔다.
defineProps({
  // left 왼쪽 정렬(기본) · center 가운데 정렬
  align: {
    type: String,
    default: 'left',
    validator: (v) => ['left', 'center'].includes(v),
  },
})
</script>

<template>
  <h1
    class="text-[26px] leading-[1.38] font-extrabold tracking-[-0.4px]"
    :class="align === 'center' ? 'text-center' : ''"
  >
    <slot />
  </h1>
</template>
