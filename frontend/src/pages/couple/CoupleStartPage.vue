<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronRight, HeartHandshake, KeyRound, Send } from 'lucide-vue-next'
import characterWaving from '@/assets/images/characters/character-waving.png'
import AppHeader from '@/components/layout/AppHeader.vue'

const router = useRouter()
const isSurveyNoticeOpen = ref(false)

function openSurveyNotice() {
  isSurveyNoticeOpen.value = true
}

function closeSurveyNotice() {
  isSurveyNoticeOpen.value = false
}

function startCoupleSurvey() {
  isSurveyNoticeOpen.value = false
  router.push({ name: 'survey-couple' })
}
</script>

<template>
  <section class="flex min-h-dvh flex-col overflow-hidden bg-canvas">
    <AppHeader
      title="커플 연동"
      variant="plain"
      :fallback-to="{ name: 'service-introduction' }"
      class="[&_h1]:!text-[20px] [&_h1]:!font-semibold"
    />

    <main class="flex flex-1 flex-col px-7 pt-8">
      <div class="text-center">
        <div class="relative mx-auto flex h-[174px] w-[174px] items-center justify-center">
          <span
            class="absolute h-[174px] w-[174px] rounded-full bg-[radial-gradient(circle,_rgba(255,244,79,0.32)_0%,_rgba(255,244,79,0.14)_50%,_transparent_74%)]"
            aria-hidden="true"
          ></span>
          <img
            :src="characterWaving"
            alt="손을 흔드는 찰떡귱합 캐릭터"
            class="relative z-10 h-[154px] w-[154px] object-contain"
          />
        </div>

        <h1 class="mt-3 text-[28px] leading-[1.35] font-extrabold tracking-[-0.03em] text-ink">
          두 분이 하나씩 선택해 주세요
        </h1>
        <p class="mt-2 text-[16px] leading-[1.55] font-medium text-muted">
          한 분은 초대 코드를 만들고,<br />다른 한 분은 받은 코드를 입력해 주세요.
        </p>
      </div>

      <nav class="mt-8 space-y-4" aria-label="커플 연동 방법 선택">
        <button
          type="button"
          class="group rounded-card border-line-card flex min-h-[128px] w-full items-center border bg-white px-5 py-6 text-left shadow-[0_4px_14px_rgba(17,17,17,0.05)] transition duration-150 active:scale-[0.99] active:bg-surface-muted"
          @click="openSurveyNotice"
        >
          <span
            class="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-brand text-brand-ink"
          >
            <Send :size="27" :stroke-width="1.9" aria-hidden="true" />
          </span>

          <span class="ml-4 min-w-0 flex-1 text-left">
            <strong class="block text-[18px] font-extrabold tracking-[-0.02em] text-ink">
              초대 코드 보낼게요
            </strong>
            <span class="mt-1 block text-[16px] leading-[1.45] font-medium text-muted">
              공동 목표를 정하고 새 초대 코드를 <br />만들어요.
            </span>
          </span>

          <ChevronRight
            :size="23"
            :stroke-width="2"
            class="ml-2 shrink-0 text-ink-sub transition-transform group-active:translate-x-0.5"
            aria-hidden="true"
          />
        </button>

        <RouterLink
          :to="{ name: 'couple-invite' }"
          class="group rounded-card border-line-card flex min-h-[128px] items-center border bg-white px-5 py-6 shadow-[0_4px_14px_rgba(17,17,17,0.05)] transition duration-150 active:scale-[0.99] active:bg-surface-muted"
        >
          <span
            class="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-mint text-good"
          >
            <KeyRound :size="27" :stroke-width="1.9" aria-hidden="true" />
          </span>

          <span class="ml-4 min-w-0 flex-1 text-left">
            <strong class="block text-[18px] font-extrabold tracking-[-0.02em] text-ink">
              초대 코드 입력할게요
            </strong>
            <span class="mt-1 block text-[16px] leading-[1.45] font-medium text-muted">
              파트너에게 받은 코드를 입력하고 <br />
              바로 연결해요.
            </span>
          </span>

          <ChevronRight
            :size="23"
            :stroke-width="2"
            class="ml-2 shrink-0 text-ink-sub transition-transform group-active:translate-x-0.5"
            aria-hidden="true"
          />
        </RouterLink>
      </nav>
    </main>

    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div
          v-if="isSurveyNoticeOpen"
          class="fixed inset-0 z-50 flex items-end justify-center bg-black/45 px-5 pb-6 sm:items-center sm:pb-0"
          role="presentation"
          @click.self="closeSurveyNotice"
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="survey-notice-title"
            aria-describedby="survey-notice-description"
            class="w-full max-w-[390px] rounded-[24px] bg-white px-6 pt-7 pb-5 shadow-xl"
          >
            <div
              class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-brand text-gray-900"
            >
              <HeartHandshake :size="29" :stroke-width="2" />
            </div>

            <div class="mt-5 text-center">
              <h2 id="survey-notice-title" class="text-[20px] font-extrabold text-gray-950">
                공동 설문은 초대자가 대표로 답해요
              </h2>

              <p
                id="survey-notice-description"
                class="mt-3 text-[14px] leading-[1.7] font-medium text-gray-600"
              >
                두 분 모두 작성하더라도 공동 결과에는<br />
                <strong class="font-extrabold text-gray-900">
                  초대 코드를 만든 분의 답변만 반영돼요.
                </strong>
              </p>
            </div>

            <div class="mt-5 rounded-[14px] bg-brand-soft px-4 py-3">
              <p class="text-center text-[14px] leading-[1.6] font-semibold text-gray-700">
                그래서 한 분만 작성해도<br />
                궁합·추천·리포트를 함께 이용할 수 있어요.
              </p>
            </div>

            <button
              type="button"
              class="mt-5 flex h-[52px] w-full items-center justify-center rounded-[14px] bg-brand text-[14px] font-extrabold text-gray-950 transition active:scale-[0.99]"
              @click="startCoupleSurvey"
            >
              확인했어요, 설문 시작하기
            </button>

            <button
              type="button"
              class="mt-2 flex h-10 w-full items-center justify-center text-[14px] font-bold text-gray-500"
              @click="closeSurveyNotice"
            >
              뒤로 가기
            </button>
          </section>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>
