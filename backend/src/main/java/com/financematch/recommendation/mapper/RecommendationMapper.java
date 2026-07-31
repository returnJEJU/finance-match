package com.financematch.recommendation.mapper;

import com.financematch.recommendation.domain.Recommendation;
import com.financematch.recommendation.domain.JointRecommendationProduct;
import com.financematch.recommendation.domain.PersonalInvestmentProduct;
import com.financematch.recommendation.domain.PersonalTaxSavingProduct;
import com.financematch.recommendation.domain.RecommendationResult;
import com.financematch.recommendation.domain.RecommendationSlot;
import com.financematch.recommendation.policy.RecommendedProduct;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationMapper {

    // 같은 커플의 추천 생성 요청을 직렬화하고 대기 없이 중복 요청을 거부한다.
    Long lockCoupleIdByMemberIdNowait(@Param("memberId") Long memberId);

    // 추천 계산에 사용하는 모든 입력 데이터 중 가장 최근 수정 시각을 조회한다.
    LocalDateTime findLatestInputUpdatedAtByMemberId(@Param("memberId") Long memberId);

    // 로그인 회원이 소유한 커플의 추천 결과 ID만 조회한다.
    Long findRecommendationIdByMemberId(@Param("memberId") Long memberId);

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
}
