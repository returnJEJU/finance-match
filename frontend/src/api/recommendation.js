import { z } from 'zod'
import { api } from '@/api/client'

const optionalTextSchema = z.string().nullable()

const packageProductSchema = z
  .object({
    productId: z.number(),
    productName: z.string(),
    description: optionalTextSchema,
    recommendationReason: optionalTextSchema.optional(),
    productUrl: z.string(),
    comparisonLabel: optionalTextSchema,
    comparisonValue: optionalTextSchema,
    riskLevel: z.number().int().min(1).max(6).nullable(),
    riskLabel: optionalTextSchema,
    loanPurpose: z.enum(['JEONSE', 'HOUSING', 'CAR', 'BUSINESS']).nullable(),
    aum: z.number().int().nonnegative().nullable(),
  })
  .refine(
    ({ comparisonLabel, comparisonValue }) =>
      (comparisonLabel === null) === (comparisonValue === null),
    { message: '상품 비교 항목과 비교값은 함께 제공되어야 합니다.' },
  )

const packageSlotSchema = z.object({
  slotId: z.number(),
  selectedProductId: z.number(),
  slotType: z.enum(['DEPOSIT', 'SAVINGS', 'INVESTMENT', 'LOAN']),
  slotName: z.string(),
  products: z.array(packageProductSchema).min(1),
})

const taxSavingProductSchema = z.object({
  productId: z.number(),
  productName: z.string(),
  description: optionalTextSchema,
  recommendationReason: optionalTextSchema.optional(),
  productUrl: z.string(),
  accountType: z.enum(['ISA', 'PENSION_SAVINGS', 'IRP']),
})

const investmentProductSchema = z.object({
  productId: z.number(),
  productName: z.string(),
  description: optionalTextSchema,
  recommendationReason: optionalTextSchema.optional(),
  productUrl: z.string(),
  riskLevel: z.number().int().min(1).max(6),
  riskLabel: z.enum(['초고위험', '고위험', '위험', '중립', '안정']),
  aum: z.number().int().nonnegative(),
})

const personalTaxSavingSchema = z.object({
  targetMemberId: z.number(),
  targetMemberName: z.string(),
  products: z.array(taxSavingProductSchema).min(1),
})

const personalInvestmentSchema = z.object({
  targetMemberId: z.number(),
  targetMemberName: z.string(),
  products: z.array(investmentProductSchema).min(1),
})

export const recommendationSchema = z.object({
  recommendationId: z.number(),
  packageName: z.string().min(1),
  hasHighInterestDebt: z.boolean(),
  packageSlots: z.array(packageSlotSchema),
  personalTaxSavingRecommendation: personalTaxSavingSchema.nullable(),
  personalInvestmentRecommendation: personalInvestmentSchema.nullable(),
})

export async function getRecommendation() {
  const data = await api.get('/v1/members/me/recommendation')
  return recommendationSchema.parse(data)
}

export async function createRecommendation() {
  await api.post('/v1/members/me/recommendation')
}
