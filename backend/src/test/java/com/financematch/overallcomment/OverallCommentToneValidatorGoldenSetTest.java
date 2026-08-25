package com.financematch.overallcomment;

import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.FirstStepInfo;
import com.financematch.overallcomment.dto.GoalInfo;
import com.financematch.overallcomment.dto.NamesInfo;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * OverallCommentToneValidator 단독의 정확도를 재는 골든셋 실험. {@link
 * OverallCommentRealCoupleExperimentTest}에서 실제 커플 3쌍(지은·도현/하나·두리/유진·민호)의 실제 축
 * 데이터로 LLM이 코멘트를 생성했을 때, 문법상 정상인 문단인데도 ToneValidator가 문장 단위로 걸어서
 * 위반 처리하는 경우(오탐, FP)를 관찰했다. 이 테스트는 그 관찰을 수치로 만든다.
 *
 * <p>커플마다 정답(사람이 직접 판정)이 붙은 케이스 6개를 수동으로 작성한다(직접 LLM으로 생성하지
 * 않고 손으로 쓴 이유: LLM 생성은 매번 문장이 달라져서 "이 텍스트는 명백히 정상/오염이다"라는
 * 고정된 정답을 유지할 수 없다 — 골든셋은 정답이 바뀌지 않아야 재측정·회귀 비교가 의미 있다):
 *
 * <ul>
 *   <li>CLEAN — 강점 문단 긍정, 약점 문단 개선 필요 톤. 정답 valid.
 *   <li>STRONG_CONTAMINATED — 강점 문단에 부정/보완 필요 문구를 섞음. 정답 invalid.
 *   <li>WEAK_INVERTED — 약점 문단을 칭찬 일변도로 뒤집음. 정답 invalid.
 *   <li>ORDER_SWAPPED — CLEAN과 내용은 같고 문단 순서만 약점→강점으로 바꿈. {@link
 *       OverallCommentToneValidator}의 독스트링이 "문단 순서를 가정하지 않는다"고 명시한 걸 그대로
 *       검증한다. 정답 valid.
 *   <li>BOUNDARY_NEUTRAL — CLEAN 약점 문단에 부정어가 전혀 없는 순수 사실 문장 하나를 끼워 넣음(예:
 *       "금융 자산은 1억 5,400만원이에요"). 문단 전체 톤은 여전히 "보완 필요"이므로 정답은 valid —
 *       그런데 이게 정확히 지난 실험에서 ToneValidator가 오탐 낸 패턴이다.
 *   <li>WEAK_ALL_NEUTRAL — 약점 문단 전체를 부정어 없이 순수 사실 나열로만 씀(개선 필요 톤이 어디에도
 *       없음). BOUNDARY_NEUTRAL과 달리 이건 진짜 규칙 위반이다. 정답 invalid.
 * </ul>
 *
 * <p>처음엔 실제 커플 3쌍 × 6케이스 = 18케이스로 시작했다(valid 9 / invalid 9). 그런데 이 3쌍이
 * 전부 "강점1/약점4" 아니면 "강점4/약점1"이라 강·약 분포가 두 극단에만 몰려있었다. 로컬 DB에는
 * 커플이 총 6쌍뿐이라(2026-08-25 시점), 나머지 3쌍을 마저 확인해보니 전부 "강점3/약점2"였다 —
 * 그중 약점 축 조합이 다른 정훈·은영(couple 201017)을 추가했다(실제 데이터, 목표 초과 달성
 * 케이스도 겸함). "강점5/약점0"·"강점0/약점5" 두 극단은 로컬 DB에 아예 없어서(가장 취약한
 * 실사용자층을 대표하는 후자가 특히 중요한데도) 손으로 구성해 추가했다 — weakAxes/strongAxes가
 * 완전히 비면 코드 경로 자체가 달라지므로(문단이 1개뿐, {@link OverallCommentValidator}의 문단
 * 분리 검사가 스킵됨) 실제 데이터가 없어도 반드시 넣어야 한다고 판단했다. 지금은 4쌍(실제 3 +
 * 실제 1) × 6케이스 + 전부강점 3케이스 + 전부약점 4케이스 = 31케이스(valid 16 / invalid 15)다.
 *
 * <pre>{@code
 * ./gradlew test --tests "com.financematch.overallcomment.OverallCommentToneValidatorGoldenSetTest" -DrunLlmExperiment=true
 * }</pre>
 */
@EnabledIfSystemProperty(named = "runLlmExperiment", matches = "true")
class OverallCommentToneValidatorGoldenSetTest {

    private OverallCommentToneValidator toneValidator;

    private void init() throws Exception {
        Properties props = new Properties();
        try (InputStream in =
                getClass().getClassLoader().getResourceAsStream("application-secret.properties")) {
            props.load(in);
        }
        String apiKey = props.getProperty("openai.api.key");
        OverallCommentLlmClient llmClient = new OverallCommentLlmClient(apiKey, "gpt-4o-mini");
        toneValidator = new OverallCommentToneValidator(llmClient);
    }

    private record Case(String coupleLabel, String caseLabel, String text, boolean expectedValid) {}

    // ── 지은·도현 (강점=부채 상환 / 약점=절세·자산·가치관·목표) ──────────────────────────
    private static final String JD_STRONG =
            "부채 상환이 정말 잘 되고 있어요. 지은님과 도현님은 각각 500만원의 부채를 상환하고 있어 안정적인 상황을 유지하고 있어요.";
    private static final String JD_WEAK =
            "절세 활용은 아직 아쉬워요. 지은님은 ISA, IRP, 연금저축을 모두 개설하지 않아 보완이 필요해요. "
                    + "도현님도 IRP만 개설했을 뿐 연금저축은 비어 있어 좀 더 챙기면 좋겠어요. "
                    + "금융 자산 역시 두 분 합산 1억 5,400만원으로 또래 커플 대비 83% 수준이라 아쉬운 편이에요. "
                    + "투자 가치관 일치도도 46%로 낮아 서로 맞춰갈 필요가 있어요. "
                    + "목표 달성 가능성도 63%에 그쳐 1억 1,120만원이 부족한 상황이라 조금 더 노력이 필요해요.";

    private List<Case> jieunDohyunCases() {
        String strongContaminated =
                "부채 상환은 아쉬운 편이에요. 지은님과 도현님은 각각 500만원의 부채를 상환하고 있지만, 여전히 보완이 필요해 보여요.";
        String weakInverted =
                "절세 활용도 아주 잘하고 있어요. 지은님과 도현님 모두 자산 관리에 적극적이에요. "
                        + "금융 자산도 탄탄하고, 투자 가치관도 서로 잘 맞아요. 목표 달성 가능성도 높아요.";
        // 앞 문장의 반복이 아니라, 이 문단에 전혀 없던 새 정보(목표 시점)를 감정 표현 없이 덧붙인다
        // — "반복이라 어색해서 걸리는 건 아닌지"를 가려내기 위한 통제 버전.
        String boundaryNeutral = JD_WEAK + " 두 분의 목표 시점은 24개월 뒤예요.";
        String weakAllNeutral =
                "절세 활용 현황을 보면, 지은님은 ISA·IRP·연금저축이 모두 없고 도현님은 IRP만 있어요. "
                        + "금융 자산은 두 분 합산 1억 5,400만원으로 또래 커플 대비 83%예요. "
                        + "투자 가치관 일치도는 46%이고, 목표 달성 가능성은 63%로 1억 1,120만원이 부족해요.";

        return List.of(
                new Case("지은·도현", "CLEAN", JD_STRONG + "\n\n" + JD_WEAK, true),
                new Case("지은·도현", "STRONG_CONTAMINATED", strongContaminated + "\n\n" + JD_WEAK, false),
                new Case("지은·도현", "WEAK_INVERTED", JD_STRONG + "\n\n" + weakInverted, false),
                new Case("지은·도현", "ORDER_SWAPPED", JD_WEAK + "\n\n" + JD_STRONG, true),
                new Case("지은·도현", "BOUNDARY_NEUTRAL", JD_STRONG + "\n\n" + boundaryNeutral, true),
                new Case("지은·도현", "WEAK_ALL_NEUTRAL", JD_STRONG + "\n\n" + weakAllNeutral, false));
    }

    private OverallCommentPromptInput jieunDohyunInput() {
        return new OverallCommentPromptInput(
                new NamesInfo("지은", "도현"),
                new GoalInfo("부동산 자금 마련", "3억원", "24개월", "1,400만원", "1억 1,120만원", "63%"),
                List.of(new AxisFact("부채 상환", 0.90, List.of("지은 500만원", "도현 500만원"))),
                List.of(
                        new AxisFact("절세 활용", 0.16, List.of("지은 ISA/IRP/연금저축 미개설")),
                        new AxisFact("금융 자산", 0.43, List.of("합산 1억 5,400만원", "또래 대비 83%")),
                        new AxisFact("투자 가치관", 0.46, List.of("일치도 46%")),
                        new AxisFact("목표 달성 가능성", 0.63, List.of("63% 도달 예상", "1억 1,120만원 부족"))),
                new FirstStepInfo("절세 활용", List.of("지은 ISA/IRP/연금저축 미개설")),
                List.of("3억원", "1억 1,120만원", "63%", "1억 5,400만원", "83%", "500만원", "46%"));
    }

    // ── 하나·두리 (강점=목표·절세·부채·자산 / 약점=가치관) ────────────────────────────
    private static final String HD_STRONG =
            "두 분의 목표 달성 가능성이 높아요. 3년 뒤 목표의 85% 도달이 예상되며, 목표까지 5,250만원 부족해요. "
                    + "절세 활용도 잘하고 있어요. 하나님은 ISA 한도까지 1,000만원 남았고, 연금저축은 연 600만원 한도를 다 채우셨어요. "
                    + "두리님 또한 ISA와 연금저축 한도를 채우며, IRP에 연 300만원 납입하고 있어요. "
                    + "부채 상환도 잘 진행되고 있어요. 하나님은 2,000만원, 두리님은 4,000만원을 상환했어요. "
                    + "금융 자산도 안정적이에요. 두 분의 합산 금융자산이 2억 7,000만원이며, 또래 커플 대비 145% 이기도 해요.";
    private static final String HD_WEAK =
            "투자 가치관에 조금 더 신경 쓰면 좋겠어요. 두 분의 투자 가치관의 일치도가 68%로 나타났어요. "
                    + "이를 보완하기 위해 서로의 투자 가치관을 더 논의해보면 좋겠어요.";

    private List<Case> hanaDuriCases() {
        String strongContaminated =
                "두 분의 목표 달성 가능성은 아쉬운 편이에요. 3년 뒤 목표의 85% 도달이 예상되며, 목표까지 5,250만원 부족해요. "
                        + "절세 활용도 잘하고 있어요. 하나님은 ISA 한도까지 1,000만원 남았고, 연금저축은 연 600만원 한도를 다 채우셨어요. "
                        + "두리님 또한 ISA와 연금저축 한도를 채우며, IRP에 연 300만원 납입하고 있어요. "
                        + "부채 상환도 잘 진행되고 있어요. 하나님은 2,000만원, 두리님은 4,000만원을 상환했어요. "
                        + "금융 자산도 안정적이에요. 두 분의 합산 금융자산이 2억 7,000만원이며, 또래 커플 대비 145% 이기도 해요.";
        String weakInverted =
                "투자 가치관도 아주 잘 맞아요. 두 분의 투자 가치관 일치도가 68%로 나타났고, 이 정도면 충분히 훌륭해요.";
        // 반복이 아니라 새 정보(목표 기간)를 감정 표현 없이 덧붙인 통제 버전.
        String boundaryNeutral = HD_WEAK + " 두 분은 3년을 목표 기간으로 설정해뒀어요.";
        String weakAllNeutral = "투자 가치관 일치도는 68%예요. 서로 다른 항목들이 존재해요.";

        return List.of(
                new Case("하나·두리", "CLEAN", HD_STRONG + "\n\n" + HD_WEAK, true),
                new Case("하나·두리", "STRONG_CONTAMINATED", strongContaminated + "\n\n" + HD_WEAK, false),
                new Case("하나·두리", "WEAK_INVERTED", HD_STRONG + "\n\n" + weakInverted, false),
                new Case("하나·두리", "ORDER_SWAPPED", HD_WEAK + "\n\n" + HD_STRONG, true),
                new Case("하나·두리", "BOUNDARY_NEUTRAL", HD_STRONG + "\n\n" + boundaryNeutral, true),
                new Case("하나·두리", "WEAK_ALL_NEUTRAL", HD_STRONG + "\n\n" + weakAllNeutral, false));
    }

    private OverallCommentPromptInput hanaDuriInput() {
        return new OverallCommentPromptInput(
                new NamesInfo("하나", "두리"),
                new GoalInfo("부동산 자금 마련", "3억 5,000만원", "3년", "1,000만원", "5,250만원", "85%"),
                List.of(
                        new AxisFact("목표 달성 가능성", 0.85, List.of("85% 도달 예상")),
                        new AxisFact("절세 활용", 0.80, List.of("ISA/연금저축 한도 채움")),
                        new AxisFact("부채 상환", 0.70, List.of("하나 2,000만원", "두리 4,000만원")),
                        new AxisFact("금융 자산", 0.62, List.of("합산 2억 7,000만원", "또래 대비 145%"))),
                List.of(new AxisFact("투자 가치관", 0.68, List.of("일치도 68%"))),
                new FirstStepInfo("금융 자산", List.of("합산 2억 7,000만원", "또래 대비 145%")),
                List.of("3억 5,000만원", "5,250만원", "85%", "2,000만원", "4,000만원", "2억 7,000만원", "145%", "68%"));
    }

    // ── 유진·민호 (강점=가치관 / 약점=절세·자산·부채·목표) ────────────────────────────
    private static final String YM_STRONG =
            "유진님과 민호님은 투자 가치관이 잘 맞아요. 두 분의 투자 가치관 일치도가 96%로 정말 좋은 편이에요.";
    private static final String YM_WEAK =
            "절세 활용은 아쉬운 부분이에요. 유진님과 민호님 모두 ISA, IRP, 연금저축을 개설하지 않아 보완이 필요해요. "
                    + "금융 자산도 부족한 편이에요. 두 분의 합산 금융자산이 590만원으로 또래 커플 대비 3%에 불과해 늘려나갈 필요가 있어요. "
                    + "부채 상환도 신경 써야 해요. 유진님은 1억 2,000만원, 민호님은 5,700만원의 부채가 있어 부담이 커요. "
                    + "목표 달성 가능성도 아쉬워요. 24개월 뒤 목표의 19%에 그쳐 6,509만원이 부족한 상황이라 노력이 필요해요.";

    private List<Case> yujinMinhoCases() {
        String strongContaminated =
                "유진님과 민호님은 투자 가치관에서 다소 아쉬운 차이를 보여요. 일치도는 96%지만 여전히 보완할 부분이 있어요.";
        String weakInverted =
                "절세 활용도 잘하고 있어요. 금융 자산도 안정적이고, 부채 상환도 순조로워요. 목표 달성 가능성도 높아요.";
        // 반복이 아니라 새 정보(목표 유형)를 감정 표현 없이 덧붙인 통제 버전.
        String boundaryNeutral = YM_WEAK + " 유진님과 민호님은 결혼 자금 마련을 목표로 참여했어요.";
        String weakAllNeutral =
                "절세 현황을 보면 유진님과 민호님 모두 ISA·IRP·연금저축이 없어요. "
                        + "금융 자산은 590만원으로 또래 대비 3%예요. "
                        + "부채는 유진님 1억 2,000만원, 민호님 5,700만원이에요. "
                        + "목표 달성률은 19%로 6,509만원이 부족해요.";

        return List.of(
                new Case("유진·민호", "CLEAN", YM_STRONG + "\n\n" + YM_WEAK, true),
                new Case("유진·민호", "STRONG_CONTAMINATED", strongContaminated + "\n\n" + YM_WEAK, false),
                new Case("유진·민호", "WEAK_INVERTED", YM_STRONG + "\n\n" + weakInverted, false),
                new Case("유진·민호", "ORDER_SWAPPED", YM_WEAK + "\n\n" + YM_STRONG, true),
                new Case("유진·민호", "BOUNDARY_NEUTRAL", YM_STRONG + "\n\n" + boundaryNeutral, true),
                new Case("유진·민호", "WEAK_ALL_NEUTRAL", YM_STRONG + "\n\n" + weakAllNeutral, false));
    }

    private OverallCommentPromptInput yujinMinhoInput() {
        return new OverallCommentPromptInput(
                new NamesInfo("유진", "민호"),
                new GoalInfo("결혼 자금 마련", "8,000만원", "24개월", "210만원", "6,509만원", "19%"),
                List.of(new AxisFact("투자 가치관", 0.96, List.of("일치도 96%"))),
                List.of(
                        new AxisFact("절세 활용", 0.0, List.of("전부 미개설")),
                        new AxisFact("금융 자산", 0.02, List.of("합산 590만원", "또래 대비 3%")),
                        new AxisFact("부채 상환", 0.05, List.of("유진 1억 2,000만원", "민호 5,700만원")),
                        new AxisFact("목표 달성 가능성", 0.19, List.of("19% 도달 예상", "6,509만원 부족"))),
                new FirstStepInfo("절세 활용", List.of("전부 미개설")),
                List.of("8,000만원", "6,509만원", "19%", "590만원", "3%", "1억 2,000만원", "5,700만원", "96%"));
    }

    // ── 정훈·은영 (couple_id=201017, 실제 DB — 강점=목표·부채·가치관 / 약점=절세·자산) ──────
    // 로컬 DB에 실제로 존재하는 6쌍 중 나머지 3쌍(couple 2,3,201017)이 전부 "강점3/약점2"로
    // 몰려있어서, 그중 약점 축 조합이 다른(자산+절세, 다른 두 후보는 둘 다 가치관+절세) 이 커플을
    // 골랐다. 목표를 초과 달성한(achievementRate>100%) 케이스라 shortfall=null인 것도 새로운
    // 경로다.
    private static final String JH_STRONG =
            "두 분의 목표 달성 가능성이 아주 높아요. 15년 뒤 목표를 초과 달성할 것으로 예상되며, 예상 자산은 10억 2,960만원에 이를 것 같아요. "
                    + "부채 상환도 잘 관리되고 있어요. 정훈님과 은영님 각각 1,000만원의 부채를 갖고 있는데, 상환 부담이 크지 않아요. "
                    + "투자 가치관도 잘 맞아요. 두 분의 투자 가치관 일치도가 77%로 높은 편이에요.";
    private static final String JH_WEAK =
            "절세 활용은 아쉬운 부분이에요. 정훈님과 은영님 모두 ISA, IRP, 연금저축을 하나도 개설하지 않아 보완이 필요해요. "
                    + "금융 자산도 부족한 편이에요. 두 분의 합산 금융자산이 8,000만원으로 또래 커플 대비 47%에 그쳐 늘려나갈 필요가 있어요.";

    private List<Case> jeonghunEunyoungCases() {
        String strongContaminated =
                "두 분의 목표 달성 가능성은 아쉬운 편이에요. 15년 뒤 목표를 초과 달성할 것으로 예상되며, 예상 자산은 10억 2,960만원에 이를 것 같아요. "
                        + "부채 상환도 잘 관리되고 있어요. 정훈님과 은영님 각각 1,000만원의 부채를 갖고 있는데, 상환 부담이 크지 않아요. "
                        + "투자 가치관도 잘 맞아요. 두 분의 투자 가치관 일치도가 77%로 높은 편이에요.";
        String weakInverted = "절세 활용도 아주 잘하고 있어요. 금융 자산도 안정적인 편이에요.";
        // 반복이 아니라 새 정보(목표 기간)를 감정 표현 없이 덧붙인 통제 버전.
        String boundaryNeutral = JH_WEAK + " 두 분의 목표 기간은 15년이에요.";
        String weakAllNeutral =
                "절세 현황을 보면 정훈님과 은영님 모두 ISA·IRP·연금저축이 없어요. "
                        + "금융 자산은 8,000만원으로 또래 대비 47%예요.";

        return List.of(
                new Case("정훈·은영", "CLEAN", JH_STRONG + "\n\n" + JH_WEAK, true),
                new Case("정훈·은영", "STRONG_CONTAMINATED", strongContaminated + "\n\n" + JH_WEAK, false),
                new Case("정훈·은영", "WEAK_INVERTED", JH_STRONG + "\n\n" + weakInverted, false),
                new Case("정훈·은영", "ORDER_SWAPPED", JH_WEAK + "\n\n" + JH_STRONG, true),
                new Case("정훈·은영", "BOUNDARY_NEUTRAL", JH_STRONG + "\n\n" + boundaryNeutral, true),
                new Case("정훈·은영", "WEAK_ALL_NEUTRAL", JH_STRONG + "\n\n" + weakAllNeutral, false));
    }

    private OverallCommentPromptInput jeonghunEunyoungInput() {
        return new OverallCommentPromptInput(
                new NamesInfo("정훈", "은영"),
                new GoalInfo("노후 자금 마련", "10억원", "180개월", "1,000만원", null, "103%"),
                List.of(
                        new AxisFact("목표 달성 가능성", 1.0, List.of("초과 달성 예상")),
                        new AxisFact("부채 상환", 0.86, List.of("정훈 1,000만원", "은영 1,000만원")),
                        new AxisFact("투자 가치관", 0.77, List.of("일치도 77%"))),
                List.of(
                        new AxisFact("절세 활용", 0.0, List.of("전부 미개설")),
                        new AxisFact("금융 자산", 0.28, List.of("합산 8,000만원", "또래 대비 47%"))),
                new FirstStepInfo("절세 활용", List.of("전부 미개설")),
                List.of("10억원", "103%", "1,000만원", "77%", "8,000만원", "47%"));
    }

    // ── 가상 커플: 전부 강점(강점 5/약점 0) ────────────────────────────────────────────
    // 로컬 DB의 실제 커플 6쌍 중엔 이 분포가 없다(전부 강점3~4 이하) — weakAxes가 완전히 비어서
    // OverallCommentValidator의 문단 분리 검사 자체가 스킵되는, 코드 경로가 다른 극단 케이스라
    // 실제 데이터가 없어도 반드시 넣어야 한다고 판단해 손으로 구성했다. 실제 커플이 아니므로
    // 이름은 가상의 이름(가온·다온)을 썼다.
    private static final String ALL_STRONG_TEXT =
            "두 분은 재정적으로 아주 탄탄해요. 금융 자산이 합산 4억원으로 또래 커플 대비 200%에 달하고, "
                    + "부채 상환도 순조로워서 부담이 거의 없어요. 투자 가치관도 92%로 서로 잘 맞고, "
                    + "목표 달성 가능성도 높아서 3년 뒤 목표를 초과 달성할 것으로 예상돼요. "
                    + "절세 활용도 잘하고 있어서 두 분 모두 ISA와 연금저축 한도를 다 채웠어요.";

    private List<Case> allStrongCases() {
        String contaminated =
                "두 분은 재정적으로 다소 아쉬운 편이에요. 금융 자산이 합산 4억원으로 또래 커플 대비 200%에 달하고, "
                        + "부채 상환도 순조로워서 부담이 거의 없어요. 투자 가치관도 92%로 서로 잘 맞고, "
                        + "목표 달성 가능성도 높아서 3년 뒤 목표를 초과 달성할 것으로 예상돼요. "
                        + "절세 활용도 잘하고 있어서 두 분 모두 ISA와 연금저축 한도를 다 채웠어요.";
        // 반복이 아니라 새 정보(나이 차이)를 감정 표현 없이 덧붙인 통제 버전.
        String boundaryNeutral = ALL_STRONG_TEXT + " 두 분의 나이 차이는 2살이에요.";

        return List.of(
                new Case("전부강점(가온·다온)", "CLEAN", ALL_STRONG_TEXT, true),
                new Case("전부강점(가온·다온)", "CONTAMINATED", contaminated, false),
                new Case("전부강점(가온·다온)", "BOUNDARY_NEUTRAL", boundaryNeutral, true));
    }

    private OverallCommentPromptInput allStrongInput() {
        return new OverallCommentPromptInput(
                new NamesInfo("가온", "다온"),
                new GoalInfo("여유 자금 투자", "2억원", "36개월", "4억원", null, "150%"),
                List.of(
                        new AxisFact("금융 자산", 0.95, List.of("합산 4억원", "또래 대비 200%")),
                        new AxisFact("부채 상환", 0.95, List.of("부담 거의 없음")),
                        new AxisFact("투자 가치관", 0.92, List.of("일치도 92%")),
                        new AxisFact("목표 달성 가능성", 0.95, List.of("초과 달성 예상")),
                        new AxisFact("절세 활용", 0.95, List.of("ISA/연금저축 한도 채움"))),
                List.of(),
                new FirstStepInfo("절세 활용", List.of("ISA/연금저축 한도 채움")),
                List.of("4억원", "200%", "92%", "2억원", "150%"));
    }

    // ── 가상 커플: 전부 약점(강점 0/약점 5) ────────────────────────────────────────────
    // 재정적으로 가장 취약한 실사용자층을 대표하는 극단 케이스. 마찬가지로 로컬 DB엔 이 분포의
    // 실제 커플이 없어서 손으로 구성했다. 이름은 가상(서준·하람).
    private static final String ALL_WEAK_TEXT =
            "절세 활용은 아쉬운 부분이에요. 두 분 모두 ISA, IRP, 연금저축을 하나도 개설하지 않아 보완이 필요해요. "
                    + "금융 자산도 부족해서 합산 500만원으로 또래 커플 대비 3%에 그쳐 늘려나갈 필요가 있어요. "
                    + "부채 상환도 신경 써야 해요. 두 분 다 부채 부담이 커서 상환 계획이 필요해요. "
                    + "투자 가치관도 차이가 커서 조율이 필요해요. "
                    + "목표 달성 가능성도 낮아서 지금 속도로는 부족한 상황이라 노력이 필요해요.";

    private List<Case> allWeakCases() {
        String inverted =
                "절세 활용도 아주 잘하고 있어요. 금융 자산도 안정적이고, 부채 상환도 순조로워요. "
                        + "투자 가치관도 잘 맞고, 목표 달성 가능성도 높아요.";
        String allNeutral =
                "절세 현황을 보면 두 분 모두 ISA·IRP·연금저축이 없어요. 금융 자산은 500만원으로 또래 대비 3%예요. "
                        + "부채가 있고, 투자 가치관 일치도는 낮은 편이에요. 목표 달성률은 낮은 수준이에요.";
        // 반복이 아니라 새 정보(나이 차이)를 감정 표현 없이 덧붙인 통제 버전.
        String boundaryNeutral = ALL_WEAK_TEXT + " 두 분의 나이 차이는 3살이에요.";

        return List.of(
                new Case("전부약점(서준·하람)", "CLEAN", ALL_WEAK_TEXT, true),
                new Case("전부약점(서준·하람)", "WEAK_INVERTED", inverted, false),
                new Case("전부약점(서준·하람)", "WEAK_ALL_NEUTRAL", allNeutral, false),
                new Case("전부약점(서준·하람)", "BOUNDARY_NEUTRAL", boundaryNeutral, true));
    }

    private OverallCommentPromptInput allWeakInput() {
        return new OverallCommentPromptInput(
                new NamesInfo("서준", "하람"),
                new GoalInfo("결혼 자금 마련", "1억원", "24개월", "50만원", "9,500만원", "5%"),
                List.of(),
                List.of(
                        new AxisFact("절세 활용", 0.0, List.of("전부 미개설")),
                        new AxisFact("금융 자산", 0.03, List.of("합산 500만원", "또래 대비 3%")),
                        new AxisFact("부채 상환", 0.1, List.of("부담 큼")),
                        new AxisFact("투자 가치관", 0.2, List.of("일치도 낮음")),
                        new AxisFact("목표 달성 가능성", 0.05, List.of("도달률 낮음"))),
                new FirstStepInfo("절세 활용", List.of("전부 미개설")),
                List.of("1억원", "9,500만원", "5%", "500만원", "3%"));
    }

    @Test
    void 골든셋으로_ToneValidator의_FPR_FNR을_측정한다() throws Exception {
        init();

        List<Case> allCases = new ArrayList<>();
        allCases.addAll(jieunDohyunCases());
        allCases.addAll(hanaDuriCases());
        allCases.addAll(yujinMinhoCases());
        allCases.addAll(jeonghunEunyoungCases());
        allCases.addAll(allStrongCases());
        allCases.addAll(allWeakCases());

        java.util.Map<String, OverallCommentPromptInput> inputByLabel =
                java.util.Map.of(
                        "지은·도현", jieunDohyunInput(),
                        "하나·두리", hanaDuriInput(),
                        "유진·민호", yujinMinhoInput(),
                        "정훈·은영", jeonghunEunyoungInput(),
                        "전부강점(가온·다온)", allStrongInput(),
                        "전부약점(서준·하람)", allWeakInput());

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        System.out.println();
        System.out.println("case                              expected  actual  result");
        System.out.println("--------------------------------------------------------------");

        for (Case c : allCases) {
            OverallCommentPromptInput input = inputByLabel.get(c.coupleLabel());
            OverallCommentToneValidator.ValidationResult result = toneValidator.validate(c.text(), input);
            boolean actualValid = result.valid();

            // "positive"=위반(invalid) 관례. expected invalid를 Positive로 본다.
            String verdict;
            if (c.expectedValid() && actualValid) {
                tn++;
                verdict = "OK (TN)";
            } else if (c.expectedValid() && !actualValid) {
                fp++;
                verdict = "MISS -> FALSE POSITIVE";
            } else if (!c.expectedValid() && !actualValid) {
                tp++;
                verdict = "OK (TP)";
            } else {
                fn++;
                verdict = "MISS -> FALSE NEGATIVE";
            }

            System.out.printf(
                    "%-6s %-20s expected=%-8s actual=%-8s %s%n",
                    c.coupleLabel(), c.caseLabel(), label(c.expectedValid()), label(actualValid), verdict);
            if (!result.violations().isEmpty()) {
                System.out.println("       violations=" + result.violations());
            }
        }

        int actualValidCount = tn + fp; // 정답이 valid인 케이스 총합
        int actualInvalidCount = tp + fn; // 정답이 invalid인 케이스 총합
        double fpr = actualValidCount == 0 ? 0 : (double) fp / actualValidCount;
        double fnr = actualInvalidCount == 0 ? 0 : (double) fn / actualInvalidCount;
        double passRate = (double) (tp + tn) / allCases.size();

        System.out.println();
        System.out.println("================ 결과 ================");
        System.out.println("전체 케이스: " + allCases.size() + " (정답 valid " + actualValidCount + " / invalid "
                + actualInvalidCount + ")");
        System.out.println("TP=" + tp + " TN=" + tn + " FP=" + fp + " FN=" + fn);
        System.out.printf("Pass rate(정답과 일치한 비율): %.1f%%%n", passRate * 100);
        System.out.printf(
                "FPR(정상인데 위반으로 오판, %d/%d): %.1f%%%n", fp, actualValidCount, fpr * 100);
        System.out.printf(
                "FNR(오염인데 통과시킴, %d/%d): %.1f%%%n", fn, actualInvalidCount, fnr * 100);
    }

    private String label(boolean valid) {
        return valid ? "valid" : "invalid";
    }
}
