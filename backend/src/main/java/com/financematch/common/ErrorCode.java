package com.financematch.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 에러 코드.
 *
 * <p>컨트롤러·서비스에서 문자열 코드를 직접 만들지 않고 반드시 이 enum 을 사용한다. enum 이름이 그대로 응답의
 * {@code code} 필드가 되어 프론트엔드로 전달되므로, 이름을 바꾸면 프론트 에러 분기도 함께 바꿔야 한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 공통 =====
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // ===== 인증 토큰 (JWT — 공통 인프라) =====
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // ===================================================================
    //  ⚠️ 도메인별 에러 코드는 여기에 추가한다 — 【API 명세 확정 후】
    //     각 담당자가 자기 도메인 구획을 만들어 추가한다. 예:
    //       // ===== 인증 (임민지) =====
    //       EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    //       // ===== 커플 (강현지) =====
    //       INVITE_CODE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않은 초대코드입니다."),
    // ===================================================================

    // ===== 초대 (강현지) =====
    INVITATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 가능한 초대 코드가 존재합니다."),
    COUPLE_ALREADY_CONNECTED(HttpStatus.CONFLICT, "이미 파트너와 연결된 회원입니다."),

    // ===== 추천 =====
    COUPLE_NOT_CONNECTED(HttpStatus.NOT_FOUND, "연결된 커플의 정보가 없습니다."),
    RECOMMENDATION_NOT_READY(
            HttpStatus.CONFLICT, "추천 생성에 필요한 데이터가 준비되지 않았습니다."),
    RECOMMENDATION_IN_PROGRESS(
            HttpStatus.CONFLICT, "해당 커플의 추천 결과를 이미 생성하고 있습니다."),
    RECOMMENDATION_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR, "추천 결과 생성 중 오류가 발생했습니다."),
    RECOMMENDATION_NOT_FOUND(
            HttpStatus.NOT_FOUND, "현재 정보 기준으로 조회할 추천 결과가 없습니다.");

    private final HttpStatus status;
    private final String message;
}
