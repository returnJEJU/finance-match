<script setup>
import { AlertCircle } from 'lucide-vue-next'

defineEmits(['confirm'])
</script>

<template>
  <aside class="rounded-card bg-[#fff0f1] px-5 py-4 text-center">
    <div class="flex items-center justify-center gap-1.5 text-[15px] font-bold text-[#ee5d67]">
      <AlertCircle class="h-4 w-4" fill="currentColor" color="white" />
      먼저 갚는 게 이득이에요
    </div>
    <p class="mt-2 text-xs leading-5 text-[#d85a62]">
      고금리 부채가 있어요. 투자·저축보다<br />
      고금리 상환이 먼저예요.
    </p>
    <button
      type="button"
      class="mt-2 text-[11px] text-gray-400 underline"
      @click="$emit('confirm')"
    >
      그래도 추천을 볼게요
    </button>
  </aside>
</template>
