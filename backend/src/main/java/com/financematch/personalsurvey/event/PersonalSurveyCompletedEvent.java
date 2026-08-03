package com.financematch.personalsurvey.event;

/**
 * 한 회원이 개인 설문을 완료했을 때 발행된다. 파트너까지 완료됐는지(=커플 궁합도 계산이 가능한지)는
 * 이 이벤트를 발행하는 시점이 아니라, 트랜잭션 커밋 후 리스너가 직접 다시 조회해 판단한다 — 발행
 * 시점엔 아직 커밋 전이라 다른 트랜잭션(파트너 쪽 요청)에서 그 결과를 볼 수 없기 때문이다.
 */
public record PersonalSurveyCompletedEvent(Long memberId) {}
