package com.financematch.overallcomment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.overallcomment.dto.AxisFact;
import com.financematch.overallcomment.dto.FirstStepInfo;
import com.financematch.overallcomment.dto.GoalInfo;
import com.financematch.overallcomment.dto.NamesInfo;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverallCommentPromptBuilderTest {

    private final OverallCommentPromptBuilder builder = new OverallCommentPromptBuilder();
    private final ObjectMapper mapper = new ObjectMapper();

    // scratchpad java-openai 참고 프로젝트의 prompt.txt 예시 입력을 그대로 재현한다.
    private OverallCommentPromptInput sampleInput() {
        return new OverallCommentPromptInput(
                new NamesInfo("민준님", "서연님"),
                new GoalInfo("내 집 마련", "6억원", "5년", "2억 7,000만원", "3억 3,000만원", "45%"),
                List.of(new AxisFact("절세 활용", 0.75, List.of("민준님 IRP 연 700만원 한도를 채움"))),
                List.of(
                        new AxisFact(
                                "투자 가치관", 0.45, List.of("손실 감내력 차이가 큼", "투자 경험 차이가 있음")),
                        new AxisFact(
                                "금융 자산",
                                0.32,
                                List.of("두 분 합산 금융자산 8,000만원", "또래 중앙값보다 낮음")),
                        new AxisFact(
                                "목표 달성 가능성",
                                0.3,
                                List.of("5년 뒤 목표의 45%에 도달", "3억 3,000만원 부족")),
                        new AxisFact(
                                "부채",
                                0.28,
                                List.of("민준님 6,000만원", "서연님 3,000만원", "소득 대비 상환 부담이 다소 높음"))),
                new FirstStepInfo(
                        "청약저축 미가입 상태",
                        List.of("서연님 청약저축 미가입", "가입 시 월 10만원 기준 소득공제 가능")),
                List.of(
                        "6억원", "2억 7,000만원", "3억 3,000만원", "45%", "5년", "8,000만원", "6,000만원",
                        "3,000만원", "700만원"));
    }

    @Test
    void 입력_데이터_JSON이_참고_프로젝트와_동일한_필드_구조로_직렬화된다() throws Exception {
        String prompt = builder.build(sampleInput());

        String json = extractInputDataJson(prompt);
        System.out.println("=== [입력 데이터] 섹션 실제 출력 ===");
        System.out.println(json);

        JsonNode root = mapper.readTree(json);

        assertEquals("민준님", root.at("/names/me").asText());
        assertEquals("서연님", root.at("/names/partner").asText());

        assertEquals("내 집 마련", root.at("/goal/purpose").asText());
        assertEquals("6억원", root.at("/goal/targetAmount").asText());
        assertEquals("5년", root.at("/goal/monthsLabel").asText());
        assertEquals("2억 7,000만원", root.at("/goal/expectedAsset").asText());
        assertEquals("3억 3,000만원", root.at("/goal/shortfall").asText());
        assertEquals("45%", root.at("/goal/achievementRate").asText());

        assertTrue(root.get("strongAxes").isArray());
        assertEquals("절세 활용", root.at("/strongAxes/0/name").asText());
        assertEquals(0.75, root.at("/strongAxes/0/ratio").asDouble());
        assertEquals("민준님 IRP 연 700만원 한도를 채움", root.at("/strongAxes/0/facts/0").asText());

        assertEquals(4, root.get("weakAxes").size());
        assertEquals("투자 가치관", root.at("/weakAxes/0/name").asText());
        assertEquals("부채", root.at("/weakAxes/3/name").asText());

        assertEquals("청약저축 미가입 상태", root.at("/firstStep/reason").asText());
        assertEquals("서연님 청약저축 미가입", root.at("/firstStep/facts/0").asText());

        assertTrue(root.get("allowedNumbers").isArray());
        assertEquals("6억원", root.at("/allowedNumbers/0").asText());

        // 필드 순서까지 참고 프로젝트 예시와 동일한지(가독성/리뷰 편의용) 최상위 키 순서를 확인한다.
        List<String> topLevelKeys = List.of(
                "names", "goal", "strongAxes", "weakAxes", "firstStep", "allowedNumbers");
        List<String> actualKeys = new java.util.ArrayList<>();
        root.fieldNames().forEachRemaining(actualKeys::add);
        assertEquals(topLevelKeys, actualKeys);
    }

    @Test
    void weakAxes가_비어있으면_강약_문단_분리_체크리스트_문구가_빠진다() {
        OverallCommentPromptInput input =
                new OverallCommentPromptInput(
                        new NamesInfo("민준님", "서연님"),
                        new GoalInfo("내 집 마련", "6억원", "5년", "2억 7,000만원", null, "45%"),
                        List.of(new AxisFact("절세 활용", 0.75, List.of("한도를 채움"))),
                        List.of(),
                        new FirstStepInfo("절세 활용", List.of("한도를 채움")),
                        List.of("6억원"));

        String prompt = builder.build(input);

        assertFalse(prompt.contains("weakAxes가 있으므로"));
    }

    @Test
    void 입력값_직렬화에_실패하면_IllegalStateException으로_감싼다() {
        OverallCommentPromptInput input = mock(OverallCommentPromptInput.class);
        when(input.getNames()).thenThrow(new RuntimeException("직렬화 대상에서 발생한 오류"));

        assertThrows(IllegalStateException.class, () -> builder.build(input));
    }

    // 프롬프트 문자열의 "[입력 데이터]" 섹션에서 JSON 블록만 중괄호 짝을 맞춰 추출한다.
    private String extractInputDataJson(String prompt) {
        int start = prompt.indexOf("[입력 데이터]");
        int braceStart = prompt.indexOf('{', start);
        int depth = 0;
        for (int i = braceStart; i < prompt.length(); i++) {
            char c = prompt.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return prompt.substring(braceStart, i + 1);
                }
            }
        }
        throw new IllegalStateException("입력 데이터 JSON을 찾지 못했습니다.");
    }
}
