import { z } from 'zod'

import { api } from '@/api/client'

/**
 * 자산 연동 API.
 *
 * 커플 합산이 아니라 <b>회원별 개인 연동</b>이다. 각자 자기 토큰으로 호출해 자기 자산만 저장하고,
 * 궁합 계산·추천 단계에서 두 사람 것을 조회해 합친다.
 *
 * 지금은 서버가 목데이터를 돌려준다. 회원 ID 가 홀수면 부채 없는 시나리오, 짝수면 전세자금대출이
 * 있는 시나리오다(가입 직후에는 커플이 없어 역할을 정할 수 없어서 쓰는 임시 방편).
 */

/** 자산 카테고리. 백엔드 enum 과 글자까지 같아야 한다. */
export const ASSET_CATEGORY_LABELS = {
  BANK_CHECKING: '계좌·현금',
  BANK_SAVINGS: '예적금',
  SECURITIES: '투자',
  INSURANCE: '보험',
}

const assetLinkResponseSchema = z.object({
  totalAsset: z.number(),
  totalDebt: z.number(),
  /** 연동된 계좌·대출 건수 */
  assetCount: z.number(),
  /**
   * 카테고리별 금액.
   *
   * 응답을 만들 때만 쪼개 주는 값이고 DB 에는 합계만 저장된다 — 즉 <b>재조회로는 이 구성을 다시
   * 얻을 수 없다.</b> 그래서 연동 직후 화면에 보여주려면 이 응답을 들고 있어야 한다.
   */
  summary: z.array(
    z.object({
      category: z.enum(['BANK_CHECKING', 'BANK_SAVINGS', 'SECURITIES', 'INSURANCE']),
      amount: z.number(),
    }),
  ),
  /** 세제혜택 계좌 보유 여부. 연금·ISA 추천 자격 판단에 쓰인다. */
  accounts: z.object({
    hasPensionSavings: z.boolean(),
    hasIrp: z.boolean(),
    hasIsa: z.boolean(),
  }),
  linkedAt: z.string(),
})

/**
 * 자산 연동 (최초 1회).
 *
 * @throws {ApiError} ASSET_ALREADY_LINKED(409) — 이미 연동한 회원.
 *   다시 불러오려면 갱신 API 를 써야 한다.
 * @throws {ApiError} MYDATA_LINK_FAILED(502) — 마이데이터 조회 실패
 */
export async function linkAssets() {
  const data = await api.post('/v1/members/me/assets')
  return assetLinkResponseSchema.parse(data)
}
