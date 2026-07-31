package com.financematch.report.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.dto.reason.FinancialValueReasonInput;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import org.springframework.stereotype.Component;

/**
 * 축별 reason 프롬프트 조립. report_llm_case_practice 의 {@code PromptBuilder}와 같은 역할이다 —
 * 공통 규칙(AGENTIC.md §2) + 축별 규칙(§4) + 입력 JSON을 하나의 프롬프트 문자열로 합친다.
 *
 * <p>금융 자산 축은 결국 ratio 하나로만 판단하는 고정 문장 2개뿐이라 LLM이 필요 없어져
 * {@code AssetStabilityReasonFormatter}(포맷터, LLM 미사용)로 옮겨갔다 — 여기 없다.
 */
@Component
public class ReportPromptBuilder {

    private static final int DEFAULT_MAX_LENGTH = 100;
    // 절세 축은 두 사람·여러 계좌를 같이 언급할 수 있어 팀 확정(GAP-09)으로 길이 예외를 둔다.
    public static final int TAX_STRATEGY_MAX_LENGTH = 200;

    private static final String COMMON_RULES_TEMPLATE =
            """
            [공통 규칙]
            - 뷰어 독립성: '당신/귀하/배우자님/남편분/아내분/상대방/파트너님' 사용 금지. 이름+'님' 또는 '두 분'만 사용.
            - 입력 데이터의 이름은 이미 성을 뗀 축약형이다(GAP-08). 그대로 쓰고, 다시 줄이거나 성을 붙이지 말 것.
            - 입력 데이터에 없는 숫자를 새로 만들어내지 말 것.
            - 해요체로 쓰고, 최대 %d자.
            - 수익 보장·단정적 투자 권유 표현('반드시', '수익이 납니다', '보장' 등) 금지.
            """;

    // GAP-02 팀 확정: 임계값 없이 score 를 직접 비교. 동점일 때만 이름 없이 처리.
    private static final String DEBT_REPAYMENT_RULES =
            """
            [부채 축 규칙]
            - 둘 다 부채 없음 → "두 분 모두 부채가 없어 높은 점수가 나왔어요." 형태 (이름 언급 금지)
            - 정확히 한 명만 부채 있음 → 그 사람(=score 가 더 낮은 쪽) 이름만 언급
              ("부채 점수 감점에 OOO님이 더 큰 영향을 끼쳤어요." 형태)
            - 둘 다 부채 있음 + score 가 다름 → score 가 더 낮은(더 위험한) 쪽 이름만 언급
              ("두 분 모두 부채가 있어요. 하지만 부채 위험도는 OOO님이 더 높아요." 형태)
            - 둘 다 부채 있음 + score 가 정확히 같음 → "두 분 모두 부채 위험도가 높아요." 형태 (이름 언급 금지)
            """;

    // 가치관일치 축은 4문항 diff 계산이 필요하지만, 최종 검증은 서비스 계층이 직접 계산한 값과
    // 대조하므로(ReportRuleValidator 가 아니라 각 *ReasonService), 프롬프트는 결정 규칙만 명시한다.
    private static final String FINANCIAL_VALUE_RULES_TEMPLATE =
            """
            [투자 가치관 일치도 축 규칙]
            - 설문 항목은 다음 4개 고정 어휘만 사용한다(이 외 항목명을 만들어내면 실패):
              총자산 중 금융자산 비중 > 투자 경험 > 금융 투자 상품 이해도 > 손실 감내력
            - 각 항목의 |me-partner| 차이(diff) 중 최댓값이 %d 이하면 → "두 분은 가치관이 비슷해요."
            - 최댓값이 %d 초과이고 diff 4개가 전부 같으면 → "두 분은 네 가지 항목 모두에서 가치관 차이가 있어요."
            - 그 외에는 → "두 분은 {diff 최대 항목}이 차이가 나요." (비슷한 항목은 언급하지 않는다)
              (최댓값이 동점이면 위 고정 순서에서 먼저 오는 항목을 택한다)
            - 개인 이름은 절대 언급하지 않는다. "두 분"만 사용.
            """;

    // GAP-07·GAP-09·GAP-11 팀 확정 반영.
    private static final String TAX_STRATEGY_RULES =
            """
            [절세 축 규칙]
            계좌 3종 이름은 정확히 "ISA", "IRP", "연금저축"만 쓴다. 사람별로 우선순위대로 판단한다:
            1. ISA·IRP·연금저축 세 계좌를 모두 개설 안 했으면 → 개별 나열하지 말고
               "ISA·IRP·연금저축 모두 개설 안 하셨어요" 처럼 뭉뚱그려 표현 + 추천탭 언급 필수
            2. 일부만 미개설이면 → 미개설 계좌만 정확히 지목해 개설 권유 + 추천탭 언급 필수
            3. 전부 개설했지만 일부 계좌가 납입 한도 미달이면 → 그 계좌만 지목해 한도 금액·받을 수 있는
               혜택 금액을 입력값 그대로 명시 (추천탭 언급 불필요)
            4. 전부 개설 + 전부 한도 충족이면 → 그 사람은 언급하지 않는다(문제 없는 사람은 등장 안 함)

            - 두 사람 다 문제(1~3 중 하나)가 있으면 **둘 다** 언급한다(한 명만 골라내지 않음).
            - 두 사람을 같이 언급할 때는 반드시 **이름 가나다순**으로 먼저 등장시킨다
              (보는 사람이 누구든 순서가 같아야 하므로 "나 먼저"는 금지).
            - 두 사람 모두 4번(전부 충족)이면 → 권유 표현 없이 계좌 3종(ISA·IRP·연금저축)을 모두
              언급하는 칭찬 문구를 쓴다.
            - 대상자 이름은 반드시 명시한다("두 분"으로 뭉뚱그리지 않는다).
            """;

    private static final String OUTPUT_INSTRUCTION =
            """
            위 규칙과 입력 데이터만 근거로 reason 문장 하나만 생성하세요.
            문장 외 다른 설명, 마크다운, 따옴표는 출력하지 마세요.
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    public String buildDebtRepaymentPrompt(DebtRepaymentReasonInput input) {
        return build("부채", DEBT_REPAYMENT_RULES, input, DEFAULT_MAX_LENGTH);
    }

    public String buildFinancialValuePrompt(FinancialValueReasonInput input, int threshold) {
        String axisRules = FINANCIAL_VALUE_RULES_TEMPLATE.formatted(threshold, threshold);
        return build("투자 가치관 일치도", axisRules, input, DEFAULT_MAX_LENGTH);
    }

    public String buildTaxStrategyPrompt(TaxStrategyReasonInput input) {
        return build("절세", TAX_STRATEGY_RULES, input, TAX_STRATEGY_MAX_LENGTH);
    }

    private String build(String axisLabel, String axisRules, Object input, int maxLength) {
        String inputJson;
        try {
            inputJson = mapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new IllegalStateException("입력값 직렬화 실패: " + axisLabel, e);
        }

        String commonRules = COMMON_RULES_TEMPLATE.formatted(maxLength);

        return """
                당신은 부부 궁합 리포트의 "%s" 축 reason 문장 생성기입니다.

                %s
                %s
                [입력 데이터]
                %s

                %s
                """
                .formatted(axisLabel, commonRules, axisRules, inputJson, OUTPUT_INSTRUCTION);
    }
}
