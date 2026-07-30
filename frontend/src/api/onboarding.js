import { z } from 'zod'

import { api } from '@/api/client'

const onboardingStatusSchema = z.object({
  coupleConnected: z.boolean(),
  hasInvitation: z.boolean(),
  personalSurveyCompleted: z.boolean(),
  partnerPersonalSurveyCompleted: z.boolean().nullable(),
})

export async function getOnboardingStatus() {
  const data = await api.get('/v1/members/me/onboarding-status')
  return onboardingStatusSchema.parse(data)
}
