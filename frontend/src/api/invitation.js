import { z } from 'zod'
import { api } from '@/api/client'

const createInvitationResponseSchema = z.object({
  inviteCode: z.string().length(8),
})

export async function createInvitation(payload) {
  const data = await api.post('/v1/members/me/invitation', payload)
  return createInvitationResponseSchema.parse(data)
}
