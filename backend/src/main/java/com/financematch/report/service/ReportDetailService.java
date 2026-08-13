package com.financematch.report.service;

import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.report.domain.ReportCoupleDetailSource;
import com.financematch.report.domain.ReportMemberDetailSource;
import com.financematch.report.dto.detail.ReportDetails;
import com.financematch.report.mapper.ReportDetailMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportDetailService {

    private final ReportDetailMapper reportDetailMapper;
    private final ReportDetailFactory reportDetailFactory;

    public ReportDetails getDetails(Long memberId) {
        ReportCoupleDetailSource couple = reportDetailMapper.findCoupleDetailByMemberId(memberId);
        if (couple == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "리포트 상세 데이터를 찾을 수 없습니다.");
        }

        List<ReportMemberDetailSource> members =
                reportDetailMapper.findMemberDetailsByCoupleId(couple.getCoupleId());
        if (members.size() != 2) {
            throw new ApiException(ErrorCode.NOT_FOUND, "커플 리포트 상세 데이터를 찾을 수 없습니다.");
        }

        ReportMemberDetailSource me =
                members.stream()
                        .filter(member -> memberId.equals(member.getMemberId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ErrorCode.NOT_FOUND,
                                                "로그인 회원의 리포트 상세 데이터를 찾을 수 없습니다."));
        ReportMemberDetailSource partner =
                members.stream()
                        .filter(member -> !memberId.equals(member.getMemberId()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                ErrorCode.NOT_FOUND,
                                                "파트너의 리포트 상세 데이터를 찾을 수 없습니다."));

        BigDecimal peerAssetMedian = reportDetailMapper.findMedianFinancialAssetByAge(averageAge(me, partner));

        return reportDetailFactory.create(memberId, couple, me, partner, peerAssetMedian);
    }

    private int averageAge(ReportMemberDetailSource me, ReportMemberDetailSource partner) {
        return (age(me.getBirthDate()) + age(partner.getBirthDate())) / 2;
    }

    private int age(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
