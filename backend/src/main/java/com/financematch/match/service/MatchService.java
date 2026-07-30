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
import com.financematch.report.service.GoalFeasibilityScoreService;

import lombok.RequiredArgsConstructor;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchMapper matchMapper;
    private final MatchCalculationInputConverter converter;
    private final MatchCalculator calculator;
    private final GoalFeasibilityScoreService goalFeasibilityScoreService;

    @Transactional
    public CompatibilityResult getOrCalculateCompatibilityResult(
            Long memberId
    ) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID가 필요합니다.");
        }

        // 1. 로그인 회원이 속한 커플과 공동 목표 조회
        MatchCoupleData couple =
                matchMapper.findCoupleDataByMemberId(memberId);

        if (couple == null) {
            throw new IllegalArgumentException(
                    "연결된 커플 정보를 찾을 수 없습니다."
            );
        }

        // 2. 이미 계산한 결과가 있으면 재계산하지 않고 반환
        CompatibilityResult existingResult =
                matchMapper.findCompatibilityResultByCoupleId(
                        couple.getCoupleId()
                );

        if (existingResult != null) {
            return existingResult;
        }

        // 3. 커플을 구성하는 두 회원의 계산 원천 데이터 조회
        MatchMemberData memberA =
                matchMapper.findMemberDataByMemberId(
                        couple.getInviterId()
                );

        MatchMemberData memberB =
                matchMapper.findMemberDataByMemberId(
                        couple.getInviteeId()
                );

        if (memberA == null || memberB == null) {
            throw new IllegalArgumentException(
                    "금융 궁합도 계산에 필요한 회원 정보를 찾을 수 없습니다."
            );
        }

        // 4. DB 조회 데이터를 계산기 입력 형식으로 변환
        MatchCalculationInput calculationInput =
                converter.convert(couple, memberA, memberB);

        // 5. 금융 궁합도 계산
        MatchCalculationResult calculationResult =
                calculator.calculate(calculationInput);

        // 6. 계산 결과 최초 저장
        int insertedCount =
                matchMapper.insertCompatibilityResult(
                        couple.getCoupleId(),
                        calculationResult
                );

        if (insertedCount != 1) {
            throw new IllegalStateException(
                    "금융 궁합도 결과 저장에 실패했습니다."
            );
        }

        // 7. 저장된 결과를 다시 조회하여 반환
        CompatibilityResult savedResult =
                matchMapper.findCompatibilityResultByCoupleId(
                        couple.getCoupleId()
                );

        if (savedResult == null) {
            throw new IllegalStateException(
                    "저장된 금융 궁합도 결과를 찾을 수 없습니다."
            );
        }

        // 8. 목표 달성 가능성 축 reason 생성·저장 (LLM 미사용 · 결정론적)
        goalFeasibilityScoreService.generateAndSave(
                savedResult.getId(),
                calculationResult.getExpectedAsset(),
                calculationInput.getTargetAmount(),
                calculationInput.getTargetPeriodMonths()
        );

        return savedResult;
    }

    @Transactional(readOnly = true)
    public CompatibilityResult getCompatibilityResult(
            Long memberId
    ) {
        if (memberId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        MatchCoupleData couple =
                matchMapper.findCoupleDataByMemberId(memberId);

        if (couple == null) {
            throw new ApiException(
                    ErrorCode.NOT_FOUND,
                    "연결된 커플 정보를 찾을 수 없습니다."
            );
        }

        CompatibilityResult result =
                matchMapper.findCompatibilityResultByCoupleId(
                        couple.getCoupleId()
                );

        if (result == null) {
            throw new ApiException(
                    ErrorCode.NOT_FOUND,
                    "금융 궁합도 계산 결과를 찾을 수 없습니다."
            );
        }

        return result;
    }
}