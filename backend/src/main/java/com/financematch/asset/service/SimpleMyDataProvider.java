package com.financematch.asset.service;

import com.financematch.asset.domain.MyDataSnapshot;
import com.financematch.auth.domain.Member;
import com.financematch.auth.mapper.MemberMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 목 서버 도입 전 사용하는 마이데이터 구현.
 *
 * <p>실제 마이데이터는 사람을 식별해 그 사람의 계좌를 돌려주므로, 목 구현도 회원마다 고정된 자산을
 * 돌려줘야 한다. 식별 키는 <b>이메일</b>이다 — 회원 번호와 달리 가입할 때 직접 정할 수 있어서, 데모
 * 계정을 시드로 미리 넣든 시연 중에 그 자리에서 가입하든 같은 자산이 붙는다.
 *
 * <p>매핑에 없는 이메일은 {@link MyDataScenario#DEFAULT}를 받는다. 등록되지 않은 계정이 자산 연동을
 * 시도해도 실패하지 않는다.
 *
 * <p>목 서버가 준비되면 이 클래스와 {@link MyDataScenario}를 함께 지우고 HTTP 구현으로 교체한다.
 * 매핑표는 그때 통째로 사라지며, {@link MyDataProvider}를 쓰는 쪽은 바뀌지 않는다.
 */
@Component
@RequiredArgsConstructor
public class SimpleMyDataProvider implements MyDataProvider {

    private static final Map<String, MyDataScenario> SCENARIO_BY_EMAIL =
            Map.of(
                    "demo.a@chaltteok.dev", MyDataScenario.DEMO_INVITER,
                    "demo.b@chaltteok.dev", MyDataScenario.DEMO_INVITEE,
                    "demo.saver@chaltteok.dev", MyDataScenario.SAVER,
                    "demo.investor@chaltteok.dev", MyDataScenario.INVESTOR,
                    "demo.newlywed@chaltteok.dev", MyDataScenario.NEWLYWED,
                    "demo.renter@chaltteok.dev", MyDataScenario.RENTER,
                    "demo.rich@chaltteok.dev", MyDataScenario.RICH,
                    "demo.debt@chaltteok.dev", MyDataScenario.HIGH_RATE_DEBT);

    private final MemberMapper memberMapper;

    @Override
    public MyDataSnapshot fetch(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 필요합니다.");
        }

        return scenarioOf(memberId)
                .build(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private MyDataScenario scenarioOf(Long memberId) {
        Member member = memberMapper.findById(memberId);
        if (member == null || member.getEmail() == null) {
            return MyDataScenario.DEFAULT;
        }

        return SCENARIO_BY_EMAIL.getOrDefault(member.getEmail(), MyDataScenario.DEFAULT);
    }
}
