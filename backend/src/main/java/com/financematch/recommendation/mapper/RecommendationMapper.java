package com.financematch.recommendation.mapper;

import com.financematch.recommendation.domain.Recommendation;
import com.financematch.recommendation.domain.JointRecommendationProduct;
import com.financematch.recommendation.domain.PersonalInvestmentProduct;
import com.financematch.recommendation.domain.PersonalTaxSavingProduct;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.domain.RecommendationSlot;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationMapper {

    // 커플의 최신 추천 결과 ID를 조회한다.
    Long findRecommendationIdByCoupleId(@Param("coupleId") Long coupleId);

    // 로그인 회원이 소유한 커플의 추천 결과 ID만 조회한다.
    Long findRecommendationIdByMemberId(@Param("memberId") Long memberId);

    // 특정 커플 추천 결과를 회원 소유 조건과 함께 조회한다.
    Long findRecommendationIdByCoupleIdAndMemberId(
            @Param("coupleId") Long coupleId,
            @Param("memberId") Long memberId);

    // 로그인 회원이 소유한 추천 부모와 현재 커플의 고금리 부채 여부를 조회한다.
    RecommendationResult findRecommendationResultByMemberId(@Param("memberId") Long memberId);

    // 로그인 회원이 소유한 추천의 공동 슬롯 상품을 저장 순위대로 조회한다.
    List<JointRecommendationProduct> findJointProductsByRecommendationIdAndMemberId(
            @Param("recommendationId") Long recommendationId,
            @Param("memberId") Long memberId);

    // 로그인 회원 본인의 개인 절세 추천만 조회한다.
    List<PersonalTaxSavingProduct> findPersonalTaxSavingByMemberId(
            @Param("memberId") Long memberId);

    // 로그인 회원 본인의 개인 투자 추천만 조회한다.
    List<PersonalInvestmentProduct> findPersonalInvestmentByMemberId(
            @Param("memberId") Long memberId);

    // 추천 입력이나 상품 정보가 추천 저장 시점 이후 변경되지 않았는지 확인한다.
    boolean isRecommendationCurrentByMemberId(@Param("memberId") Long memberId);

    // recommendation 부모 row를 생성하거나 기존 row의 updated_at만 갱신한다.
    int upsertRecommendation(Recommendation recommendation);

    // 공동 추천 갱신 전 기존 슬롯과 슬롯 상품 후보를 제거한다.
    int deleteSlotsByRecommendationId(@Param("recommendationId") Long recommendationId);

    // 공동 추천 대분류 슬롯을 저장한다.
    int insertRecommendationSlot(RecommendationSlot slot);

    // 공동 추천 슬롯 안의 후보 상품을 저장한다.
    int insertRecommendationProduct(
            @Param("slotId") Long slotId,
            @Param("product") RecommendedProduct product);

    // 개인 절세 추천 갱신 전 대상 회원의 기존 결과를 제거한다.
    int deletePersonalTaxSavingByMemberId(@Param("memberId") Long memberId);

    // 개인 절세 추천 후보 상품을 저장한다.
    int insertPersonalTaxSaving(
            @Param("memberId") Long memberId,
            @Param("product") RecommendedProduct product);

    // 개인 투자 추천 갱신 전 대상 회원의 기존 결과를 제거한다.
    int deletePersonalInvestmentByMemberId(@Param("memberId") Long memberId);

    // 개인 투자 추천 후보 상품을 저장한다.
    int insertPersonalInvestment(
            @Param("memberId") Long memberId,
            @Param("product") RecommendedProduct product);

    // 테스트와 검증에서 커플별 recommendation UNIQUE 유지 여부를 확인한다.
    int countRecommendationsByCoupleId(@Param("coupleId") Long coupleId);

    // 테스트와 검증에서 저장된 공동 슬롯 수를 확인한다.
    int countSlotsByRecommendationId(@Param("recommendationId") Long recommendationId);

    // 테스트와 검증에서 저장된 공동 후보 상품 수를 확인한다.
    int countProductsByRecommendationId(@Param("recommendationId") Long recommendationId);

    // 테스트와 검증에서 슬롯별 대표 상품 수를 확인한다.
    int countSelectedProductsByRecommendationId(@Param("recommendationId") Long recommendationId);

    // 테스트와 검증에서 특정 공동 슬롯 저장 여부를 확인한다.
    int countSlotsByType(
            @Param("recommendationId") Long recommendationId,
            @Param("slotType") RecommendationSlotType slotType);

    // 테스트와 검증에서 개인 절세 추천 저장 건수를 확인한다.
    int countPersonalTaxSavingByMemberId(@Param("memberId") Long memberId);

    // 테스트와 검증에서 개인 투자 추천 저장 건수를 확인한다.
    int countPersonalInvestmentByMemberId(@Param("memberId") Long memberId);
}
