package com.financematch.auth.mapper;

import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberAgreement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper {

    boolean existsByEmail(String email);

    int insert(Member member);

    int insertAgreement(MemberAgreement memberAgreement);
}
