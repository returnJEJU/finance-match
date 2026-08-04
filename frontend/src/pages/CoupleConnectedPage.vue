<script setup>
// 파트너 연결 완료 · 레이아웃: BlankLayout
//
// 초대 코드를 입력해 연결에 성공하면(InviteCodePage) 이 화면으로 온다.
// 파트너가 만들어 둔 공동 목표를 확인시킨 뒤 개인 설문으로 넘긴다.
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { ArrowRight, LoaderCircle } from 'lucide-vue-next'
import { getCommonSurvey } from '@/api/invitation'
import CommonGoalSummaryCard from '@/components/couple/CommonGoalSummaryCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterMarried from '@/assets/images/characters/character-married.png'

const route = useRoute()
const router = useRouter()

// 연결 직후 InviteCodePage 가 넘겨준다. 새로고침하면 사라지므로 없을 때를 대비한다.
const partnerName = computed(() =>
  typeof route.query.partnerName === 'string' ? route.query.partnerName.trim() : '',
)

const headline = computed(() =>
  partnerName.value ? `${partnerName.value}님과 연결됐어요!` : '파트너와 연결됐어요!',
)

// queryKey 는 InviteCreatedPage 와 같다 — 방금 받아온 값이 있으면 다시 부르지 않는다.
const {
  data: commonSurvey,
  isLoading,
  isError,
} = useQuery({
  queryKey: ['commonSurvey'],
  queryFn: getCommonSurvey,
})

function goToPersonalSurvey() {
  router.push({ name: 'survey-personal' })
}
</script>

<template>
  <div class="flex min-h-screen flex-col px-5 pt-6 pb-14">
    <div class="flex flex-1 flex-col">
      <div class="text-center">
        <img :src="logoWordmark" alt="찰떡귱합" class="mx-auto w-[74px]" />
        <img :src="characterMarried" alt="" class="mx-auto mt-3.5 w-[180px]" />

        <PageTitle class="mt-2 !text-[32px]" align="center">{{ headline }}</PageTitle>
        <p class="text-muted mt-2 text-[16px] leading-[1.5]">
          파트너가 설정한 공동 목표를 확인한 뒤<br />개인 설문을 진행해 주세요.
        </p>
      </div>

      <!-- 불러오는 동안에는 카드 자리를 비워 두고 표시만 돌린다. -->
      <div v-if="isLoading" class="text-muted flex items-center justify-center py-12">
        <LoaderCircle :size="22" class="animate-spin" />
      </div>

      <template v-else>
        <p v-if="isError" class="mt-5 text-center text-[16px] font-semibold text-warn">
          공동 목표 정보를 불러오지 못했어요.
        </p>

        <CommonGoalSummaryCard :survey="commonSurvey" />
      </template>

      <div class="flex-1"></div>
    </div>

    <BaseButton class="mt-7 flex-none text-[20px]" @click="goToPersonalSurvey">
      개인 설문 시작하기
      <ArrowRight :size="18" :stroke-width="2.4" />
    </BaseButton>
  </div>
</template>
