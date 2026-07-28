<script setup>
// 온보딩 화면 (앱 진입) · 레이아웃: BlankLayout
// 카카오 로그인은 아직 붙이지 않는다. 버튼만 두고 인증 도메인이 준비되면 클릭 핸들러를 채운다.
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterMarried from '@/assets/images/characters/character-married.png'
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <!-- 본문 — 버튼 영역을 뺀 남은 공간을 모두 차지하고, 그 안에서 가운데 정렬.
         버튼 여백을 바꾸면 남는 공간이 달라져 본문도 함께 움직인다. -->
    <div class="flex flex-1 flex-col items-center justify-center px-5 text-center">
      <img :src="logoWordmark" alt="찰떡귱합" class="mb-1.5 w-[260px]" />
      <p class="text-muted mb-1.5 text-[12.5px]">"우리의 금융 궁합도 계산기"</p>
      <img :src="characterMarried" alt="" class="w-[236px]" />

      <h1 class="mt-0.5 text-[26px] leading-[1.38] font-extrabold tracking-[-0.4px]">
        예비 부부를 위한<br />달콤한 금융 궁합
      </h1>
      <p class="text-muted mt-2.5 text-[13px] leading-[1.5]">
        서로 다른 두 사람의 경제력을 하나로,<br />찰떡같이 붙는 자산 관리를 시작해보세요.
      </p>
    </div>

    <!-- 하단 영역
         pb-*   : 로그인 링크를 화면 맨 아래에서 띄우는 거리 -->
    <div class="px-7 pb-[26px]">
      <!-- 버튼 두 개
           gap-*  : 두 버튼 사이 간격
           mb-*   : 로그인 링크와의 거리 → 키우면 버튼 두 개가 위로 올라간다 -->
      <div class="mb-2.5 flex flex-col gap-5">
        <!-- TODO(인증): 카카오 로그인 연동 후 클릭 시 카카오 동의 화면으로 이동 -->
        <button
          type="button"
          class="bg-kakao text-kakao-ink rounded-card flex h-[54px] items-center justify-center text-[16px] font-bold cursor-pointer transition-transform duration-100 active:scale-[0.98]"
        >
          카카오로 계속하기
        </button>

        <RouterLink
          :to="{ name: 'signup' }"
          class="bg-brand rounded-card flex h-[54px] items-center justify-center text-[16px] font-bold cursor-pointer transition-transform duration-100 active:scale-[0.98]"
        >
          시작하기
        </RouterLink>
      </div>

      <!-- '로그인' 만 링크. 앞 문구는 일반 텍스트라 눌러도 흐려지지 않는다. -->
      <p class="text-muted-soft text-center text-[13px]">
        이미 계정이 있나요?
        <RouterLink
          :to="{ name: 'login' }"
          class="text-ink-sub ml-3 font-bold transition-opacity active:opacity-60"
        >
          로그인
        </RouterLink>
      </p>
    </div>
  </div>
</template>
