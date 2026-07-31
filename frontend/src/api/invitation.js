import { z } from 'zod'
import { api } from '@/api/client'

const createInvitationResponseSchema = z.object({
  inviteCode: z.string().length(8),
})

const invitationResponseSchema = z.object({
  hasInvitation: z.boolean(),
  inviteCode: z.string().length(8).nullable(),
})

const commonSurveyResponseSchema = z.object({
  goalType1: z.string(),
  goalType2: z.string(),
  targetAmount: z.union([z.number(), z.string()]).transform(Number),
  targetPeriodMonths: z.number(),
  loanPurpose: z.string(),
  hasLoanWithinOneMonth: z.boolean(),
})

export async function createInvitation(payload) {
  const data = await api.post('/v1/members/me/invitation', payload)
  return createInvitationResponseSchema.parse(data)
}

export async function getInvitation() {
  const data = await api.get('/v1/members/me/invitation')
  return invitationResponseSchema.parse(data)
}

export async function getCommonSurvey() {
  const data = await api.get('/v1/members/me/invitation/common-survey')
  return commonSurveyResponseSchema.parse(data)
}
