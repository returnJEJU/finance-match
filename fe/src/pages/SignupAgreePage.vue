<script setup>
// 회원가입 - 약관 동의 (2/4) · 레이아웃: BlankLayout
//
// 체크 상태는 화면 안에서만 관리한다. 저장·전송은 인증 도메인이 준비되면 붙인다.
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, ChevronDown, ChevronLeft, TriangleAlert } from 'lucide-vue-next'

// 약관 목록. detail 이 있으면 펼쳐서 내용을 볼 수 있다.
const TERMS = [
  {
    key: 'mydata',
    required: true,
    label: '마이데이터 서비스 이용약관',
    detail:
      '서비스 내용: 본인신용정보 통합조회, 자산 분석 및 궁합 리포트 제공\n이용 기간: 회원 탈퇴 또는 전송요구 철회 시까지',
  },
  {
    key: 'privacy',
    required: true,
    label: '개인정보 수집·이용 동의',
    detail:
      '수집 항목: 계좌·예적금·투자·대출·보험·카드 정보 및 거래내역\n보유 기간: 서비스 이용 종료 또는 삭제 요청 시까지 (1년 미로그인 시 파기)',
  },
  {
    key: 'asset',
    required: true,
    label: '자산정보 연동(마이데이터) 동의',
    detail:
      '전송 요구 항목: 금융기관이 보유한 계좌·거래내역 등 신용정보\n전송 주기: 앱 실행 시 및 1일 1회 자동 갱신 (전송요구는 언제든 철회 가능)',
  },
  {
    key: 'match',
    required: true,
    label: '자산 궁합 매칭·상대방 공유 동의',
    detail:
      '제공 대상: 내가 초대해 연결된 상대방 1인\n제공 항목: 개별 금액·계좌번호가 아닌 궁합 점수·비율 등 분석 결과',
  },
  {
    key: 'marketing',
    required: false,
    label: '마케팅 정보 수신 동의',
    detail:
      '수신 내용: 신규 상품·이벤트 안내 (앱 푸시·이메일)\n철회 방법: 마이페이지 > 알림 설정에서 언제든 해제',
  },
]

const router = useRouter()

const checked = ref({})
const expanded = ref('')

// 전체 동의는 개별 항목이 모두 켜졌을 때만 켜진 것으로 본다.
const allChecked = computed(() => TERMS.every((term) => checked.value[term.key]))

function toggleAll() {
  const next = !allChecked.value
  checked.value = Object.fromEntries(TERMS.map((term) => [term.key, next]))
}

function toggle(key) {
  checked.value = { ...checked.value, [key]: !checked.value[key] }
}

function toggleDetail(key) {
  expanded.value = expanded.value === key ? '' : key
}
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <!-- 상단 진행바 — 4단계 중 2단계 -->
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
        <i class="bg-brand-deep block h-full w-2/4 rounded-full"></i>
      </div>
      <span class="text-muted-soft flex-none text-[12px] font-medium">2/4</span>
    </div>

    <div class="flex flex-1 flex-col px-7">
      <h1 class="mt-3.5 text-[26px] leading-[1.38] font-extrabold tracking-[-0.4px]">
        자산 연동을 위해<br />동의가 필요해요.
      </h1>

      <p
        class="bg-brand-soft text-brand-ink rounded-field mt-4.5 px-3.5 py-3 text-[12px] leading-[1.6]"
      >
        찰떡귱합은 마이데이터로 내 자산 정보를 불러옵니다. 연동한 자산 정보는 내가 초대한 상대방(예:
        예비배우자)에게 <b class="font-bold">궁합 결과 형태로만</b> 공유될 수 있어요.
      </p>

      <!-- 전체 동의 -->
      <button
        type="button"
        class="border-line-field rounded-field mt-5.5 flex h-14 cursor-pointer items-center gap-[11px] border bg-white px-[15px] text-[15px] font-bold transition-transform duration-100 active:scale-[0.99]"
        @click="toggleAll"
      >
        <span
          class="flex h-[22px] w-[22px] flex-none items-center justify-center rounded-full border-[1.5px]"
          :class="
            allChecked
              ? 'bg-brand-deep border-brand-deep text-ink'
              : 'border-[#DADADA] text-transparent'
          "
        >
          <Check class="h-3 w-3" stroke-width="3.5" />
        </span>
        약관 전체 동의
      </button>

      <!-- 개별 약관 -->
      <div class="mt-4">
        <template v-for="(term, index) in TERMS" :key="term.key">
          <hr v-if="!term.required && index > 0" class="border-line-soft my-1.5 border-t" />

          <div class="flex items-center gap-[11px] px-1 py-4 text-[13px]">
            <button
              type="button"
              class="flex h-[22px] w-[22px] flex-none cursor-pointer items-center justify-center rounded-full border-[1.5px]"
              :class="
                checked[term.key]
                  ? 'bg-brand-deep border-brand-deep text-ink'
                  : 'border-[#DADADA] text-transparent'
              "
              :aria-label="`${term.label} 동의`"
              @click="toggle(term.key)"
            >
              <Check class="h-3 w-3" stroke-width="3.5" />
            </button>

            <span class="flex-1 text-[#333]">
              <span v-if="term.required" class="text-required font-bold">[필수]</span>
              <span v-else>[선택]</span>
              {{ term.label }}
            </span>

            <button
              type="button"
              class="flex-none cursor-pointer text-[#C9C9CE]"
              :aria-label="expanded === term.key ? '내용 접기' : '내용 펼치기'"
              @click="toggleDetail(term.key)"
            >
              <ChevronDown
                class="h-4 w-4 transition-transform duration-200"
                :class="expanded === term.key ? 'rotate-180' : ''"
              />
            </button>
          </div>

          <p
            v-if="expanded === term.key"
            class="bg-surface-muted rounded-chip mb-1.5 px-3 py-3 text-[11.5px] leading-[1.65] whitespace-pre-line text-[#6B6B70]"
          >
            {{ term.detail }}
          </p>
        </template>
      </div>

      <p class="text-muted mt-3.5 flex gap-[7px] text-[11px] leading-[1.55]">
        <TriangleAlert class="text-required mt-px h-[13px] w-[13px] flex-none" />
        <span>
          초대한 상대방에게 내 자산 정보가 <b class="text-ink font-bold">궁합 결과로 제공</b>되는 데
          동의합니다.
        </span>
      </p>

      <div class="h-6 flex-1"></div>
    </div>

    <div class="flex flex-none flex-col px-7 pb-14">
      <RouterLink
        :to="{ name: 'signup-cert' }"
        class="bg-brand rounded-card flex h-[54px] cursor-pointer items-center justify-center text-[16px] font-bold transition-transform duration-100 active:scale-[0.98]"
      >
        동의하고 계속하기
      </RouterLink>
    </div>
  </div>
</template>
