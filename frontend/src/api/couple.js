import { z } from 'zod'
import { api } from '@/api/client'

const coupleProfileMessageSchema = z.object({
  profileMessage: z.string(),
})

const createCoupleResponseSchema = z.object({
  partnerName: z.string(),
})

export async function createCouple(inviteCode) {
  const data = await api.post('/v1/members/me/couple', {
    inviteCode,
  })
  return createCoupleResponseSchema.parse(data)
}

export async function getCoupleProfileMessage() {
  const data = await api.get('/v1/members/me/couple/profile-message')
  return coupleProfileMessageSchema.parse(data)
}

export async function updateCoupleProfileMessage(profileMessage) {
  const data = await api.patch('/v1/members/me/couple/profile-message', {
    profileMessage,
  })
  return coupleProfileMessageSchema.parse(data)
}

export async function disconnectCouple() {
  await api.delete('/v1/members/me/couple')
}
