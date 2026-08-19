package com.financematch.overallcomment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class OverallCommentLlmClientExceptionTest {

    @Test
    void 메시지만_받는_생성자는_원인이_없다() {
        OverallCommentLlmClient.LlmCallException exception =
                new OverallCommentLlmClient.LlmCallException("호출 실패");

        assertEquals("호출 실패", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void 메시지와_원인을_함께_받는_생성자는_원인을_그대로_보존한다() {
        RuntimeException cause = new RuntimeException("원인");

        OverallCommentLlmClient.LlmCallException exception =
                new OverallCommentLlmClient.LlmCallException("호출 실패", cause);

        assertEquals("호출 실패", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
