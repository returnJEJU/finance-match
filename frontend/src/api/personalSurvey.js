import { z } from 'zod'

import { api } from '@/api/client'

const financialAssetRatioSchema = z.enum([
  'UNDER_10',
  'UNDER_30',
  'UNDER_50',
  'UNDER_80',
  'OVER_80',
])

const investmentExperienceSchema = z.enum([
  'LOW_RISK',
  'MODERATE_LOW_RISK',
  'MODERATE_RISK',
  'MODERATE_HIGH_RISK',
  'HIGH_RISK',
])

const financialKnowledgeSchema = z.enum(['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'])

const capitalPreservationAttitudeSchema = z.enum([
  'ZERO',
  'UNDER_10',
  'UNDER_20',
  'UNDER_50',
  'UNDER_70',
  'FULL',
])

const personalSurveyRequestSchema = z.object({
  annualIncome: z.number().int().min(0).max(999_999_999_999_999),
  monthlyAvailableAmount: z.number().int().min(0).max(999_999_999_999_999),
  financialAssetRatio: financialAssetRatioSchema,
  investmentExperiences: z.array(investmentExperienceSchema).min(1).max(5),
  financialKnowledge: financialKnowledgeSchema,
  capitalPreservationAttitude: capitalPreservationAttitudeSchema,
})

const personalSurveyResultSchema = z.object({
  name: z.string(),
  investmentType: z.string(),
  headline: z.string(),
  description: z.string(),
})

export async function savePersonalSurvey(input) {
  const body = personalSurveyRequestSchema.parse(input)
  const data = await api.put('/v1/members/me/personal-survey', body)
  return z.null().parse(data)
}

export async function getPersonalSurveyResult() {
  const data = await api.get('/v1/members/me/personal-survey')
  return personalSurveyResultSchema.parse(data)
}
