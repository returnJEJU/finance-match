<script setup>
import { ArrowRight, Check } from 'lucide-vue-next'

import characterMarried from '@/assets/images/characters/character-married.png'
import BaseButton from '@/components/ui/BaseButton.vue'

const steps = [
  { label: '자산 연동 완료', completed: true },
  { label: '초대 코드 생성', completed: false },
  { label: '파트너 연결', completed: false },
]
</script>

<template>
  <main
    class="flex min-h-screen flex-col bg-gradient-to-br from-[#fff875] via-[#e7f9a6] to-[#9ef1e7] px-6 pt-10 pb-6"
  >
    <!-- 캐릭터 영역 -->
    <section class="flex flex-1 flex-col items-center justify-center text-center">
      <img :src="characterMarried" alt="함께 금융 관리를 시작하는 커플" class="w-[230px]" />

      <h1 class="mt-5 text-[26px] leading-[1.35] font-bold tracking-[-0.04em]">
        둘이서 함께 그려보는<br />
        <span class="text-[#706914]">달콤한 금융 미래</span>
      </h1>

      <p class="text-ink-sub mt-5 text-[15px] leading-[1.7]">
        찰떡귱합이 당신의 소중한 파트너와<br />
        함께하는 자산 관리를 시작합니다.
      </p>

      <!-- 진행 단계 -->
      <div class="mt-8 w-full rounded-[16px] bg-white/70 px-4 py-5 backdrop-blur-sm">
        <ol class="relative grid grid-cols-3">
          <!-- 아이콘 사이 연결선 -->
          <div
            class="bg-line-card absolute top-[18px] right-[16.66%] left-[16.66%] h-[2px]"
            aria-hidden="true"
          ></div>

          <li
            v-for="step in steps"
            :key="step.label"
            class="relative z-10 flex flex-col items-center"
          >
            <div
              class="flex h-9 w-9 items-center justify-center rounded-full"
              :class="step.completed ? 'bg-[#9ceee6]' : 'bg-[#e7e7e7]'"
            >
              <Check
                v-if="step.completed"
                class="h-[18px] w-[18px]"
                stroke-width="3"
                aria-hidden="true"
              />

              <span v-else class="h-2 w-2 rounded-full bg-[#555]" aria-hidden="true"></span>
            </div>

            <span class="mt-2 text-[12px] font-medium tracking-[-0.03em]">
              {{ step.label }}
            </span>
          </li>
        </ol>
      </div>
    </section>

    <!-- 시작 버튼 -->
    <BaseButton
      :to="{ name: 'couple-start' }"
      class="mt-8 h-[58px] shrink-0 rounded-full text-[18px]"
    >
      시작하기
      <ArrowRight class="h-6 w-6" stroke-width="2.3" aria-hidden="true" />
    </BaseButton>
  </main>
</template>
