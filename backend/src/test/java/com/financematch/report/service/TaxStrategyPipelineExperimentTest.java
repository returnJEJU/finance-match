package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import com.financematch.report.llm.ReasonRuleValidator;
import com.financematch.report.llm.ReportLlmClient;
import com.financematch.report.llm.ReportPromptBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * 절세 축 reason 을 실제 LLM({@code openai.model}, 기본 gpt-5-nano)으로 직접 호출해보는 실험용
 * 테스트다. {@code MatchService}·DB 없이 {@link TaxStrategyReasonInput} 값만 손으로 바꿔가며 실제
 * 프롬프트가 만드는 문장을 콘솔(stdout)에서 확인한다.
 *
 * <p>실제 네트워크 호출(비용·지연 발생)이라 {@code ./gradlew test} 를 돌릴 때 매번 실행되지 않도록
 * 시스템 프로퍼티로 막아뒀다. 실행하려면:
 *
 * <pre>{@code
 * ./gradlew test --tests "*TaxStrategyPipelineExperimentTest*" -DrunLlmExperiment=true
 * }</pre>
 *
 * <p>{@code application-secret.properties}(gitignore 대상)에 실제 {@code openai.api.key} 가 있어야
 * 의미 있는 응답이 나온다 — 없으면 매 시도 검증 실패로 끝나고 결정론적 폴백 문구만 보게 된다.
 */
@EnabledIfSystemProperty(named = "runLlmExperiment", matches = "true")
class TaxStrategyPipelineExperimentTest {

    private static final TaxAccountInput OPEN_FULL =
            new TaxAccountInput(true, new BigDecimal(20_000_000), new BigDecimal(20_000_000));

    private final ReportPromptBuilder promptBuilder = new ReportPromptBuilder();
    private final ReasonRuleValidator ruleValidator = new ReasonRuleValidator();
    private final ReportLlmClient llmClient = new ReportLlmClient(loadProperty("openai.api.key"), loadProperty("openai.model"));
    private final TaxStrategyReasonService service =
            new TaxStrategyReasonService(llmClient, promptBuilder, ruleValidator);

    @Test
    void 한명만_계좌_한개_미개설() {
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(TaxAccountInput.unopened(), OPEN_FULL, OPEN_FULL);
        run(new TaxStrategyReasonInput("김철수", "이영희", me, partner));
    }

    @Test
    void 세_계좌_다_미개설() {
        TaxSavingProfile me =
                new TaxSavingProfile(
                        TaxAccountInput.unopened(), TaxAccountInput.unopened(), TaxAccountInput.unopened());
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        run(new TaxStrategyReasonInput("김철수", "이영희", me, partner));
    }

    @Test
    void 한도_미달() {
        // 1200만원 납입 / 2000만원 한도
        TaxAccountInput underLimitIsa =
                new TaxAccountInput(true, new BigDecimal(12_000_000), new BigDecimal(20_000_000));
        TaxSavingProfile me = new TaxSavingProfile(underLimitIsa, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        run(new TaxStrategyReasonInput("김철수", "이영희", me, partner));
    }

    @Test
    void 둘_다_문제있으면_가나다순으로_둘_다_언급() {
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, TaxAccountInput.unopened(), OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, TaxAccountInput.unopened());
        run(new TaxStrategyReasonInput("김철수", "이영희", me, partner));
    }

    @Test
    void 둘_다_전부_충족이면_칭찬_문구() {
        TaxSavingProfile me = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        run(new TaxStrategyReasonInput("김철수", "이영희", me, partner));
    }

    /**
     * 여기 값만 자유롭게 바꿔서 원하는 케이스를 즉석에서 만들어 실행하면 된다. 다른 축·DB·MatchService
     * 는 전혀 관여하지 않는다.
     */
    @Test
    void 커스텀_케이스() {
        TaxAccountInput isaUnderLimit =
                new TaxAccountInput(true, new BigDecimal(5_000_000), new BigDecimal(20_000_000));
        TaxSavingProfile me = new TaxSavingProfile(isaUnderLimit, TaxAccountInput.unopened(), OPEN_FULL);
        TaxSavingProfile partner = new TaxSavingProfile(OPEN_FULL, OPEN_FULL, OPEN_FULL);
        run(new TaxStrategyReasonInput("김하나", "이두리", me, partner));
    }

    private void run(TaxStrategyReasonInput input) {
        System.out.println("=== 절세 축 LLM 실험 ===");
        System.out.println("[입력값]");
        System.out.println("meName      = " + input.getMeName());
        System.out.println("partnerName = " + input.getPartnerName());
        printProfile("me", input.getMe());
        printProfile("partner", input.getPartner());

        System.out.println();
        System.out.println("[LLM에 실제로 들어가는 형태 — 이름은 축약형, 금액은 '만원' 단위 문자열]");
        System.out.println(
                "meName      = " + KoreanNameFormatter.abbreviate(input.getMeName()));
        System.out.println(
                "partnerName = " + KoreanNameFormatter.abbreviate(input.getPartnerName()));
        printPromptProfile("me", input.getMe());
        printPromptProfile("partner", input.getPartner());

        String reason = service.generate(input);

        System.out.println();
        System.out.println("[결과]");
        System.out.println("reason = " + reason);
        System.out.println();

        assertNotNull(reason);
        assertFalse(reason.isBlank());
    }

    private void printProfile(String label, TaxSavingProfile profile) {
        System.out.println(label + ".isa     = " + describe(profile.getIsa()));
        System.out.println(label + ".irp     = " + describe(profile.getIrp()));
        System.out.println(label + ".pension = " + describe(profile.getPension()));
    }

    private String describe(TaxAccountInput account) {
        if (!account.isOpened()) {
            return "미개설";
        }
        return "개설(납입 " + account.getContributed() + "원 / 한도 " + account.getAnnualLimit() + "원)";
    }

    private void printPromptProfile(String label, TaxSavingProfile profile) {
        System.out.println(label + ".isa     = " + describeForPrompt(profile.getIsa()));
        System.out.println(label + ".irp     = " + describeForPrompt(profile.getIrp()));
        System.out.println(label + ".pension = " + describeForPrompt(profile.getPension()));
    }

    private String describeForPrompt(TaxAccountInput account) {
        if (!account.isOpened()) {
            return "미개설";
        }
        return "개설(납입 "
                + WonAmountFormatter.format(account.getContributed())
                + " / 한도 "
                + WonAmountFormatter.format(account.getAnnualLimit())
                + ")";
    }

    private static String loadProperty(String key) {
        Properties props = new Properties();
        loadInto(props, "application-local.properties");
        loadInto(props, "application-secret.properties"); // 있으면 local 값을 덮어씀
        return props.getProperty(key, "");
    }

    private static void loadInto(Properties props, String resourceName) {
        try (InputStream in =
                TaxStrategyPipelineExperimentTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
