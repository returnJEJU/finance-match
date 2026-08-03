package com.financematch.personalsurvey.service;

import com.financematch.match.service.MatchService;
import com.financematch.personalsurvey.domain.ReadyCoupleInvestmentTypes;
import com.financematch.personalsurvey.event.PersonalSurveyCompletedEvent;
import com.financematch.personalsurvey.mapper.PersonalSurveyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 커플 양쪽 회원이 모두 개인 설문을 완료한 시점에 궁합도(리포트) 계산을 자동으로 트리거한다.
 *
 * <p>{@link PersonalSurveyCompletedEvent} 는 설문 저장 트랜잭션 커밋 직전에 발행되지만, 이 리스너는
 * {@link TransactionPhase#AFTER_COMMIT} 이후에만 실행된다 — 그래야 방금 저장한 값이 실제로 DB에서
 * 조회 가능한 상태가 된다.
 *
 * <p>{@code getOrCalculateCompatibilityResult}는 LLM을 여러 번 호출해 수십 초~수 분이 걸릴 수 있어
 * ({@code MatchService} 참고), 설문 저장 API의 응답을 막지 않도록 {@link Async}로 별도 스레드
 * ({@code RootConfig#taskExecutor}) 에서 실행한다 — 완전한 실행 결과를 기다리지 않는 fire-and-forget
 * 방식이라 실패해도 설문 저장 자체는 이미 성공한 상태로 남는다(로그로만 남기고 예외를 삼킨다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoupleReportTriggerListener {

    private final PersonalSurveyMapper personalSurveyMapper;
    private final MatchService matchService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPersonalSurveyCompleted(PersonalSurveyCompletedEvent event) {
        Long memberId = event.memberId();

        // investment_type 은 개인 설문 저장이 성공하면 항상 채워지므로(PersonalSurveyService.save),
        // 두 회원 다 non-null 인지가 곧 "둘 다 설문 완료"와 같다. 커플 미연동이거나 파트너가 아직이면 null.
        ReadyCoupleInvestmentTypes ready = personalSurveyMapper.findReadyCoupleInvestmentTypes(memberId);
        if (ready == null) {
            return;
        }

        try {
            matchService.getOrCalculateCompatibilityResult(memberId);
        } catch (Exception e) {
            // 설문 저장은 이미 커밋된 뒤라 실패해도 롤백할 게 없다 — 다음 로그인/재시도 때
            // MatchService 의 캐시 확인 로직이 다시 계산을 시도하므로 여기서는 로그만 남긴다.
            log.error("커플 궁합도(리포트) 자동 계산 실패 (memberId={})", memberId, e);
        }
    }
}
