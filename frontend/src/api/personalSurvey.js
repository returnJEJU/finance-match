import { z } from 'zod'

import { api } from '@/api/client'

const personalSurveyResultSchema = z.object({
  name: z.string(),
  investmentType: z.string(),
  headline: z.string(),
  description: z.string(),
})

export async function getPersonalSurveyResult() {
  const data = await api.get('/v1/members/me/personal-survey')
  return personalSurveyResultSchema.parse(data)
}
