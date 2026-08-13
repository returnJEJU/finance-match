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

const assetDetailSchema = z.object({
  referenceLabel: z.string(),
  referenceValue: z.number(),
  referenceProgress: z.number(),
  currentLabel: z.string(),
  currentValue: z.number(),
  progress: z.number(),
  note: z.string(),
})

const investmentValueRowSchema = z.object({
  label: z.string(),
  me: z.string(),
  partner: z.string(),
  match: z.string(),
})

const investmentValueDetailSchema = z.object({
  columns: z.array(z.string()),
  rows: z.array(investmentValueRowSchema),
})

const debtGaugeSchema = z.object({
  label: z.string(),
  value: z.number(),
  decimals: z.number(),
  unit: z.string(),
  threshold: z.number().nullable(),
  thresholdLabel: z.string().nullable(),
  progress: z.number(),
  status: z.string(),
  description: z.string(),
  amountLabel: z.string(),
  amount: z.string(),
})

const debtDetailSchema = z.object({
  summary: z.string(),
  gauges: z.array(debtGaugeSchema),
})

const goalDetailSchema = z.object({
  shortageLabel: z.string(),
  shortageValue: z.number(),
  shortageBadge: z.string(),
  availableAsset: z.string(),
  achievementRate: z.string(),
  progress: z.number(),
  monthlySaving: z.string(),
  minMonthlySaving: z.number(),
  maxMonthlySaving: z.number(),
  selectedMonthlySaving: z.number(),
  baseAchievement: z.number(),
  maxAchievement: z.number(),
  baseShortage: z.number(),
  minShortage: z.number(),
  targetAmount: z.string(),
})

const taxStatusRowSchema = z.object({
  label: z.string(),
  me: z.string(),
  partner: z.string(),
})

const taxDetailSchema = z.object({
  columns: z.array(z.string()),
  rows: z.array(taxStatusRowSchema),
})

const aiCommentSchema = z.object({
  title: z.string(),
  headline: z.string(),
  body: z.string(),
})

const reportDetailsSchema = z.object({
  asset: assetDetailSchema,
  investmentValue: investmentValueDetailSchema,
  debt: debtDetailSchema,
  goal: goalDetailSchema,
  tax: taxDetailSchema,
  aiComment: aiCommentSchema,
})

const reportResponseSchema = z.object({
  name: z.string(),
  partnerName: z.string(),
  totalScore: z.number(),
  scoreAxes: z.array(scoreAxisSchema),
  targetMonths: z.number(),
  loanPurpose: z.string(),
  investmentProfile: investmentProfileSchema,
  goalProgress: goalProgressSchema,
  scoreDetails: reportDetailsSchema,
})

export async function getReport() {
  const data = await api.get('/v1/members/me/report')
  return reportResponseSchema.parse(data)
}

const reportStatusSchema = z.object({
  ready: z.boolean(),
  completedAxes: z.number(),
  totalAxes: z.number(),
})

// 리포트가 아직 준비 중일 때(NOT_FOUND) 진행 바를 그리기 위해 폴링하는 가벼운 조회.
export async function getReportStatus() {
  const data = await api.get('/v1/members/me/report/status')
  return reportStatusSchema.parse(data)
}
