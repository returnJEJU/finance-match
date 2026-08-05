import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 자산 연동 결과 임시 보관소.
 *
 * 연동 화면(`AssetLinkingPage`)이 받은 응답을 완료 화면(`AssetLinkedPage`)까지 넘기기 위한 것이다.
 *
 * 같은 세션에서 완료 화면을 바로 그리기 위한 캐시다. 새로고침·재진입으로 메모리 값이 사라진
 * 경우에는 완료 화면이 자산 조회 API 로 다시 채운다.
 */
export const useAssetLinkStore = defineStore('assetLink', () => {
  /** 연동 응답 전체. { totalAsset, totalDebt, assetCount, summary, accounts, linkedAt } */
  const result = ref(null)

  const hasResult = computed(() => result.value !== null)

  function setResult(value) {
    result.value = value
  }

  function reset() {
    result.value = null
  }

  return { result, hasResult, setResult, reset }
})
