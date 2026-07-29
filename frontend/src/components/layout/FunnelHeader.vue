<script setup>
// 회원가입 퍼널(1/4 ~ 4/4) 화면 맨 위에 오는 상단바. 뒤로가기 + 진행바 + 단계 표시.
//
//   <FunnelHeader :step="2" />              뒤로가기 있음
//   <FunnelHeader :step="1" :show-back="false" />   첫 단계라 뒤로가기 없음
//
// 진행바가 없는 화면(기관 직접 선택·연동 중)은 생김새가 달라서 이 컴포넌트를 쓰지 않는다.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'

const props = defineProps({
  // 현재 단계 (1~4)
  step: { type: Number, required: true },
  // 전체 단계 수
  total: { type: Number, default: 4 },
  // 첫 단계에서는 뒤로 갈 곳이 없어 숨긴다.
  showBack: { type: Boolean, default: true },
  // 앱 안에 돌아갈 기록이 없을 때(주소 직접 입력·새로고침) 대신 갈 화면
  fallbackTo: { type: [String, Object], default: null },
})

const router = useRouter()

const percent = computed(() => `${(props.step / props.total) * 100}%`)

// 주소를 직접 열거나 새로고침하면 앱 안에 기록이 없어 router.back() 이 앱 밖으로 나간다.
function goBack() {
  if (window.history.state?.back) router.back()
  else if (props.fallbackTo) router.replace(props.fallbackTo)
}
</script>

<template>
  <div class="flex flex-none items-center gap-2.5 px-7 pt-3.5 pb-1.5">
    <button
      v-if="showBack"
      type="button"
      class="w-6 flex-none cursor-pointer"
      aria-label="뒤로 가기"
      @click="goBack"
    >
      <ChevronLeft class="h-5 w-5" />
    </button>
    <!-- 뒤로가기가 없어도 진행바 위치가 흔들리지 않도록 같은 폭을 비워둔다. -->
    <div v-else class="w-6 flex-none"></div>

    <div class="bg-line-card h-1 flex-1 overflow-hidden rounded-full">
      <i class="bg-brand-deep block h-full rounded-full" :style="{ width: percent }"></i>
    </div>

    <span class="text-muted-soft flex-none text-[12px] font-medium">{{ step }}/{{ total }}</span>
  </div>
</template>
