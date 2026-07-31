<script setup>
import { computed } from 'vue'
import {
  ArrowRight,
  ChartNoAxesCombined,
  HandCoins,
  Rocket,
  Scale,
  ShieldCheck,
} from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'

import typeStable from '@/assets/images/characters/types/type-stable.png'
import typeStabilitySeeking from '@/assets/images/characters/types/type-stability-seeking.png'
import typeRiskNeutral from '@/assets/images/characters/types/type-risk-neutral.png'
import typeActive from '@/assets/images/characters/types/type-active.png'
import typeAggressive from '@/assets/images/characters/types/type-aggressive.png'

// TODO(API 연동):
// memberName 상수를 삭제하고 API 응답의 result.name을 템플릿에서 사용한다.
const memberName = 'OO'

// TODO(API 연동):
// 1. headline과 description은 API 응답값을 사용하므로 이 객체에서 제거한다.
// 2. character, icon, accentClass는 프런트 표시 정보이므로 그대로 유지한다.
const investmentTypeMeta = {
  안정형: {
    headline: '안정형에 대한 임시 헤드라인...',
    description: '안정형에 대한 전체 설명...',
    character: typeStable,
    icon: ShieldCheck,
    accentClass: 'text-[#80D39C]',
  },
  안정추구형: {
    headline: '안정추구형에 대한 임시 헤드라인...',
    description: '안정추구형에 대한 전체 설명...',
    character: typeStabilitySeeking,
    icon: HandCoins,
    accentClass: 'text-[#FFABEC]',
  },
  위험중립형: {
    headline: '위험중립형에 대한 임시 헤드라인...',
    description: '위험중립형에 대한 전체 설명...',
    character: typeRiskNeutral,
    icon: Scale,
    accentClass: 'text-[#80C7F1]',
  },
  적극투자형: {
    headline: '적극투자형에 대한 임시 헤드라인...',
    description: '적극투자형에 대한 전체 설명...',
    character: typeActive,
    icon: ChartNoAxesCombined,
    accentClass: 'text-[#FFB231]',
  },
  공격투자형: {
    headline: '공격투자형에 대한 임시 헤드라인...',
    description: '공격투자형에 대한 전체 설명...',
    character: typeAggressive,
    icon: Rocket,
    accentClass: 'text-[#EF4744]',
  },
}

// TODO(API 연동):
// 아래 목데이터를 useQuery의 data로 교체한다.
// 예상 응답: { name, investmentType, headline, description }
const result = {
  investmentType: '위험중립형',
}

// TODO(API 연동):
// useQuery의 data는 ref이므로 investmentTypeMeta[result.value.investmentType]으로 변경한다.
const resultMeta = computed(() => investmentTypeMeta[result.investmentType])
</script>

<template>
  <main class="flex min-h-screen flex-col px-5 pt-6 pb-6">
    <header class="text-center">
      <h1 class="text-[18px] font-bold">나의 금융 스타일 결과</h1>
    </header>

    <section class="flex flex-1 flex-col">
      <!-- 결과 문구 -->
      <div class="mt-10 text-center">
        <p class="text-[19px] font-medium tracking-[-0.2px]">{{ memberName }}님의 금융 스타일은</p>

        <p class="mt-2 flex items-baseline justify-center gap-2 tracking-[-1px]">
          <strong class="text-[44px] leading-[1.2] font-extrabold" :class="resultMeta.accentClass">
            {{ result.investmentType }}
          </strong>

          <span class="text-[26px] font-medium">입니다.</span>
        </p>
      </div>

      <!-- 캐릭터 -->
      <div class="relative mt-4 flex h-[250px] items-center justify-center">
        <div
          class="absolute h-[250px] w-[330px] rounded-full bg-[radial-gradient(circle,_rgba(255,244,79,0.32)_0%,_rgba(255,244,79,0.14)_50%,_transparent_74%)]"
          aria-hidden="true"
        ></div>

        <img
          :src="resultMeta.character"
          :alt="`${result.investmentType} 캐릭터`"
          class="relative h-[210px] w-[210px] object-contain"
        />
      </div>

      <!-- 설명 카드 -->
      <article
        class="border-line-card rounded-card mt-auto flex h-[285px] flex-none flex-col overflow-hidden border bg-white px-6 py-6"
      >
        <div class="flex flex-none items-start gap-2">
          <component
            :is="resultMeta.icon"
            class="mt-0.5 h-5 w-5 flex-none text-[#777000]"
            :stroke-width="2"
            aria-hidden="true"
          />

          <h2 class="text-[18px] leading-[1.4] font-bold">
            {{ resultMeta.headline }}
          </h2>
        </div>

        <div
          class="text-ink-sub mt-4 min-h-0 flex-1 overflow-y-auto pr-2 text-[16px] leading-[1.7]"
        >
          <p>{{ resultMeta.description }}</p>
        </div>
      </article>
    </section>

    <!-- 하단 CTA -->
    <div class="mt-5 flex-none">
      <BaseButton :to="{ name: 'dashboard-waiting' }" class="w-full rounded-full text-[18px]">
        금융 궁합도 확인하러 가기
        <ArrowRight class="h-[19px] w-[19px]" :stroke-width="2.2" aria-hidden="true" />
      </BaseButton>
    </div>
  </main>
</template>
