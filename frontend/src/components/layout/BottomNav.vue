<script setup>
import { useRoute } from 'vue-router'

// 준비된 에셋 아이콘 (empty=비활성 / filled=활성)
import homeEmpty from '@/assets/images/icons/home-empty.svg'
import homeFilled from '@/assets/images/icons/home-filled.svg'
import boxEmpty from '@/assets/images/icons/box-empty.svg'
import boxFilled from '@/assets/images/icons/box-filled.svg'
import reportEmpty from '@/assets/images/icons/report-empty.svg'
import reportFilled from '@/assets/images/icons/report-filled.svg'
import personEmpty from '@/assets/images/icons/person-empty.svg'
import personFilled from '@/assets/images/icons/person-filled.svg'

const route = useRoute()

// to: 라우터 등록 시 이 name과 맞춰야 활성 표시가 동작한다.
const tabs = [
  { name: 'dashboard', label: '홈', to: '/dashboard', empty: homeEmpty, filled: homeFilled },
  { name: 'report', label: '리포트', to: '/report', empty: reportEmpty, filled: reportFilled },
  { name: 'recommend', label: '추천', to: '/recommend', empty: boxEmpty, filled: boxFilled },
  { name: 'my', label: '마이페이지', to: '/my', empty: personEmpty, filled: personFilled },
]

// const isActive = (name) => route.name === name
const isActive = (name) => {
  return route.name === name || route.meta.activeTab === name
}
</script>

<template>
  <nav
    class="fixed inset-x-0 bottom-0 z-40 mx-auto flex h-[58px] max-w-[428px] items-center justify-around border-t border-gray-100 bg-white"
  >
    <RouterLink
      v-for="tab in tabs"
      :key="tab.name"
      :to="tab.to"
      class="flex flex-col items-center gap-[3px]"
      :class="{
        'pointer-events-none cursor-default': route.meta.navLocked,
      }"
      :aria-disabled="route.meta.navLocked ? 'true' : undefined"
      :tabindex="route.meta.navLocked ? -1 : 0"
    >
      <img
        :src="isActive(tab.name) ? tab.filled : tab.empty"
        :alt="tab.label"
        class="h-[18px] w-[18px]"
      />
      <span class="text-[10px]" :class="isActive(tab.name) ? 'text-gray-900' : 'text-gray-400'">
        {{ tab.label }}
      </span>
    </RouterLink>
  </nav>
</template>
