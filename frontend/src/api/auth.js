import { z } from 'zod'

import { api } from '@/api/client'
import { onboardingStatusSchema } from '@/api/onboarding'

/**
 * 인증 API — 회원가입 · 로그인.
 *
 * 이 파일은 백엔드 계약을 그대로 말한다. 화면의 입력 형태를 계약 형태로 옮기는 일(생년월일 필드명,
 * 약관 묶기 등)은 호출하는 쪽(스토어)이 한다. 여기서 화면 사정을 알기 시작하면 화면이 바뀔 때마다
 * API 모듈을 고쳐야 한다.
 *
 * 토큰 저장도 여기서 하지 않는다. 저장 위치(localStorage)와 시점은 authStore 의 몫이다.
 */

const signupResponseSchema = z.object({
  member: z.object({
    id: z.number(),
    email: z.string(),
    name: z.string(),
  }),
  // 다음 단계인 자산연동을 인증하기 위한 토큰. 로그인과 별개로 회원가입 직후 발급된다.
  accessToken: z.string(),
})

const loginResponseSchema = z.object({
  accessToken: z.string(),
  // 로그인 응답의 member 에는 email 이 없다(화면에서 쓰지 않는다).
  member: z.object({
    id: z.number(),
    name: z.string(),
  }),
  isFirstLogin: z.boolean(),
  progress: onboardingStatusSchema,
})

const memberProfileSchema = z.object({
  id: z.number(),
  name: z.string(),
})

const withdrawResponseSchema = z.object({
  memberId: z.number(),
  status: z.literal('WITHDRAWN'),
  withdrawnAt: z.string(),
})

/**
 * 회원가입.
 *
 * @param {object} payload 백엔드 계약 그대로의 형태
 *   { name, gender: 'F'|'M', birthDate: 'YYYY-MM-DD', email, password,
 *     agreements: { mydataTerms, privacy, assetLink, coupleShare, marketing } }
 * @throws {ApiError} EMAIL_EXISTS(409) · CONSENT_REQUIRED(400) · INVALID_INPUT(400)
 */
export async function signup(payload) {
  const data = await api.post('/v1/auth/signup', payload)
  return signupResponseSchema.parse(data)
}

/**
 * 로그인.
 *
 * @throws {ApiError} INVALID_CREDENTIALS(401) — 없는 이메일·비밀번호 불일치·탈퇴 회원을
 *   구분하지 않고 하나로 응답한다(계정 열거 방지). 그래서 화면에서도 나눠 안내할 수 없다.
 */
export async function login({ email, password }) {
  const data = await api.post('/v1/auth/login', { email, password })
  return loginResponseSchema.parse(data)
}

/**
 * 로그인 회원 자신의 기본 프로필.
 */
export async function getCurrentMember() {
  const data = await api.get('/v1/members/me')
  return memberProfileSchema.parse(data)
}

/**
 * 로그아웃.
 *
 * 백엔드는 성공 시 data:null 을 반환한다. 토큰 삭제는 호출자가 처리한다.
 */
export async function logout() {
  const data = await api.post('/v1/auth/logout')
  return z.null().parse(data)
}

/**
 * 회원탈퇴.
 *
 * axios delete 의 두 번째 인자는 config 이므로 요청 본문은 반드시 data 안에 넣는다.
 *
 * @throws {ApiError} INVALID_PASSWORD(400) · INVALID_CONFIRMATION(400) ·
 *   MEMBER_ALREADY_WITHDRAWN(409) · UNAUTHORIZED(401) · MEMBER_NOT_FOUND(404)
 */
export async function withdraw({ password, confirmationText }) {
  const data = await api.delete('/v1/members/me', {
    data: {
      password,
      confirmationText: confirmationText.trim(),
    },
  })

  return withdrawResponseSchema.parse(data)
}
