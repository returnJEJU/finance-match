package com.financematch.auth.domain;

/**
 * 회원 성별.
 *
 * <p>enum 이름이 그대로 {@code member.gender} 컬럼에 저장된다(F/M). 이름을 바꾸면 DB 값과 어긋나므로
 * 바꾸지 않는다.
 */
public enum Gender {
    F, // 여성
    M // 남성
}
