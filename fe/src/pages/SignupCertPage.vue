<script setup>
// 회원가입 - 인증서 선택 (3/4) · 레이아웃: BlankLayout
//
// 실제 간편인증 연동은 하지 않는다. 어떤 인증서를 고를지만 화면 안에서 관리한다.
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

// 인증서 목록. mark 는 로고 대신 쓰는 머리글자, color 는 각 사의 브랜드 색.
const CERTS = [
  { key: 'kakao', name: '카카오 인증서', mark: 'K', color: '#FAE100', note: '30초만에 인증' },
  { key: 'naver', name: '네이버 인증서', mark: 'N', color: '#03C75A', note: '' },
  { key: 'toss', name: '토스 인증서', mark: 'T', color: '#3182F6', note: '' },
]

const router = useRouter()

const selected = ref('kakao')
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <!-- 상단 진행바 — 4단계 중 3단계 -->
    <div class="flex flex-none items-center gap-2.5 px-7 pt-3.5 pb-1.5">
      <button
        type="button"
        class="w-6 flex-none cursor-pointer"
        aria-label="뒤로 가기"
        @click="router.back()"
      >
        <ChevronLeft class="h-5 w-5" />
      </button>
      <div class="bg-line-card h-1 flex-1 overflow-hidden rounded-full">
        <i class="bg-brand-deep block h-full w-3/4 rounded-full"></i>
      </div>
      <span class="text-muted-soft flex-none text-[12px] font-medium">3/4</span>
    </div>

    <div class="flex flex-1 flex-col px-7">
      <h1 class="mt-3.5 text-[26px] leading-[1.38] font-extrabold tracking-[-0.4px]">
        인증 방법을<br />선택해 주세요.
      </h1>
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

      <RouterLink
        :to="{ name: 'signup-asset' }"
        class="bg-brand rounded-card flex h-[54px] cursor-pointer items-center justify-center text-[16px] font-bold transition-transform duration-100 active:scale-[0.98]"
      >
        동의하고 진행
      </RouterLink>
    </div>
  </div>
</template>
