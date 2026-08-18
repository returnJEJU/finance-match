package com.financematch.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.financematch.exception.ApiException;
import com.financematch.report.domain.ReportCoupleDetailSource;
import com.financematch.report.domain.ReportMemberDetailSource;
import com.financematch.report.dto.detail.ReportDetails;
import com.financematch.report.mapper.ReportDetailMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportDetailServiceTest {

    @Mock private ReportDetailMapper reportDetailMapper;
    @Mock private ReportDetailFactory reportDetailFactory;

    @InjectMocks private ReportDetailService reportDetailService;

    private ReportCoupleDetailSource couple(long coupleId) {
        ReportCoupleDetailSource couple = new ReportCoupleDetailSource();
        ReflectionTestUtils.setField(couple, "coupleId", coupleId);
        return couple;
    }

    private ReportMemberDetailSource member(long memberId, LocalDate birthDate) {
        ReportMemberDetailSource member = new ReportMemberDetailSource();
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "birthDate", birthDate);
        return member;
    }

    @Test
    void 커플_상세_데이터가_없으면_예외를_던진다() {
        when(reportDetailMapper.findCoupleDetailByMemberId(1L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> reportDetailService.getDetails(1L));
        assertEquals("리포트 상세 데이터를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 커플_구성원이_2명이_아니면_예외를_던진다() {
        when(reportDetailMapper.findCoupleDetailByMemberId(1L)).thenReturn(couple(10L));
        when(reportDetailMapper.findMemberDetailsByCoupleId(10L))
                .thenReturn(List.of(member(1L, LocalDate.of(1990, 1, 1))));

        ApiException exception =
                assertThrows(ApiException.class, () -> reportDetailService.getDetails(1L));
        assertEquals("커플 리포트 상세 데이터를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 로그인_회원이_구성원_목록에_없으면_예외를_던진다() {
        when(reportDetailMapper.findCoupleDetailByMemberId(1L)).thenReturn(couple(10L));
        when(reportDetailMapper.findMemberDetailsByCoupleId(10L))
                .thenReturn(
                        List.of(
                                member(2L, LocalDate.of(1990, 1, 1)), member(3L, LocalDate.of(1991, 1, 1))));

        ApiException exception =
                assertThrows(ApiException.class, () -> reportDetailService.getDetails(1L));
        assertEquals("로그인 회원의 리포트 상세 데이터를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 파트너가_구성원_목록에_없으면_예외를_던진다() {
        when(reportDetailMapper.findCoupleDetailByMemberId(1L)).thenReturn(couple(10L));
        when(reportDetailMapper.findMemberDetailsByCoupleId(10L))
                .thenReturn(
                        List.of(
                                member(1L, LocalDate.of(1990, 1, 1)), member(1L, LocalDate.of(1990, 1, 1))));

        ApiException exception =
                assertThrows(ApiException.class, () -> reportDetailService.getDetails(1L));
        assertEquals("파트너의 리포트 상세 데이터를 찾을 수 없습니다.", exception.getMessage());
    }

    @Test
    void 정상_조회시_평균_나이로_또래_중앙값을_조회하고_팩토리_결과를_그대로_반환한다() {
        ReportCoupleDetailSource couple = couple(10L);
        LocalDate today = LocalDate.now();
        ReportMemberDetailSource me = member(1L, today.minusYears(30));
        ReportMemberDetailSource partner = member(2L, today.minusYears(32));
        when(reportDetailMapper.findCoupleDetailByMemberId(1L)).thenReturn(couple);
        when(reportDetailMapper.findMemberDetailsByCoupleId(10L)).thenReturn(List.of(me, partner));
        when(reportDetailMapper.findMedianFinancialAssetByAge(31)).thenReturn(new BigDecimal("50000000"));
        ReportDetails details = new ReportDetails(null, null, null, null, null, null);
        when(reportDetailFactory.create(1L, couple, me, partner, new BigDecimal("50000000")))
                .thenReturn(details);

        ReportDetails result = reportDetailService.getDetails(1L);

        assertSame(details, result);
    }

    @Test
    void 생년월일이_없으면_나이를_0으로_취급한다() {
        ReportCoupleDetailSource couple = couple(10L);
        ReportMemberDetailSource me = member(1L, null);
        ReportMemberDetailSource partner = member(2L, null);
        when(reportDetailMapper.findCoupleDetailByMemberId(1L)).thenReturn(couple);
        when(reportDetailMapper.findMemberDetailsByCoupleId(10L)).thenReturn(List.of(me, partner));
        when(reportDetailMapper.findMedianFinancialAssetByAge(0)).thenReturn(BigDecimal.ZERO);
        when(reportDetailFactory.create(any(), any(), any(), any(), any()))
                .thenReturn(new ReportDetails(null, null, null, null, null, null));

        reportDetailService.getDetails(1L);

        org.mockito.Mockito.verify(reportDetailMapper).findMedianFinancialAssetByAge(0);
    }
}
