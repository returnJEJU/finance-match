<script setup>
// 시작 화면 · 레이아웃: BlankLayout
// 디자인: Claude 디자인 시스템 「찰떡귱합 온보딩 UI」 - 01 시작
//
// 카카오 로그인은 아직 붙이지 않았다. UI만 만들어 두고 인증 도메인이
// 준비되면 클릭 핸들러를 채운다. (01a 동의 → 01b 로딩 → 02b 추가정보 순서)
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterMarried from '@/assets/images/characters/character-married.png'
</script>

<template>
  <div class="bg-canvas flex min-h-screen flex-col">
    <!-- 본문 — 남는 공간을 모두 차지하고 그 안에서 가운데 정렬 -->
    <div class="flex flex-1 flex-col items-center justify-center px-5 text-center">
      <img :src="logoWordmark" alt="찰떡귱합" class="mb-1.5 w-[158px]" />
      <p class="text-muted mb-1.5 text-[12.5px]">"우리의 금융 궁합도 계산기"</p>
      <img :src="characterMarried" alt="" class="w-[236px]" />

      <h1 class="mt-0.5 text-[23px] leading-[1.38] font-extrabold tracking-[-0.4px]">
        예비 부부를 위한<br />달콤한 금융 궁합
      </h1>
      <p class="text-muted mt-2.5 text-[13px] leading-[1.5]">
        서로 다른 두 사람의 경제력을 하나로,<br />찰떡같이 붙는 자산 관리를 시작해보세요.
      </p>
    </div>

    <!-- 하단 버튼 -->
    <div class="flex flex-col gap-2.5 px-5 pb-[26px]">
      <!-- TODO(인증): 카카오 로그인 연동 후 클릭 시 카카오 동의 화면으로 이동 -->
      <button
        type="button"
        class="bg-brand-deep rounded-card text-ink flex h-[54px] items-center justify-center gap-[7px] text-[16px] font-bold"
      >
        💬 카카오로 계속하기
      </button>

      <RouterLink
        :to="{ name: 'signup' }"
        class="bg-brand rounded-card text-ink flex h-[54px] items-center justify-center gap-[7px] text-[16px] font-bold"
      >
        시작하기
      </RouterLink>

      <RouterLink :to="{ name: 'login' }" class="text-muted-soft text-center text-[13px]">
        이미 계정이 있나요? <b class="text-ink-sub">로그인</b>
      </RouterLink>
    </div>
  </div>
</template>
