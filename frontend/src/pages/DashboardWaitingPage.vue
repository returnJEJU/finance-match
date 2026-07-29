<script setup>
// TODO(담당자): 이 화면을 구현하세요. (기준: 찰떡궁합_UI.pdf)
// 화면: 미완성 대시보드(대기) · 레이아웃: BlankLayout
import { useRouter } from 'vue-router'
import { CreditCard, Heart, LockKeyhole, RefreshCw } from 'lucide-vue-next'

const router = useRouter()

// 새로고침 버튼
const handleRefresh = () => {
  router.push('/match/calculating')
}
</script>

<template>
  <section class="flex min-h-[calc(100vh-108px)] flex-col px-5 py-4">
    <!-- 상단 안내 문구 -->
    <p class="text-center text-[15px] font-semibold text-brand-ink">
      함께 만드는 안정적인 우리의 금융 미래 💛
    </p>

    <!-- 흰색 메인 카드 -->
    <div
      class="mt-5 flex flex-1 flex-col items-center rounded-[30px] bg-white px-6 pt-12 pb-10 shadow-[0_4px_20px_rgba(0,0,0,0.04)]"
    >
      <!-- 자물쇠 영역 -->
      <div class="relative flex h-[230px] w-[230px] items-center justify-center">
        <!-- 연한 노란색 원 -->
        <div class="absolute inset-0 rounded-full bg-brand-soft" />

        <!-- 원 안쪽 장식 -->
        <div class="absolute inset-[18px] rounded-full border-[10px] border-white/50" />

        <!-- 왼쪽 하트 -->
        <Heart class="absolute top-1 left-0 text-[#ebe9dc]" :size="43" :stroke-width="1.8" />

        <!-- 오른쪽 카드 장식 -->
        <CreditCard
          class="absolute right-0 bottom-8 rotate-12 text-[#d7d3c0]"
          :size="36"
          :stroke-width="1.5"
        />

        <!-- 가운데 흰색 원 -->
        <div
          class="relative flex h-[112px] w-[112px] items-center justify-center rounded-full bg-white shadow-[0_8px_20px_rgba(90,85,30,0.16)]"
        >
          <!-- 자물쇠 아이콘 -->
          <LockKeyhole class="text-brand-ink" :size="54" :stroke-width="2.4" />
        </div>
      </div>

      <!-- 안내 문구 -->
      <div class="mt-10 text-center">
        <h1 class="text-[18px] font-bold">아직 궁합을 계산할 수 없습니다</h1>

        <p class="mt-3 text-[15px] leading-6 text-ink-sub">
          상대방이 개인 설문을 완료하면<br />
          금융 궁합도가 자동으로 생성됩니다.
        </p>
      </div>

      <!-- 새로고침 버튼 -->
      <button
        type="button"
        class="mt-12 flex h-12 w-[200px] items-center justify-center gap-2 rounded-full border-2 border-brand bg-white text-[15px] font-semibold transition active:scale-[0.98]"
        @click="handleRefresh"
      >
        <RefreshCw :size="17" :stroke-width="2.2" />
        새로고침
      </button>
    </div>
  </section>
</template>
