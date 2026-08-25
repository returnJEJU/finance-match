package com.financematch.match.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financematch.match.calculator.MatchCalculationInput;
import com.financematch.match.calculator.MatchCalculationResult;
import com.financematch.match.calculator.MatchCalculator;
import com.financematch.match.calculator.MemberCalculationInput;
import com.financematch.match.domain.CompatibilityResult;
import com.financematch.match.domain.MatchCoupleData;
import com.financematch.match.domain.MatchMemberData;
import com.financematch.match.mapper.MatchMapper;

import com.financematch.overallcomment.OverallCommentInputBuilder;
import com.financematch.overallcomment.OverallCommentService;
import com.financematch.overallcomment.dto.OverallCommentPromptInput;
import com.financematch.report.dto.reason.DebtRepaymentReasonInput;
import com.financematch.report.dto.reason.FinancialValueReasonInput;
import com.financematch.report.dto.reason.TaxAccountInput;
import com.financematch.report.dto.reason.TaxSavingProfile;
import com.financematch.report.dto.reason.TaxStrategyReasonInput;
import com.financematch.report.service.AssetStabilityScoreService;
import com.financematch.report.service.DebtRepaymentScoreService;
import com.financematch.report.service.FinancialValueScoreService;
import com.financematch.report.service.GoalFeasibilityScoreService;
import com.financematch.report.service.TaxStrategyScoreService;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchMapper matchMapper;
    private final MatchCalculationPersistenceService matchCalculationPersistenceService;
    private final GoalFeasibilityScoreService goalFeasibilityScoreService;
    private final AssetStabilityScoreService assetStabilityScoreService;
    private final DebtRepaymentScoreService debtRepaymentScoreService;
    private final FinancialValueScoreService financialValueScoreService;
    private final TaxStrategyScoreService taxStrategyScoreService;
    private final OverallCommentInputBuilder overallCommentInputBuilder;
    private final OverallCommentService overallCommentService;


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

        // 4~7. 계산 + compatibility_result 최초 저장 — 여기까지만 짧은 트랜잭션(MatchCalculationPersistenceService).
        // 아래 8~12(reason 생성)는 모두 결정론적 로직이지만, 저장까지 포함하므로 트랜잭션 밖에서 진행한다.
        MatchCalculationPersistenceResult persisted =
                matchCalculationPersistenceService.calculateAndPersist(couple, memberA, memberB);

        CompatibilityResult savedResult = persisted.savedResult();
        MatchCalculationInput calculationInput = persisted.calculationInput();
        MatchCalculationResult calculationResult = persisted.calculationResult();

        // 8. 목표 달성 가능성 축 reason 생성·저장 (결정론적)
        goalFeasibilityScoreService.generateAndSave(
                savedResult.getId(),
                calculationResult.getExpectedAsset(),
                calculationInput.getTargetAmount(),
                calculationInput.getTargetPeriodMonths()
        );

        // 9. 금융 자산 축 reason 생성·저장 (결정론적)
        assetStabilityScoreService.generateAndSave(
                savedResult.getId(),
                calculationResult.getCoupleAssetRatio()
        );

        // 10~12. 부채·투자가치관·절세 축 reason 생성·저장 (결정론적)
        DebtRepaymentReasonInput debtRepaymentReasonInput = new DebtRepaymentReasonInput(
                memberA.getMemberName(),
                memberB.getMemberName(),
                calculationInput.getMemberA().getTotalDebt().compareTo(BigDecimal.ZERO) > 0,
                calculationInput.getMemberB().getTotalDebt().compareTo(BigDecimal.ZERO) > 0,
                calculationResult.getMemberADebtScore(),
                calculationResult.getMemberBDebtScore()
        );

        FinancialValueReasonInput financialValueReasonInput = new FinancialValueReasonInput(
                calculationInput.getMemberA().getFinancialAssetRatioScore(),
                calculationInput.getMemberA().getInvestmentExperienceScore(),
                calculationInput.getMemberA().getFinancialKnowledgeScore(),
                calculationInput.getMemberA().getCapitalPreservationScore(),
                calculationInput.getMemberB().getFinancialAssetRatioScore(),
                calculationInput.getMemberB().getInvestmentExperienceScore(),
                calculationInput.getMemberB().getFinancialKnowledgeScore(),
                calculationInput.getMemberB().getCapitalPreservationScore()
        );

        TaxSavingProfile memberATaxProfile = buildTaxSavingProfile(calculationInput.getMemberA());
        TaxSavingProfile memberBTaxProfile = buildTaxSavingProfile(calculationInput.getMemberB());

        TaxStrategyReasonInput taxStrategyReasonInput = new TaxStrategyReasonInput(
                memberA.getMemberName(),
                memberB.getMemberName(),
                memberATaxProfile,
                memberBTaxProfile
        );

        debtRepaymentScoreService.generateAndSave(savedResult.getId(), debtRepaymentReasonInput);
        financialValueScoreService.generateAndSave(savedResult.getId(), financialValueReasonInput);
        taxStrategyScoreService.generateAndSave(savedResult.getId(), taxStrategyReasonInput);

        // 13. 종합 코멘트(expert_comment) 생성·저장 — 5축 중 유일하게 LLM을 쓰는 부분이라
        // OverallCommentService 내부에서 @Async(taskExecutor)로 돌아 이 메서드를 막지 않는다.
        OverallCommentPromptInput overallCommentInput =
                overallCommentInputBuilder.build(
                        calculationInput,
                        calculationResult,
                        memberA.getMemberName(),
                        memberB.getMemberName(),
                        couple.getFirstGoalType(),
                        memberATaxProfile,
                        memberBTaxProfile);
        overallCommentService.generateAndSave(savedResult.getId(), overallCommentInput);

        return savedResult;
    }

    /**
     * 종합 코멘트({@code report.expert_comment}) 생성이 {@link OverallCommentService}의 재시도
     * 횟수까지 전부 실패해서 비어있는 경우, 프론트의 "다시 시도" 버튼으로 코멘트만 재생성한다.
     * 5축 점수({@link CompatibilityResult})는 이미 계산·저장돼 있으니 재계산·재저장하지 않고, 원천
     * 데이터로 계산 입력값만 다시 만들어({@link MatchCalculationPersistenceService#recalculate})
     * 기존 {@code CompatibilityResult}의 id에 새 코멘트를 덮어쓴다.
     */
    public void retryOverallComment(Long memberId) {
        if (memberId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }

        MatchCoupleData couple = matchMapper.findCoupleDataByMemberId(memberId);
        if (couple == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "연결된 커플 정보를 찾을 수 없습니다.");
        }

        CompatibilityResult existingResult =
                matchMapper.findCompatibilityResultByCoupleId(couple.getCoupleId());
        if (existingResult == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "금융 궁합도 계산 결과를 찾을 수 없습니다.");
        }

        MatchMemberData memberA = matchMapper.findMemberDataByMemberId(couple.getInviterId());
        MatchMemberData memberB = matchMapper.findMemberDataByMemberId(couple.getInviteeId());
        if (memberA == null || memberB == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "금융 궁합도 계산에 필요한 회원 정보를 찾을 수 없습니다.");
        }

        MatchCalculationPersistenceResult recalculated =
                matchCalculationPersistenceService.recalculate(couple, memberA, memberB, existingResult);
        MatchCalculationInput calculationInput = recalculated.calculationInput();
        MatchCalculationResult calculationResult = recalculated.calculationResult();

        TaxSavingProfile memberATaxProfile = buildTaxSavingProfile(calculationInput.getMemberA());
        TaxSavingProfile memberBTaxProfile = buildTaxSavingProfile(calculationInput.getMemberB());

        OverallCommentPromptInput overallCommentInput =
                overallCommentInputBuilder.build(
                        calculationInput,
                        calculationResult,
                        memberA.getMemberName(),
                        memberB.getMemberName(),
                        couple.getFirstGoalType(),
                        memberATaxProfile,
                        memberBTaxProfile);

        overallCommentService.generateAndSave(existingResult.getId(), overallCommentInput);
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

    // 절세 계산기(calculateTaxStrategyScore)와 같은 한도 상수를 쓴다 — IRP는 계산기가 연금저축·DC와
    // 합산 900만원 한도로 계산하지만(calculatePensionUtilization), reason 문구는 팀 확정대로
    // 연금저축(600만)·IRP(900만)를 독립된 두 계좌처럼 각각 표시한다.
    private TaxSavingProfile buildTaxSavingProfile(MemberCalculationInput member) {
        TaxAccountInput isa = new TaxAccountInput(
                member.isHasIsa(), member.getIsaAnnualDeposit(), MatchCalculator.ISA_ANNUAL_LIMIT_AMOUNT);

        TaxAccountInput irp = new TaxAccountInput(
                member.isHasIrp(),
                member.getIrpAnnualPayment().add(member.getDcAnnualPayment()),
                MatchCalculator.PENSION_IRP_ANNUAL_LIMIT);

        TaxAccountInput pension = new TaxAccountInput(
                member.isHasPensionSaving(),
                member.getPensionAnnualPayment(),
                MatchCalculator.PENSION_SAVING_ANNUAL_LIMIT);

        return new TaxSavingProfile(isa, irp, pension);
    }
}