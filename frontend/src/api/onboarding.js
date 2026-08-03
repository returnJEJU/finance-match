import { z } from 'zod'

import { api } from '@/api/client'

// 로그인 응답의 progress 도 같은 형태라 api/auth.js 가 가져다 쓴다.
// 복사해 두면 한쪽만 바뀌었을 때 조용히 어긋난다.
export const onboardingStatusSchema = z.object({
  coupleConnected: z.boolean(),
  hasInvitation: z.boolean(),
  personalSurveyCompleted: z.boolean(),
  partnerPersonalSurveyCompleted: z.boolean().nullable(),
})

export async function getOnboardingStatus() {
  const data = await api.get('/v1/members/me/onboarding-status')
  return onboardingStatusSchema.parse(data)
}
