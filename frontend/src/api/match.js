import { z } from 'zod'
import { api } from '@/api/client'

const compatibilityResultSchema = z.object({
  coupleId: z.number(),
  assetStabilityScore: z.number(),
  debtRepaymentScore: z.number(),
  financialValueScore: z.number(),
  goalFeasibilityScore: z.number(),
  taxStrategyScore: z.number(),
  taxStrategyCalculated: z.boolean(),
  totalScore: z.number(),
  resultSummary: z.string().nullable(),
})

export async function calculateCompatibility() {
  const data = await api.post('/v1/matches/compatibility', {})
  return compatibilityResultSchema.parse(data)
}

export async function getCompatibility() {
  const data = await api.get('/v1/matches/compatibility')
  return compatibilityResultSchema.parse(data)
}
