import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { login as requestLogin } from '@/api/auth'
import { clearAccessToken, getAccessToken, setAccessToken } from '@/api/client'

/**
 * 로그인 상태.
 *
 * 토큰의 실제 보관은 api/client.js 의 헬퍼(localStorage)에 맡기고, 이 스토어는 "지금 로그인한
 * 상태인가"와 "누구인가"를 앱에 알려준다. 요청에 토큰을 붙이는 일도 client.js 의 인터셉터가 하므로
 * 화면에서 헤더를 만질 일은 없다.
 *
 * <b>토큰은 localStorage, 회원 정보는 메모리에 둔다.</b> 새로고침하면 회원 이름은 사라지지만 토큰은
 * 남는다. 그래서 로그인 여부는 회원 정보가 아니라 <b>토큰 유무</b>로 판단한다 — 그래야 새로고침 후에도
 * 로그인이 유지된다. 이름까지 저장하면 불필요한 개인정보를 디스크에 남기게 된다.
 */
export const useAuthStore = defineStore('auth', () => {
  /**
   * localStorage 는 반응형이 아니라서 화면이 변화를 알아채지 못한다. 그래서 값을 여기에 한 벌
   * 들고 있으면서 저장·삭제 때 함께 갱신한다. 시작 시점에는 localStorage 에서 읽어온다.
   */
  const accessToken = ref(getAccessToken())

  /** { id, name } — 로그인·회원가입 응답으로만 채워진다. 새로고침하면 사라진다. */
  const member = ref(null)

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  /**
   * localStorage 의 실제 값과 다시 맞춘다.
   *
   * client.js 의 응답 인터셉터는 401 을 받으면 스토어를 거치지 않고 토큰을 지운다(토큰 만료).
   * 그러면 이 스토어만 "아직 로그인 중"이라고 착각하므로, 라우터 가드처럼 판단이 필요한 지점에서
   * 먼저 이걸 부른다.
   */
  function syncFromStorage() {
    const stored = getAccessToken()
    if (stored === accessToken.value) return

    accessToken.value = stored
    if (!stored) member.value = null
  }

  /**
   * 로그인.
   *
   * @returns {{isFirstLogin: boolean, progress: object}} 다음에 어느 화면으로 보낼지 판단할 재료.
   *   어디로 보낼지는 화면이 정한다 — 스토어가 라우터를 알면 재사용이 어려워진다.
   * @throws {ApiError} INVALID_CREDENTIALS(401)
   */
  async function login(credentials) {
    const result = await requestLogin(credentials)

    applySession(result.accessToken, result.member)

    return { isFirstLogin: result.isFirstLogin, progress: result.progress }
  }

  /**
   * 회원가입 응답의 토큰으로 로그인 상태를 만든다.
   *
   * 회원가입도 accessToken 을 돌려준다(다음 단계인 자산연동을 인증하기 위한 것). 가입 직후
   * 다시 로그인하지 않아도 되도록 여기서 같은 세션으로 취급한다.
   */
  function applySignup({ accessToken: token, member: signedUpMember }) {
    applySession(token, signedUpMember)
  }

  /**
   * 로그아웃.
   *
   * 서버에는 지울 세션이 없다(JWT). 실제 무효화는 저장된 토큰을 지우는 것으로 이뤄진다.
   * 서버 로그아웃 API 호출은 로그아웃 버튼을 붙일 때 함께 추가한다.
   */
  function logout() {
    clearAccessToken()
    accessToken.value = null
    member.value = null
  }

  function applySession(token, sessionMember) {
    setAccessToken(token)
    accessToken.value = token
    member.value = sessionMember
  }

  return { accessToken, member, isAuthenticated, syncFromStorage, login, applySignup, logout }
})
