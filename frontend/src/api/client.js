import axios from 'axios'

/**
 * 공통 API 클라이언트.
 *
 * 백엔드의 공통 응답 래퍼(ApiResponse: success/data/message/code)를 자동으로 처리한다.
 * 팀원은 각 도메인 API 를 만들 때 이 `api` 를 갖다 쓰기만 하면 된다.
 *
 * 【사용법 — 학습 프로젝트와 다른 점】
 *   학습: const { data } = await api.get(url)   // 래퍼가 없어 axios response.data 를 꺼냄
 *   우리: const data   = await api.get(url)      // interceptor 가 이미 래퍼를 벗겨 data 만 반환
 *
 * 실패 시에는 ApiError 가 throw 되므로 try-catch 로 잡는다:
 *   try { const user = await api.post('/auth/login', form) }
 *   catch (e) { e.message // "이미 가입된 이메일", e.code // "EMAIL_EXISTS" }
 */

/** 백엔드가 success:false 로 응답했거나 네트워크가 실패했을 때 던지는 에러. */
export class ApiError extends Error {
  constructor(message, code, status) {
    super(message)
    this.name = 'ApiError'
    this.code = code // 백엔드 ErrorCode enum 이름 (예: "EMAIL_EXISTS")
    this.status = status // HTTP 상태 코드
  }
}

// ===== JWT 토큰 관리 (localStorage) =====
// axios 가 authStore 에 직접 의존하지 않도록 헬퍼로 분리한다(순환 의존 회피).
// 인증 도메인(로그인/로그아웃)이 setAccessToken / clearAccessToken 을 호출한다.
const ACCESS_TOKEN_KEY = 'fm.accessToken'

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function clearAccessToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
}

const instance = axios.create({
  // 개발 시 vite proxy(/api → 8080)를 쓰므로 /api 로 둔다. .env 로 덮어쓸 수 있다.
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
})

// 요청 interceptor: JWT 자동 첨부. 개별 API 함수에서 헤더를 직접 붙이지 않는다.
instance.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 응답 interceptor: ApiResponse 래퍼를 벗기고, 실패는 ApiError 로 변환한다.
instance.interceptors.response.use(
  (response) => {
    const body = response.data

    // 공통 래퍼 형태가 아니면 그대로 통과 (파일 다운로드 등)
    if (body == null || typeof body !== 'object' || !('success' in body)) {
      return response.data
    }

    if (!body.success) {
      throw new ApiError(body.message ?? '요청에 실패했습니다.', body.code, response.status)
    }

    return body.data // 래퍼를 벗겨 data 만 반환
  },
  (error) => {
    const body = error.response?.data
    const status = error.response?.status ?? null

    // 인증 만료 등 401 → 저장된 토큰 정리 (로그인 리다이렉트는 라우터 가드에서)
    if (status === 401) {
      clearAccessToken()
    }

    throw new ApiError(
      body?.message ?? error.message ?? '네트워크 오류가 발생했습니다.',
      body?.code ?? null,
      status,
    )
  },
)

/**
 * 래퍼가 벗겨진 data 를 반환하는 API 헬퍼.
 * endpoint 함수는 이 결과를 Zod 로 파싱한다. (래퍼를 다시 파싱하지 않는다)
 */
export const api = {
  get: (url, config) => instance.get(url, config),
  post: (url, data, config) => instance.post(url, data, config),
  put: (url, data, config) => instance.put(url, data, config),
  patch: (url, data, config) => instance.patch(url, data, config),
  delete: (url, config) => instance.delete(url, config),
}

export default instance
