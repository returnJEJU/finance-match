package com.financematch.overallcomment;

import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.NamesInfo;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 종합 코멘트 전용 검증(공통 규칙은 {@link OverallCommentRuleValidator}가 담당). 세 가지를 본다:
 *
 * <ol>
 *   <li>출력에 등장하는 숫자가 전부 {@code allowedNumbers}에서 온 것인지(새 숫자 날조 방지)
 *   <li>strongAxes·weakAxes 이름이 하나도 빠짐없이 등장했는지(프롬프트의 최종 점검 체크리스트와
 *       동일한 기준을 코드로도 한 번 더 강제)
 *   <li>weakAxes가 있으면 강점/약점 문단이 실제로 분리됐는지(프롬프트로만 지시했을 때 모델이
 *       가끔 전부 한 문단으로 붙여 쓰는 걸 실제로 확인해서 추가한 검사)
 *   <li>names(me/partner)가 '님' 없이 조사만 붙어 등장하는지("하나는", "두리와" 등) — 모델이
 *       가끔 '님'을 빼먹는 걸 실제로 확인해서 추가한 검사
 * </ol>
 */
@Component
public class OverallCommentValidator {

    // "3억", "8004만원", "3억 8004만원", "68%" 같은 이 앱의 금액·퍼센트 표기 형태 전체를 하나의
    // 토큰으로 묶어서 잡는다. 예전엔 순수 숫자만([0-9][0-9,.]*) 토큰으로 뽑아서 "3억 8004만원"의
    // "3"과 "8004"가 각각 다른 allowedNumbers 항목("3억", "6억 8004만원")의 부분 문자열로 우연히
    // 통과해버리는 문제가 있었다(실제로 "6억 8004만원"을 "3억 8004만원"으로 잘못 쓴 출력이 이렇게
    // 통과됨). 단위까지 포함해 통짜로 비교하면 이런 조합 오류를 잡아낼 수 있다.
    private static final Pattern NUMBER_TOKEN =
            Pattern.compile("[0-9]+억(?:\\s?[0-9]+만원)?|[0-9]+만원|[0-9]+%");

    public ValidationResult validate(String commentFullText, OverallCommentPromptInput input) {
        List<String> violations = new ArrayList<>();

        for (String invented : inventedNumbers(commentFullText, input.getAllowedNumbers())) {
            violations.add("허용되지 않은 숫자 등장: " + invented);
        }

        for (AxisFact axis : allAxes(input)) {
            if (!commentFullText.contains(axis.getName())) {
                violations.add("축(" + axis.getName() + ") 언급 누락");
            }
        }

        // OverallCommentService가 paragraphs 배열을 "\n\n"으로 이어 붙이므로, 강점/약점이
        // 별도 문단(배열 원소)로 나뉘었다면 반드시 "\n\n"이 있어야 한다. weakAxes가 있는데
        // 없다면 모델이 강점·약점을 한 문단에 다 붙여 쓴 것이다.
        if (!input.getWeakAxes().isEmpty() && !commentFullText.contains("\n\n")) {
            violations.add("강점/약점 문단이 분리되지 않음(단일 문단으로 합쳐짐)");
        }

        for (String missing : missingHonorific(commentFullText, input.getNames())) {
            violations.add("이름에 '님'이 빠짐: " + missing);
        }

        return new ValidationResult(violations.isEmpty(), violations);
    }

    private List<AxisFact> allAxes(OverallCommentPromptInput input) {
        List<AxisFact> all = new ArrayList<>(input.getStrongAxes());
        all.addAll(input.getWeakAxes());
        return all;
    }

    // names는 이미 '님' 없는 축약형("하나", "두리")으로 들어온다 — 모델이 문장에서 직접 '님'을
    // 붙여야 하는데, 조사(는/은/이/가/의/와/과 등)를 '님' 없이 바로 붙이는 실수를 실제로 관찰했다
    // (예: "하나는 2000만원"). 이름 뒤에 '님'이 안 나오면 잡아낸다.
    private List<String> missingHonorific(String comment, NamesInfo names) {
        List<String> missing = new ArrayList<>();
        for (String name : List.of(names.getMe(), names.getPartner())) {
            Matcher matcher = Pattern.compile(Pattern.quote(name) + "(?!님)").matcher(comment);
            if (matcher.find()) {
                missing.add(name);
            }
        }
        return missing;
    }

    private List<String> inventedNumbers(String comment, List<String> allowedNumbers) {
        Set<String> allowedTokens = new HashSet<>();
        for (String allowed : allowedNumbers) {
            Matcher allowedMatcher = NUMBER_TOKEN.matcher(allowed);
            while (allowedMatcher.find()) {
                allowedTokens.add(allowedMatcher.group());
            }
        }

        List<String> invented = new ArrayList<>();
        Matcher matcher = NUMBER_TOKEN.matcher(comment);
        while (matcher.find()) {
            String token = matcher.group();
            if (!allowedTokens.contains(token)) {
                invented.add(token);
            }
        }
        return invented;
    }

    public record ValidationResult(boolean valid, List<String> violations) {}
}
