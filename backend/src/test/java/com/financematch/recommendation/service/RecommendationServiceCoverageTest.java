package com.financematch.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.financematch.common.ErrorCode;
import com.financematch.couple.mapper.CoupleMapper;
import com.financematch.exception.ApiException;
import com.financematch.recommendation.domain.Recommendation;
import com.financematch.recommendation.domain.RecommendationContext;
import com.financematch.recommendation.mapper.RecommendationContextMapper;
import com.financematch.recommendation.mapper.RecommendationMapper;
import com.financematch.recommendation.policy.RecommendedProduct;
import com.financematch.recommendation.type.PersonalRecommendationType;
import com.financematch.recommendation.type.RecommendationSlotType;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceCoverageTest {

    @Mock private RecommendationContextMapper contextMapper;
    @Mock private RecommendationMapper recommendationMapper;
    @Mock private RecommendationPlanner planner;
    @Mock private CoupleMapper coupleMapper;
    @Mock private RecommendationResponseAssembler assembler;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(contextMapper, recommendationMapper, planner, coupleMapper, assembler);
    }

    @Test
    void returnsCoupleNotConnectedWhenLockedCoupleDisappears() {
        when(recommendationMapper.findCoupleIdByMemberId(1L)).thenReturn(10L);
        when(recommendationMapper.lockCoupleIdByIdNowait(10L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> service.createRecommendation(1L));

        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }

    @Test
    void validatesMemberAndMissingContextWithoutCouple() {
        assertThrows(IllegalArgumentException.class, () -> service.recommend(null));
        when(contextMapper.findByMemberId(1L)).thenReturn(null);
        when(coupleMapper.existsCoupleByMemberId(1L)).thenReturn(false);

        ApiException exception = assertThrows(ApiException.class, () -> service.recommend(1L));

        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }

    @Test
    void failsWhenContextHasNoCoupleId() {
        prepareCreate(context(null), new RecommendationPlan(Map.of(), Map.of()));

        ApiException exception =
                assertThrows(ApiException.class, () -> service.createRecommendation(1L));

        assertEquals(ErrorCode.RECOMMENDATION_FAILED, exception.getErrorCode());
    }

    @Test
    void failsWhenRecommendationParentIdIsNotGenerated() {
        prepareCreate(context(10L), new RecommendationPlan(Map.of(), Map.of()));

        ApiException exception =
                assertThrows(ApiException.class, () -> service.createRecommendation(1L));

        assertEquals(ErrorCode.RECOMMENDATION_FAILED, exception.getErrorCode());
    }

    @Test
    void skipsEmptyJointSlotsAndMissingPersonalGroups() {
        Map<RecommendationSlotType, List<RecommendedProduct>> joint =
                new EnumMap<>(RecommendationSlotType.class);
        joint.put(RecommendationSlotType.SAVINGS, List.of());
        prepareCreate(context(10L), new RecommendationPlan(joint, Map.of()));
        generateParentId();

        service.createRecommendation(1L);
    }

    @Test
    void failsWhenJointSlotIdIsNotGenerated() {
        Map<RecommendationSlotType, List<RecommendedProduct>> joint =
                Map.of(RecommendationSlotType.DEPOSIT, List.of(new RecommendedProduct(1L, 1, true)));
        prepareCreate(context(10L), new RecommendationPlan(joint, Map.of()));
        generateParentId();

        ApiException exception =
                assertThrows(ApiException.class, () -> service.createRecommendation(1L));

        assertEquals(ErrorCode.RECOMMENDATION_FAILED, exception.getErrorCode());
    }

    @Test
    void savesPresentPersonalRecommendationGroups() {
        Map<PersonalRecommendationType, Map<Long, List<RecommendedProduct>>> personal =
                new EnumMap<>(PersonalRecommendationType.class);
        personal.put(
                PersonalRecommendationType.TAX_SAVING,
                Map.of(1L, List.of(new RecommendedProduct(1L, 1, true))));
        personal.put(
                PersonalRecommendationType.INVESTMENT,
                Map.of(2L, List.of(new RecommendedProduct(2L, 1, true))));
        prepareCreate(context(10L), new RecommendationPlan(Map.of(), personal));
        generateParentId();

        service.createRecommendation(1L);
    }

    private RecommendationContext context(Long coupleId) {
        RecommendationContext context = new RecommendationContext();
        context.setCoupleId(coupleId);
        context.setInviterId(1L);
        context.setInviteeId(2L);
        return context;
    }

    private void prepareCreate(RecommendationContext context, RecommendationPlan plan) {
        LocalDateTime version = LocalDateTime.of(2026, 8, 20, 1, 0);
        when(recommendationMapper.findCoupleIdByMemberId(1L)).thenReturn(10L);
        when(recommendationMapper.lockCoupleIdByIdNowait(10L)).thenReturn(10L);
        when(recommendationMapper.findLatestInputUpdatedAtByMemberId(1L))
                .thenReturn(version, version);
        when(contextMapper.findByMemberId(1L)).thenReturn(context);
        when(planner.create(context)).thenReturn(plan);
    }

    private void generateParentId() {
        doAnswer(
                        invocation -> {
                            Recommendation recommendation = invocation.getArgument(0);
                            recommendation.setId(100L);
                            return 1;
                        })
                .when(recommendationMapper)
                .upsertRecommendation(any(Recommendation.class));
    }
}
