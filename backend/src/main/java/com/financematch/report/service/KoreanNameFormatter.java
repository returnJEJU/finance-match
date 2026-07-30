package com.financematch.report.service;

/**
 * 리포트 문구에 이름을 부를 때 쓰는 축약 규칙 (GAP-08 팀 확정: 성을 떼고 축약형으로 부른다).
 *
 * <p>예: "김하나" → "하나". 정렬(가나다순)이나 신원 식별에는 이 축약형이 아니라 원래 전체 이름을
 * 써야 한다 — 이건 어디까지나 "화면에 보여줄 때"만 쓰는 변환이다.
 *
 * <p>두 글자 이하 이름은 성을 떼면 어색하거나 아무것도 안 남아 그대로 둔다. 두 글자 성씨(남궁·황보 등)는
 * 지원하지 않는다 — 필요해지면 성씨 목록을 추가한다.
 */
public final class KoreanNameFormatter {

    private KoreanNameFormatter() {}

    public static String abbreviate(String fullName) {
        if (fullName == null || fullName.length() <= 1) {
            return fullName;
        }
        return fullName.substring(1);
    }
}
