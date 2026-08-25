package com.financematch.overallcomment;

import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.FirstStepInfo;
import com.financematch.overallcomment.dto.GoalInfo;
import com.financematch.overallcomment.dto.NamesInfo;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 실제 DB에 있는 커플 3쌍(로컬 finance_match DB, 2026-08-25 시점 실측값)의 실제 점수·자산·부채·절세계좌
 * 데이터를 그대로 넣어서 OverallCommentService의 3단계 검증(rule → validator → tone)과 재시도
 * 로직이 실제로 어떻게 동작하는지 관찰하는 1회성 실험용 테스트.
 *
 * <p>세 입력은 강/약 축 분포가 서로 다르게 골랐다:
 * <ul>
 *   <li>지은·도현(couple 201016) — 강점 1(부채) / 약점 4(절세·자산·가치관·목표) : 약점 문단이
 *       두꺼운 경우
 *   <li>하나·두리(couple 1) — 강점 4(목표·절세·부채·자산) / 약점 1(가치관) : 강점 문단이 두꺼운
 *       경우. firstStep은 "약점 중 최약"이 아니라 "5축 중 최저 퍼센트"라 강점으로 분류된 금융
 *       자산(62%)이 firstStep으로 뽑히는 경계 케이스도 포함한다.
 *   <li>유진·민호(couple 4) — 강점 1(가치관 96%) / 약점 4(절세·자산·부채·목표, 전부 20% 미만) :
 *       극단적으로 처참한 케이스
 * </ul>
 *
 * <pre>{@code
 * ./gradlew test --tests "com.financematch.overallcomment.OverallCommentRealCoupleExperimentTest" -DrunLlmExperiment=true
 * }</pre>
 */
@EnabledIfSystemProperty(named = "runLlmExperiment", matches = "true")
class OverallCommentRealCoupleExperimentTest {

    private OverallCommentLlmClient llmClient;
    private OverallCommentRuleValidator ruleValidator;
    private OverallCommentValidator overallCommentValidator;
    private OverallCommentToneValidator toneValidator;

    private void init() throws Exception {
        Properties props = new Properties();
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream("application-secret.properties")) {
            props.load(in);
        }
        String apiKey = props.getProperty("openai.api.key");
        llmClient = new OverallCommentLlmClient(apiKey, "gpt-4o-mini");
        ruleValidator = new OverallCommentRuleValidator();
        overallCommentValidator = new OverallCommentValidator();
        toneValidator = new OverallCommentToneValidator(llmClient);
    }

    // 지은(김지은)·도현(이도현), 32/33세, couple_id=201016, compatibility_result.id=178
    // 점수: 자산43% 부채90% 가치관46% 목표63% 절세16% → 강점=[부채], 약점=[절세,자산,가치관,목표]
    private OverallCommentPromptInput jieunDohyun() {
        return new OverallCommentPromptInput(
                new NamesInfo("지은", "도현"),
                new GoalInfo("부동산 자금 마련", "3억원", "24개월", "1,400만원", "1억 1,120만원", "63%"),
                List.of(new AxisFact("부채 상환", 0.90, List.of("지은 500만원", "도현 500만원"))),
                List.of(
                        new AxisFact(
                                "절세 활용",
                                0.16,
                                List.of(
                                        "지은 ISA 미개설",
                                        "지은 IRP 미개설",
                                        "지은 연금저축 미개설",
                                        "도현 IRP 연 170만원 납입",
                                        "도현 연금저축 미개설")),
                        new AxisFact(
                                "금융 자산", 0.43, List.of("두 분 합산 금융자산 1억 5,400만원", "또래 커플 대비 83%")),
                        new AxisFact("투자 가치관", 0.46, List.of("두 분의 투자 가치관 일치도 46%")),
                        new AxisFact(
                                "목표 달성 가능성",
                                0.63,
                                List.of("24개월 뒤 목표의 63% 도달 예상", "목표까지 1억 1,120만원 부족"))),
                new FirstStepInfo(
                        "절세 활용",
                        List.of(
                                "지은 ISA 미개설",
                                "지은 IRP 미개설",
                                "지은 연금저축 미개설",
                                "도현 IRP 연 170만원 납입",
                                "도현 연금저축 미개설")),
                List.of(
                        "3억원", "1억 1,120만원", "63%", "1억 5,400만원", "83%", "500만원", "46%",
                        "170만원"));
    }

    // 하나(김하나)·두리(이두리), 32/33세, couple_id=1, compatibility_result.id=1
    // 점수: 자산62%(원점수 15.52/25>13 예외로 강점) 부채70% 가치관68% 목표85% 절세80%
    //      → 강점=[목표,절세,부채,자산], 약점=[가치관]
    // firstStep은 5축 중 최저 퍼센트인 자산(62%) — 강점으로 분류됐지만 그게 곧 firstStep이 되는
    // 경계 케이스.
    private OverallCommentPromptInput hanaDuri() {
        return new OverallCommentPromptInput(
                new NamesInfo("하나", "두리"),
                new GoalInfo("부동산 자금 마련", "3억 5,000만원", "3년", "1,000만원", "5,250만원", "85%"),
                List.of(
                        new AxisFact(
                                "목표 달성 가능성",
                                0.85,
                                List.of("3년 뒤 목표의 85% 도달 예상", "목표까지 5,250만원 부족")),
                        new AxisFact(
                                "절세 활용",
                                0.80,
                                List.of(
                                        "하나 ISA 한도까지 1,000만원 남음",
                                        "하나 IRP 미개설",
                                        "하나 연금저축 연 600만원 한도를 채움",
                                        "두리 ISA 연 2,000만원 한도를 채움",
                                        "두리 IRP 연 300만원 납입",
                                        "두리 연금저축 연 600만원 한도를 채움")),
                        new AxisFact("부채 상환", 0.70, List.of("하나 2,000만원", "두리 4,000만원")),
                        new AxisFact(
                                "금융 자산", 0.62, List.of("두 분 합산 금융자산 2억 7,000만원", "또래 커플 대비 145%"))),
                List.of(new AxisFact("투자 가치관", 0.68, List.of("두 분의 투자 가치관 일치도 68%"))),
                new FirstStepInfo(
                        "금융 자산", List.of("두 분 합산 금융자산 2억 7,000만원", "또래 커플 대비 145%")),
                List.of(
                        "3억 5,000만원", "5,250만원", "85%", "2,000만원", "4,000만원", "2억 7,000만원",
                        "145%", "68%", "1,000만원", "600만원", "300만원"));
    }

    // 유진(최유진)·민호(정민호), 31/32세, couple_id=4, compatibility_result.id=3
    // 점수: 자산2% 부채5% 가치관96% 목표19% 절세0% → 강점=[가치관], 약점=[절세,자산,부채,목표]
    // (전부 20% 미만인 극단적으로 처참한 케이스 — 절세는 두 사람 다 계좌 전무)
    private OverallCommentPromptInput yujinMinho() {
        return new OverallCommentPromptInput(
                new NamesInfo("유진", "민호"),
                new GoalInfo("결혼 자금 마련", "8,000만원", "24개월", "210만원", "6,509만원", "19%"),
                List.of(new AxisFact("투자 가치관", 0.96, List.of("두 분의 투자 가치관 일치도 96%"))),
                List.of(
                        new AxisFact(
                                "절세 활용",
                                0.0,
                                List.of(
                                        "유진 ISA 미개설",
                                        "유진 IRP 미개설",
                                        "유진 연금저축 미개설",
                                        "민호 ISA 미개설",
                                        "민호 IRP 미개설",
                                        "민호 연금저축 미개설")),
                        new AxisFact(
                                "금융 자산", 0.02, List.of("두 분 합산 금융자산 590만원", "또래 커플 대비 3%")),
                        new AxisFact("부채 상환", 0.05, List.of("유진 1억 2,000만원", "민호 5,700만원")),
                        new AxisFact(
                                "목표 달성 가능성",
                                0.19,
                                List.of("24개월 뒤 목표의 19% 도달 예상", "목표까지 6,509만원 부족"))),
                new FirstStepInfo(
                        "절세 활용",
                        List.of(
                                "유진 ISA 미개설",
                                "유진 IRP 미개설",
                                "유진 연금저축 미개설",
                                "민호 ISA 미개설",
                                "민호 IRP 미개설",
                                "민호 연금저축 미개설")),
                List.of("8,000만원", "6,509만원", "19%", "590만원", "3%", "1억 2,000만원", "5,700만원", "96%"));
    }

    @Test
    void 실제_커플_3쌍으로_검증기_동작을_관찰한다() throws Exception {
        init();

        run("① 지은·도현 (강점1/약점4, weak-heavy)", jieunDohyun());
        run("② 하나·두리 (강점4/약점1, strong-heavy)", hanaDuri());
        run("③ 유진·민호 (강점1/약점4, 극단적으로 처참)", yujinMinho());
    }

    private void run(String label, OverallCommentPromptInput input) {
        System.out.println();
        System.out.println("================ " + label + " ================");

        OverallCommentPromptBuilder promptBuilder = new OverallCommentPromptBuilder();
        String prompt = promptBuilder.build(input);

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("--- 시도 " + attempt + "/" + maxAttempts + " ---");

            String rawOutput;
            try {
                rawOutput = llmClient.generateComment(prompt);
            } catch (Exception e) {
                System.out.println("  [LLM 호출 실패] " + e.getMessage());
                continue;
            }

            List<String> paragraphs = parseParagraphs(rawOutput);
            if (paragraphs == null) {
                System.out.println("  [파싱 실패] " + rawOutput);
                continue;
            }
            String fullText = String.join("\n\n", paragraphs);
            System.out.println("  [생성된 문단]");
            System.out.println(indent(fullText));

            OverallCommentRuleValidator.ValidationResult ruleResult =
                    ruleValidator.validate(fullText, 550);
            System.out.println("  [1/3 RuleValidator] valid=" + ruleResult.valid()
                    + (ruleResult.valid() ? "" : " violations=" + ruleResult.violations()));
            if (!ruleResult.valid()) {
                continue;
            }

            OverallCommentValidator.ValidationResult contentResult =
                    overallCommentValidator.validate(fullText, input);
            System.out.println("  [2/3 OverallCommentValidator] valid=" + contentResult.valid()
                    + (contentResult.valid() ? "" : " violations=" + contentResult.violations()));
            if (!contentResult.valid()) {
                continue;
            }

            OverallCommentToneValidator.ValidationResult toneResult =
                    toneValidator.validate(fullText, input);
            System.out.println("  [3/3 ToneValidator] valid=" + toneResult.valid()
                    + (toneResult.valid() ? "" : " violations=" + toneResult.violations()));
            if (!toneResult.valid()) {
                continue;
            }

            System.out.println("  => 시도 " + attempt + "회 만에 3단계 검증 전부 통과");
            return;
        }
        System.out.println("  => " + maxAttempts + "회 모두 실패");
    }

    private String indent(String text) {
        return "    " + text.replace("\n", "\n    ");
    }

    private List<String> parseParagraphs(String rawOutput) {
        try {
            String cleaned = rawOutput.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
            }
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(cleaned);
            com.fasterxml.jackson.databind.JsonNode paragraphsNode = root.get("paragraphs");
            if (paragraphsNode == null || !paragraphsNode.isArray() || paragraphsNode.isEmpty()) {
                return null;
            }
            List<String> paragraphs = new java.util.ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode paragraph : paragraphsNode) {
                paragraphs.add(paragraph.asText());
            }
            return paragraphs;
        } catch (Exception e) {
            return null;
        }
    }
}
