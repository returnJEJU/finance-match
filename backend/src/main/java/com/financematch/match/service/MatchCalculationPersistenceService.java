package com.financematch.match.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MatchCalculator;
import com.financematch.match.converter.MatchCalculationInputConverter;
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;
import com.financematch.match.mapper.MatchMapper;

import lombok.RequiredArgsConstructor;

/**
 * 궁합도 점수 계산·최초 저장만 담당하는 짧은 트랜잭션. {@link MatchService}에서 분리한 이유는, 각 축의
 * reason 생성(특히 LLM 호출)은 초 단위로 걸릴 수 있어 같은 트랜잭션 안에 두면 DB 커넥션을 그만큼 오래
 * 붙잡게 되기 때문이다 — 계산·저장은 여기서 빠르게 끝내고, reason 생성은 트랜잭션 밖에서 진행한다.
 */
@Service
@RequiredArgsConstructor
public class MatchCalculationPersistenceService {

    private final MatchMapper matchMapper;
    private final MatchCalculationInputConverter converter;
    private final MatchCalculator calculator;

    @Transactional
    public MatchCalculationPersistenceResult calculateAndPersist(
            MatchCoupleData couple, MatchMemberData memberA, MatchMemberData memberB) {

        MatchCalculationInput calculationInput = converter.convert(couple, memberA, memberB);
        MatchCalculationResult calculationResult = calculator.calculate(calculationInput);

        int insertedCount = matchMapper.insertCompatibilityResult(couple.getCoupleId(), calculationResult);

        if (insertedCount != 1) {
            throw new IllegalStateException("금융 궁합도 결과 저장에 실패했습니다.");
        }

        CompatibilityResult savedResult = matchMapper.findCompatibilityResultByCoupleId(couple.getCoupleId());

        if (savedResult == null) {
            throw new IllegalStateException("저장된 금융 궁합도 결과를 찾을 수 없습니다.");
        }

        return new MatchCalculationPersistenceResult(savedResult, calculationInput, calculationResult);
    }
}
