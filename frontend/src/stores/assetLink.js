import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 자산 연동 결과 임시 보관소.
 *
 * 연동 화면(`AssetLinkingPage`)이 받은 응답을 완료 화면(`AssetLinkedPage`)까지 넘기기 위한 것이다.
 *
 * <b>완료 화면이 스스로 다시 불러올 수 없어서 필요하다.</b> 자산 API 는 연동(POST)과 갱신(POST)뿐이라
 * 조회 수단이 없고, 카테고리별 금액은 응답을 만들 때만 쪼개 주는 값이라 DB 에도 남지 않는다.
 *
 * 메모리에만 둔다. 새로고침하면 사라지므로 완료 화면은 값이 없을 때 연동 화면으로 돌려보낸다.
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
