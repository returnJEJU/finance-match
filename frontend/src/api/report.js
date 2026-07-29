import { z } from 'zod'
import { api } from '@/api/client'

const scoreAxisSchema = z.object({
  key: z.string(),
  name: z.string(),
  score: z.number(),
  maxScore: z.number(),
  reason: z.string(),
})

const investmentProfileSchema = z.object({
  me: z.string(),
  you: z.string(),
  we: z.string(),
})

const reportResponseSchema = z.object({
  name: z.string(),
  partnerName: z.string(),
  totalScore: z.number(),
  scoreAxes: z.array(scoreAxisSchema),
  targetMonths: z.number(),
  loanPurpose: z.string(),
  investmentProfile: investmentProfileSchema,
})

export async function getReport() {
  const data = await api.get('/v1/members/me/report')
  return reportResponseSchema.parse(data)
}
