package com.financematch.overallcomment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 강점 문단에 "아쉬워요" 같은 부정적 표현이 섞이거나, 약점 문단이 보완 필요성 없이 칭찬 일변도로
 * 쓰이는 경우를 검사한다. {@link OverallCommentRuleValidator}·{@link OverallCommentValidator}는
 * 표현·숫자·문단 분리 같은 형식만 정규식으로 보기 때문에, 문장의 실제 의미(긍정/부정 톤)까지는
 * 판단하지 못한다 — 이건 언어 이해가 필요한 영역이라 LLM에게 별도로 판정을 맡긴다.
 *
 * <p>축 이름은 {@code strongAxes}/{@code weakAxes}에서 가져오고, 문단 순서(강점 문단이 먼저,
 * 약점 문단이 나중)는 가정하지 않는다 — 프롬프트가 지시한 순서가 실제로 지켜졌는지까지 포함해서
 * LLM이 코멘트 전문을 보고 판단하게 한다.
 *
 * <p><b>판정 단위는 문장이 아니라 문단이다(2026-08-25 변경).</b> 원래는 "문장 단위로 판단하라"고
 * 지시했는데, 골든셋 실험(수동 측정, 18케이스)에서 FPR 88.9%가 나왔다 — 문단 전체 톤은 규칙을
 * 정확히 지켰는데도, "또래 커플 대비 83%예요" 같은 순수 사실 나열 문장 하나에 감정 표현이 없다는
 * 이유만으로 위반 처리되는 경우가 대부분이었다(그 순수 사실 문장 자체는 원래도 강점/약점 어느
 * 쪽으로도 무해한 중립 문장인데, "문장 단위" 지시 때문에 모델이 문장 하나하나에 톤을 요구했다).
 * 문단 순서를 바꾸기만 한 것(ORDER_SWAPPED)도 3/3 실패했다 — "문단 순서를 가정하지 않는다"는
 * 설계 의도가 프롬프트에 실제로 반영되지 않고 있었다는 뜻이다. 그래서 판정 기준을 "문단에 필요한
 * 톤이 한 군데라도 있는가"로 낮췄다 — 재현 방법·golden set은
 * {@code OverallCommentToneValidatorGoldenSetTest}에 있다.
 *
 * <p><b>Few-shot 예시는 시도했다가 되돌렸다(2026-08-25).</b> 문단 단위 판정으로도 골든셋 3회 반복
 * 측정에서 FPR이 평균 77.8%로 여전히 높아서(FNR은 3.7%까지 개선), 실패 사례(사람이 직접 "정상"
 * 이라고 확인한 것)를 valid/invalid 대조쌍 예시로 프롬프트에 추가해봤다. 결과는 역효과였다 — FPR이
 * 오히려 100%로 악화됐다. 원인: 예시에 쓴 축 이름("절세 활용", "부채 상환" 등)이 실제 입력에서도
 * 그대로 재사용되는 축 이름과 겹쳤다. 이 축들은 커플마다 강점/약점이 다르게 배정되는데(예:
 * "부채 상환"이 어떤 커플에겐 강점, 다른 커플에겐 약점), 예시가 특정 축 이름과 톤을 암묵적으로
 * 고정시켜 버리면서 모델이 "이번 입력의 [강점 항목]/[약점 항목] 목록" 대신 "예시에서 본 축 이름과
 * 톤의 조합"을 따라가 버렸다(예시와 다른 배정을 가진 커플에서 대거 오판). few-shot 예시를 다시
 * 시도한다면 실제 축 이름과 절대 겹치지 않는 가상의 이름을 써야 한다.
 *
 * <p><b>Self-consistency 다수결은 시도했다가 걷어냈다(2026-08-25, 3차 개선 → 철회).</b> 골든셋
 * 3회 반복 측정에서 FPR이 66.7~88.9% 사이로 실행마다 크게 흔들리는 걸 보고, 같은 판정을 3번(홀수 —
 * 동률 방지) 독립 반복해 다수결로 최종 valid를 정하도록 붙였다. 비용·지연은 3배가 됐다(그래서 3개
 * 호출을 병렬로 쏘는 것까지 했었다). 그런데 이후 아래 axisChecks를 추가하고 나서 "다수결 있음 vs
 * 없음"을 직접 대조 측정했더니(31케이스), FNR은 완전히 동일(26.7%)했고 FPR은 다수결이 **없을
 * 때** 오히려 더 낮았다(12.5% vs 6.3%). 즉 다수결은 비용만 3배 들 뿐 정확도에 실질적 기여가
 * 없었다 — 그래서 코드를 단순한 단일 호출로 되돌렸다. (다수결 자체가 잡아주는 건 "실행마다
 * 흔들리는 순수 노이즈"뿐인데, 실제 오류의 대부분은 유진·민호 BOUNDARY_NEUTRAL처럼 매번 똑같이
 * 틀리는 체계적 오류였다 — 그런 건 애초에 다수결로 안 풀린다.)
 *
 * <p><b>축별 근거 인용 강제(2026-08-25, 4차 개선 — 현재 유효).</b> 원래 응답 스키마는 {@code
 * {valid, violations}} 뿐이라 모델이 근거 없이 바로 결론부터 냈다. 약점 축이 여러 개인 문단에서
 * "이 중 하나라도 보완 필요 톤이 있는지" 존재-확인(existence check)을 근거 없이 하다 보니, 실제로는
 * 있는데도 없다고 오판하는 사례가 많았다(강점 축을 부정 톤으로 잘못 귀속시키는 사례도 관찰됨).
 * {@link OverallCommentLlmClient}의 스키마에 {@code axisChecks}(축마다 인용문+toneOk)를
 * {@code valid}보다 앞선 필드로 추가해서, strict json_schema가 필드를 선언 순서대로 채우는 특성을
 * 이용해 모델이 결론 전에 반드시 축별로 문단을 다시 훑어 인용하게 만들었다. 지금 정확도 개선의
 * 거의 전부가 이 변경 하나에서 나온다 — 골든셋(31케이스, {@code
 * OverallCommentToneValidatorGoldenSetTest})에서 실사용 DB에 있는 프로필 범위(강점 1~4개)는
 * FPR 0%, 강점 0개·5개 극단까지 넓히면 FPR 6.3%·FNR 26.7%.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverallCommentToneValidator {

    private static final String PROMPT_TEMPLATE =
            """
            아래는 부부 금융 리포트의 종합 코멘트입니다. 문단 단위로 톤이 규칙과 맞는지 판정하세요.

            [강점 항목 — 이 항목을 다루는 문단은 전체적으로 "잘하고 있다"는 긍정 톤을 담아야 함]
            %s

            [약점 항목 — 이 항목을 다루는 문단은 전체적으로 "보완이 필요하다"는 톤을 담아야 함(사실만
            나열하고 끝내면 안 됨)]
            %s

            [코멘트 전문]
            %s

            판정은 문장 단위가 아니라 "문단" 단위로 하세요. 각 항목이 등장하는 문단을 찾아서, 그
            문단 전체를 훑어봤을 때 요구된 톤(강점=긍정/약점=보완 필요)이 문단 어딘가에 한 번이라도
            드러나는지를 보세요. 문단이 어느 순서로 오는지(강점이 먼저든 나중이든)는 판정과 무관합니다
            — 오직 그 문단이 어떤 항목을 다루고 있고 그 항목이 강점인지 약점인지만 보세요.

            - 강점 문단인데 문단 전체가 "아쉽다/부족하다/보완이 필요하다"처럼 부정적으로만 서술됐다면
              위반입니다.
            - 약점 문단인데 문단 전체 어디에도 "보완이 필요하다"는 톤이 전혀 없이 "잘하고 있다/탄탄하다"
              같은 긍정 일변도로만 서술됐다면 위반입니다.
            - 문단 안에 "또래 커플 대비 83%%예요", "일치도는 46%%예요"처럼 감정 표현 없는 순수 사실·숫자
              나열 문장이 섞여 있는 것은 위반이 아닙니다. 그 문단의 다른 문장에서 필요한 톤이 이미
              드러났다면, 그런 중립적인 사실 문장이 하나 있다고 해서 위반으로 보지 마세요. 문단을
              구성하는 모든 문장 각각에 감정 표현이 있어야 하는 게 아니라, 문단 전체의 흐름이 그
              방향이면 됩니다. 이 규칙은 약점 축이 3~4개 이상이라 문단이 길어질 때 특히 중요합니다 —
              문단이 길어질수록 사실 나열 문장 비중이 자연히 높아지는데, 그렇다고 위반이 되는 건
              아닙니다.

            주의: 약점 항목이 "보완이 필요해요", "아쉬워요", "조금 더 신경 쓰면 좋겠어요"처럼
            부정적·개선 필요 톤으로 서술되는 것은 규칙을 정확히 지킨 정상 문단입니다 — 절대 위반이
            아닙니다. 약점 항목은 오히려 그런 톤이 있어야 통과입니다. violations에는 오직 위 두 가지
            경우(강점 문단인데 전체가 부정적, 약점 문단인데 전체가 긍정 일변도)만 담으세요.

            먼저 axisChecks에 강점/약점 항목을 전부(빠짐없이) 하나씩 담으세요. 각 항목마다 그
            항목을 다루는 문단에서 요구된 톤(강점=긍정/약점=보완 필요)이 드러나는 문장을 quote에
            그대로 인용하고(찾으면), toneOk에 그 톤이 실제로 있으면 true, 문단 전체를 봐도 전혀
            없으면 false를 넣으세요. 반드시 실제로 문단을 다시 훑어서 인용하세요 — 짐작으로 채우지
            마세요.

            axisChecks를 다 채운 뒤, toneOk가 false인 항목이 하나라도 있으면 valid를 false로 하고
            violations 배열에 그 항목 이름과 이유를 담으세요. toneOk가 전부 true면 valid를 true로,
            violations는 빈 배열로 답하세요.
            아래 형식의 JSON으로만 답하고, 다른 설명·마크다운은 출력하지 마세요.
            {"axisChecks": [{"axis": "...", "quote": "...", "toneOk": true 또는 false}, ...],
             "valid": true 또는 false, "violations": ["..."]}
            """;

    private final OverallCommentLlmClient llmClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ValidationResult validate(String commentFullText, OverallCommentPromptInput input) {
        String prompt = buildPrompt(commentFullText, input);

        String rawOutput;
        try {
            rawOutput = llmClient.checkTone(prompt);
        } catch (OverallCommentLlmClient.LlmCallException e) {
            log.warn("종합 코멘트 톤 검증 LLM 호출 실패", e);
            return new ValidationResult(false, List.of("톤 검증 호출 실패: " + e.getMessage()));
        }

        return parse(rawOutput);
    }

    private String buildPrompt(String commentFullText, OverallCommentPromptInput input) {
        String strongNames = names(input.getStrongAxes());
        String weakNames = input.getWeakAxes().isEmpty() ? "(없음)" : names(input.getWeakAxes());
        return PROMPT_TEMPLATE.formatted(strongNames, weakNames, commentFullText);
    }

    private String names(List<AxisFact> axes) {
        return axes.stream().map(AxisFact::getName).collect(Collectors.joining(", "));
    }

    // 모델이 스키마를 지켜도 방어적으로 파싱한다 — RuleValidator·Validator와 동일하게, 이 검증기가
    // 뱉는 실패도 결국 OverallCommentService의 재시도 루프가 "이번 시도는 버린다"는 신호로만
    // 쓰기 때문에 파싱 실패는 그냥 위반으로 취급하면 충분하다(별도 예외를 던지지 않는다).
    private ValidationResult parse(String rawOutput) {
        try {
            JsonNode root = mapper.readTree(rawOutput);
            JsonNode validNode = root.get("valid");
            JsonNode violationsNode = root.get("violations");
            if (validNode == null
                    || !validNode.isBoolean()
                    || violationsNode == null
                    || !violationsNode.isArray()) {
                return new ValidationResult(false, List.of("톤 검증 응답 파싱 실패: " + rawOutput));
            }

            List<String> violations = new ArrayList<>();
            for (JsonNode violation : violationsNode) {
                violations.add(violation.asText());
            }
            return new ValidationResult(validNode.asBoolean(), violations);
        } catch (Exception e) {
            return new ValidationResult(false, List.of("톤 검증 응답 파싱 실패: " + rawOutput));
        }
    }

    public record ValidationResult(boolean valid, List<String> violations) {}
}
