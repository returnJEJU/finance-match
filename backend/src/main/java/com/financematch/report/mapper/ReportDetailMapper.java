package com.financematch.report.mapper;

import com.financematch.report.domain.ReportCoupleDetailSource;
import com.financematch.report.domain.ReportMemberDetailSource;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportDetailMapper {

    ReportCoupleDetailSource findCoupleDetailByMemberId(@Param("memberId") Long memberId);

    List<ReportMemberDetailSource> findMemberDetailsByCoupleId(@Param("coupleId") Long coupleId);

    BigDecimal findMedianFinancialAssetByAge(@Param("age") int age);
}
