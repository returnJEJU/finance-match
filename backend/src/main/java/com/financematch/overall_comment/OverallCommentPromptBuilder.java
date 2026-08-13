package com.financematch.overall_comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.overall_comment.dto.AxisFact;
import com.financematch.overall_comment.dto.OverallCommentPromptInput;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 종합 코멘트 프롬프트 조립. scratchpad의 java-openai 참고 프로젝트(prompt.txt)에서 검증된 규칙
 * 문구를 그대로 가져오고, [입력 데이터]만 실제 {@link OverallCommentPromptInput}을 직렬화해 채운다.
 *
 * <p>참고 프로젝트와의 차이: 그 프로젝트는 프롬프트에 예시 입력 JSON을 하드코딩해뒀지만, 여기서는
 * {@code OverallCommentInputBuilder}가 만든 실제 값을 매번 새로 직렬화한다. 최종 점검
 * 체크리스트의 축 이름·개수도 하드코딩하지 않고 입력값(strongAxes+weakAxes)에서 그때그때
 * 뽑는다 — 절세 축이 빠지면(taxStrategyCalculated=false) 4개로 자동으로 줄어든다.
 */
@Slf4j
@Component
public class OverallCommentPromptBuilder {

    private static final String EXPRESSION_RULES =
            """
            [표현 규칙]
            - 반드시 모든 문장은 해요체를 사용한다.
              (예: "~해요", "~이에요/예요", "~있어요", "~돼요", "~같아요" 로 끝날 것.
               "~다", "~함", "~됨" 같은 다체·명사형 종결 금지.)
            - '당신/귀하/배우자님/남편분/아내분/상대방/파트너님' 사용 금지.
              names +'님' 또는 '두 분'만 사용한다.
              (예: '철수님', '영희님' 사용.
               '철수', '영희' 사용 금지.)
            - names는 문장 어디에 나오든(조사 '는/은/이/가/의/와/과' 등이 붙을 때도) 반드시
              '님'을 붙인 뒤에 조사를 붙인다. '철수는', '영희의', '철수와' 처럼 '님' 없이 조사만
              바로 붙이는 건 전부 금지다 — '철수님은', '영희님의', '철수님과'로 쓸 것.
            - 입력의 names는 이미 성을 뗀 축약형이다. 그대로 쓰고, 다시 줄이거나 성을 붙이지 말 것.
            - allowedNumbers에 없는 숫자를 쓰지 말 것. 계산해서 새 숫자를 만들지도 말 것.
              금액은 적힌 표기를 글자 그대로 옮길 것.
            - ratio는 항목의 강약을 판단하려고 넣은 값이다. 문장에 숫자로 쓰지 말 것.
            - 특정 금융상품 이름이나 금리를 말하지 말 것.
              상품 이야기는 "추천탭에서 확인해보세요"로만 한다.
            - '반드시', '수익이 납니다', '보장' 같은 단정적 표현 금지.
            - '그럼에도 불구하고', '하지만 동시에' 같은 어색한 역접 연결어 금지.
              firstStep은 앞 내용과 대조 관계가 아니므로,
              '이때', '지금', '이런 상황에서' 처럼 순접으로 자연스럽게 이어 쓴다.
            - firstStep을 설명할 때 facts에 없는 의미를 지어내 붙이지 말 것. 남은 한도는
              facts 그대로만 언급하고, "남은 한도를 채워보세요"처럼 채우기를 권유한 뒤
              구체적인 방법은 "추천탭에서 확인해보세요"로 안내한다.
            """;

    private static final String WRITING_RULES =
            """
            [작성 규칙]
            - 두 분의 현재 금융 상황을 종합해 설명한다.
            - strongAxes와 weakAxes에 있는 항목이 하나도 빠짐없이 등장해야 한다.
            - ratio가 높은 항목, 즉 JSON 데이터 상 앞에 있는 항목 먼저 언급한다.
            - strongAxes 항목은 facts를 그냥 나열하지 말고, "잘하고 있다"는 평가를 문장에 먼저
              분명히 담는다(예: "~을 잘 챙기고 있어요", "~이 탄탄해요", "~이 잘 맞아요", "~은
              안정적이에요"). facts는 그 평가를 뒷받침하는 근거로만 덧붙인다.
            - weakAxes 항목은 facts를 그냥 나열하지 말고, "보완이 필요하다"는 평가를 문장에 먼저
              분명히 담는다(예: "~은 아직 아쉬워요", "~을 보완하면 좋겠어요", "~에 조금 더 신경
              쓰면 좋아요"). facts는 그 평가를 뒷받침하는 근거로만 덧붙인다.
            - "~원이에요", "~남았어요"처럼 숫자·사실만 말하고 끝내는 문장을 만들지 말 것 — 모든
              항목 문장은 반드시 강점/약점 평가로 시작하거나 끝난다.
            - strongAxes 항목에는 '아쉽다', '아쉬운', '부족하다', '보완이 필요하다', '보완하면
              좋겠다'처럼 약점을 뜻하는 표현을 절대 쓰지 않는다. strongAxes는 오직 잘하고 있다는
              톤으로만 서술한다.
            - weakAxes 배열이 비어 있으면(이번 데이터에 약점 항목이 하나도 없으면), 아쉬운 점이나
              보완할 점을 스스로 지어내 언급하지 않는다. 이 경우 코멘트 전체를 강점 칭찬으로만
              마무리한다.
            - strongAxes를 하나도 빠짐 없이 순서대로 모두 언급한 후 weakAxes도 순서대로 모두 언급한다.
            - strongAxes 내용과 weakAxes 내용은 반드시 서로 다른 문단(paragraphs 배열의 서로 다른
              원소)에 담는다. 한 문단 안에 강점 항목과 약점 항목을 섞어 쓰지 말 것. weakAxes가
              비어 있지 않다면 paragraphs 배열은 최소 2개 이상이어야 한다(strongAxes 문단 1개
              이상 + weakAxes 문단 1개 이상). weakAxes가 비어 있으면 strongAxes 문단만으로 구성해도
              된다.
            - 점수가 높은 항목이라도 firstStep에 여력이 남아 있으면, 칭찬과 여력을 구분해 말한다.
            - 항목 이름(name)을 그대로 쓰지 말고 사람 말로 풀어 쓴다.
              ("금융 자산 항목은 21점" 같은 표현 금지)
            - facts에 적힌 사실만 근거로 쓴다. 없는 사실을 추측해 넣지 말 것.
            - 문단마다 핵심 구절 하나를 **로 감싼다.
            - 전체 300~550자. strongAxes·weakAxes 문단 수를 제외한 다른 문단은 만들지 않는다.
            """;

    private static final String OUTPUT_INSTRUCTION_TEMPLATE =
            """
            위 규칙과 입력 데이터만 근거로 종합 코멘트를 작성하세요.

            [최종 점검 체크리스트]
            %d개 항목(Axes의 name: %s)이 문장 어딘가에 전부(하나도 빠짐없이) 등장했는지 작성 후 반드시 확인하세요.
            하나라도 빠졌다면 그 항목을 자연스럽게 추가해서 다시 작성하세요.
            %s
            아래 형식의 JSON으로만 출력하고, 다른 설명·마크다운·따옴표는 출력하지 마세요.
            {"paragraphs": ["첫 문단", "둘째 문단"]}
            """;

    private static final String WEAK_AXES_PARAGRAPH_CHECK =
            "weakAxes가 있으므로 paragraphs 배열이 2개 이상이고, strongAxes 문단과 weakAxes 문단이 섞이지 않았는지도 확인하세요.\n";

    private final ObjectMapper mapper = new ObjectMapper();

    public String build(OverallCommentPromptInput input) {
        String inputJson;
        try {
            inputJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);
        } catch (Exception e) {
            throw new IllegalStateException("입력값 직렬화 실패: 종합 코멘트", e);
        }
        log.debug("[종합코멘트] 입력 데이터 JSON:\n{}", inputJson);

        int axisCount = input.getStrongAxes().size() + input.getWeakAxes().size();
        String axisNames = allAxisNames(input);
        String weakAxesCheck = input.getWeakAxes().isEmpty() ? "" : WEAK_AXES_PARAGRAPH_CHECK;
        String outputInstruction =
                OUTPUT_INSTRUCTION_TEMPLATE.formatted(axisCount, axisNames, weakAxesCheck);

        return """
                당신은 부부 금융 리포트의 "종합 코멘트" 생성기입니다.

                %s
                %s
                [입력 데이터]
                %s

                %s
                """
                .formatted(EXPRESSION_RULES, WRITING_RULES, inputJson, outputInstruction);
    }

    private String allAxisNames(OverallCommentPromptInput input) {
        return Stream.concat(input.getStrongAxes().stream(), input.getWeakAxes().stream())
                .map(AxisFact::getName)
                .collect(Collectors.joining("/"));
    }
}
