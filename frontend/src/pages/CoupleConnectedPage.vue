<script setup>
// 파트너 연결 완료 · 레이아웃: BlankLayout
//
// 초대 코드를 입력해 연결에 성공하면(InviteCodePage) 이 화면으로 온다.
// 파트너가 만들어 둔 공동 목표를 확인시킨 뒤 개인 설문으로 넘긴다.
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { ArrowRight, ClipboardList, LoaderCircle } from 'lucide-vue-next'
import { getCommonSurvey } from '@/api/invitation'
import BaseButton from '@/components/ui/BaseButton.vue'
import PageTitle from '@/components/ui/PageTitle.vue'
import logoWordmark from '@/assets/images/logo/logo-wordmark.png'
import characterMarried from '@/assets/images/characters/character-married.png'

// 서버가 주는 enum 을 화면에 보일 말로 바꾼다.
// InviteCreatedPage 에도 같은 표가 있다 — 세 번째 화면이 생기면 그때 한곳으로 뺀다.
const goalTypeLabels = {
  INVESTMENT: '여유 자금 투자',
  RETIREMENT: '노후 자금 마련',
  MARRIAGE: '결혼 자금 마련',
  HOUSING: '부동산 자금 마련',
  SHORT_TERM: '단기 자금 운용',
}

const loanPurposeLabels = {
  NONE: '계획 없음',
  JEONSE: '전세 자금',
  HOUSING: '주택 구입',
  CAR: '자동차 구입',
  BUSINESS: '사업/창업',
}

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

const summaryItems = computed(() => [
  {
    label: '목표',
    value: goalTypeLabels[commonSurvey.value?.goalType1] || '-',
  },
  {
    label: '목표 금액',
    value: formatWon(commonSurvey.value?.targetAmount),
  },
  {
    label: '기간',
    value: commonSurvey.value?.targetPeriodMonths
      ? `${commonSurvey.value.targetPeriodMonths}개월`
      : '-',
  },
  {
    label: '대출 계획',
    value: loanPurposeLabels[commonSurvey.value?.loanPurpose] || '-',
  },
])

// 원 단위 숫자를 "1억 2,000만 원" 처럼 읽기 쉬운 말로 바꾼다.
function formatWon(amount) {
  if (!amount) return '-'

  const won = Number(amount)
  const eok = Math.floor(won / 100000000)
  const man = Math.floor((won % 100000000) / 10000)

  if (eok && man) return `${eok}억 ${man.toLocaleString('ko-KR')}만 원`
  if (eok) return `${eok}억 원`
  if (man) return `${man.toLocaleString('ko-KR')}만 원`
  return `${won.toLocaleString('ko-KR')}원`
}

function goToPersonalSurvey() {
  router.push({ name: 'survey-personal' })
}
</script>

<template>
  <div class="flex min-h-screen flex-col px-7 pt-6 pb-14">
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
        <p v-if="isError" class="mt-5 text-center text-[16px] font-semibold text-red-500">
          공동 목표 정보를 불러오지 못했어요.
        </p>

        <section class="rounded-card border-line-card mt-6 border bg-white px-5 py-5 shadow-sm">
          <h2 class="text-ink flex items-center gap-2 text-[20px] font-extrabold">
            <ClipboardList :size="18" class="text-brand-ink" />
            우리의 공동 목표 요약
          </h2>

          <dl class="mt-3.5 grid grid-cols-2 gap-2.5">
            <div
              v-for="item in summaryItems"
              :key="item.label"
              class="rounded-field bg-surface-muted px-3.5 py-3"
            >
              <dt class="text-muted text-[16px] font-bold">{{ item.label }}</dt>
              <dd
                class="text-brand-ink mt-1.5 text-[18px] leading-[1.35] font-extrabold break-keep"
              >
                {{ item.value }}
              </dd>
            </div>
          </dl>
        </section>
      </template>

      <div class="flex-1"></div>
    </div>

    <BaseButton class="mt-7 flex-none text-[20px]" @click="goToPersonalSurvey">
      개인 설문 시작하기
      <ArrowRight :size="18" :stroke-width="2.4" />
    </BaseButton>
  </div>
</template>
