package com.financematch.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 모든 API 의 공통 응답 래퍼.
 *
 * <p>성공/실패와 무관하게 항상 같은 구조로 응답한다. 프론트엔드는 이 구조를 한 번만 처리하면
 * 모든 API 응답을 일관되게 다룰 수 있다.
 *
 * <pre>
 * 성공: { "success": true,  "data": { ... }, "message": null, "code": null }
 * 실패: { "success": false, "data": null,    "message": "이미 가입된 이메일", "code": "EMAIL_EXISTS" }
 * </pre>
 *
 * <p>컨트롤러에서는 {@code ApiResponse.ok(data)} 로 감싸 반환하고, 실패는 예외({@link
 * com.financematch.exception.ApiException})를 던지면 {@link
 * com.financematch.exception.GlobalExceptionHandler} 가 {@code ApiResponse.fail(...)} 로 변환한다.
 */
@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String code;

    private ApiResponse(boolean success, T data, String message, String code) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.code = code;
    }

    /** 데이터를 담은 성공 응답 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /** 데이터 없는 성공 응답 (삭제·로그아웃 등) */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    /** 에러 코드의 기본 메시지로 실패 응답 */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, errorCode.getMessage(), errorCode.name());
    }

    /** 커스텀 메시지로 실패 응답 (검증 오류 등 상세 메시지가 필요할 때) */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode.name());
    }
}
