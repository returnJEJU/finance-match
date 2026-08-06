package com.financematch.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 리포트 준비 진행 상황. 5축 reason이 각자 독립적으로 채워지는(ReportWriteMapper 참고) 동안,
 * 프론트가 진행 바를 그릴 수 있도록 지금까지 몇 축이 끝났는지 알려준다.
 */
@Getter
@AllArgsConstructor
public class ReportStatusResponse {

    private final boolean ready;
    private final int completedAxes;
    private final int totalAxes;
}
