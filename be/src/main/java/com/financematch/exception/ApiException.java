package com.financematch.exception;

import com.financematch.common.ErrorCode;
import lombok.Getter;

/**
 * 서비스 계층에서 던지는 표준 예외.
 *
 * <p>컨트롤러에서 try-catch 로 직접 응답을 만들지 말고 이 예외를 던진다. {@link GlobalExceptionHandler}
 * 가 받아서 공통 응답({@link com.financematch.common.ApiResponse})으로 변환한다.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
