<script setup>
// 회원가입 - 인증서 선택 (3/4) · 레이아웃: BlankLayout
//
// 실제 간편인증 연동은 하지 않는다. 어떤 인증서를 고를지만 화면 안에서 관리한다.
import { ref } from 'vue'
import { ChevronRight } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import FunnelHeader from '@/components/layout/FunnelHeader.vue'

// 인증서 목록. mark 는 로고 대신 쓰는 머리글자, color 는 각 사의 브랜드 색.
const CERTS = [
  { key: 'kakao', name: '카카오 인증서', mark: 'K', color: '#FAE100', note: '30초만에 인증' },
  { key: 'naver', name: '네이버 인증서', mark: 'N', color: '#03C75A', note: '' },
  { key: 'toss', name: '토스 인증서', mark: 'T', color: '#3182F6', note: '' },
]

const selected = ref('kakao')
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <FunnelHeader :step="3" :fallback-to="{ name: 'signup-agree' }" />

    <div class="flex flex-1 flex-col px-7">
      <PageTitle class="mt-3.5">인증 방법을<br />선택해 주세요.</PageTitle>
      <p class="text-muted mt-4.5 text-[13px] leading-[1.5]">
        평소 쓰던 간편인증으로 30초 만에 끝나요.
      </p>

      <div class="mt-5.5 flex flex-col gap-3.5">
        <button
          v-for="cert in CERTS"
          :key="cert.key"
          type="button"
          class="rounded-card flex cursor-pointer items-center gap-[13px] bg-white px-4 py-[17px] transition-transform duration-100 active:scale-[0.99]"
          :class="
            selected === cert.key ? 'border-brand-deep border-[1.6px]' : 'border-line-card border'
          "
          @click="selected = cert.key"
        >
          <span
            class="flex h-[38px] w-[38px] flex-none items-center justify-center rounded-full text-[17px] font-extrabold text-white"
            :style="{ backgroundColor: cert.color }"
          >
            {{ cert.mark }}
          </span>
          <span class="text-[15px] font-bold">{{ cert.name }}</span>
          <span class="flex-1"></span>
          <span class="text-muted text-[11.5px]">{{ cert.note }}</span>
        </button>
      </div>

      <button
        type="button"
        class="text-muted mt-4.5 cursor-pointer text-center text-[13px] underline underline-offset-[3px]"
      >
        다른 인증서 선택
      </button>

      <div class="flex-1"></div>
    </div>

    <div class="flex flex-none flex-col gap-4.5 px-7 pb-14">
      <button
        type="button"
        class="text-muted flex cursor-pointer items-center px-0.5 text-[12.5px]"
      >
        민간 인증서 개인정보 제3자 제공 필수 동의
        <span class="flex-1"></span>
        <ChevronRight class="h-4 w-4 text-[#C9C9CE]" />
      </button>

      <BaseButton :to="{ name: 'signup-asset' }"> 동의하고 진행 </BaseButton>
    </div>
  </div>
</template>
