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

// expected_asset 이 없는(마이그레이션 이전) report 행은 백엔드가 null 을 내려준다.
const goalProgressSchema = z
  .object({
    achieved: z.boolean(),
    amountLabel: z.string(),
    barLabel: z.string(),
    availableAsset: z.string(),
    achievementRate: z.string(),
  })
  .nullable()

const reportResponseSchema = z.object({
  name: z.string(),
  partnerName: z.string(),
  totalScore: z.number(),
  scoreAxes: z.array(scoreAxisSchema),
  targetMonths: z.number(),
  loanPurpose: z.string(),
  investmentProfile: investmentProfileSchema,
  goalProgress: goalProgressSchema,
})

export async function getReport() {
  const data = await api.get('/v1/members/me/report')
  return reportResponseSchema.parse(data)
}
