<script setup>
// 회원가입 퍼널·설문(공동/개인) 화면 맨 위에 오는 공용 sticky 상단바 — 뒤로가기 + 진행바(+선택
// 타이틀). 세 흐름 모두 생김새가 같아서 하나로 합쳤다: 회원가입은 "몇 번째 화면인지"(step/total),
// 설문은 "몇 문항을 채웠는지"(completedQuestionCount/questionCompletion.length)를 넘기면 된다 —
// 둘 다 "채워진 개수/전체 개수"라 같은 세그먼트 진행바로 표현 가능하다.
//
//   <FunnelHeader :step="2" />                                뒤로가기 있음, 타이틀 없음(회원가입)
//   <FunnelHeader :step="1" :show-back="false" />              첫 단계라 뒤로가기 없음
//   <FunnelHeader :step="n" :total="6" title="개인 설문" />     타이틀 있음(설문)
//
// 진행바가 없는 화면(기관 직접 선택·연동 중)은 생김새가 달라서 이 컴포넌트를 쓰지 않는다.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'

const props = defineProps({
  // 채워진 개수(회원가입: 현재 단계, 설문: 답변한 문항 수)
  step: { type: Number, required: true },
  // 전체 개수(회원가입: 전체 단계 수, 설문: 전체 문항 수)
  total: { type: Number, default: 4 },
  // 첫 단계에서는 뒤로 갈 곳이 없어 숨긴다.
  showBack: { type: Boolean, default: true },
  // 앱 안에 돌아갈 기록이 없을 때(주소 직접 입력·새로고침) 대신 갈 화면. 안 주면 무조건 router.back().
  fallbackTo: { type: [String, Object], default: null },
  // 뒤로가기 옆 가운데 타이틀. 안 주면(회원가입 퍼널) 뒤로가기만 있는 줄로 렌더된다.
  title: { type: String, default: '' },
  // 좌우 여백. 회원가입 퍼널은 이 컴포넌트가 유일한 여백 원천(px-7)이지만, 설문 페이지는 이미
  // px-5 로 패딩된 <section> 안에 들어가므로 본문과 맞춰 px-5 를 넘겨준다(그때 부모의 패딩과
  // 겹치지 않도록 호출부에서 class="-mx-5" 도 같이 준다).
  paddingClass: { type: String, default: 'px-7' },
})

const router = useRouter()

const segments = computed(() => Array.from({ length: props.total }))

// grid-cols-N 을 total 값에 맞춰 동적으로 — Tailwind 정적 클래스로는 표현이 안 돼 인라인 스타일로 준다.
const trackStyle = computed(() => ({
  gridTemplateColumns: `repeat(${props.total}, minmax(0, 1fr))`,
}))

// 주소를 직접 열거나 새로고침하면 앱 안에 기록이 없어 router.back() 이 앱 밖으로 나간다 — 그때만
// fallbackTo 로 대신 보낸다. fallbackTo 를 안 준 화면은 원래도 이 예외 상황을 안 가리던 곳이라
// 그냥 router.back() 그대로 둔다(호출부 동작을 바꾸지 않기 위함).
function goBack() {
  if (window.history.state?.back) {
    router.back()
    return
  }
  if (props.fallbackTo) {
    router.replace(props.fallbackTo)
    return
  }
  router.back()
}
</script>

<template>
  <div class="bg-canvas sticky top-0 z-30 pt-3.5 pb-3" :class="paddingClass">
    <div class="flex flex-none items-center">
      <button
        v-if="showBack"
        type="button"
        class="relative z-10 w-6 flex-none cursor-pointer"
        aria-label="뒤로 가기"
        @click="goBack"
      >
        <ChevronLeft class="h-5 w-5" />
      </button>
      <!-- 뒤로가기가 없어도 진행바 위치가 흔들리지 않도록 같은 폭을 비워둔다. -->
      <div v-else class="w-6 flex-none"></div>

      <h1 v-if="title" class="-ml-6 flex-1 text-center text-[20px] font-semibold text-ink">
        {{ title }}
      </h1>
    </div>

    <div class="mt-2.5">
      <span class="text-brand-ink text-[16px] font-extrabold">{{ step }}/{{ total }}</span>

      <div class="mt-2 grid h-1.5 gap-1" :style="trackStyle">
        <div
          v-for="(_, index) in segments"
          :key="index"
          class="h-full rounded-full transition-colors"
          :class="index < step ? 'bg-brand-deep' : 'bg-line-card'"
        ></div>
      </div>
    </div>
  </div>
</template>
