package com.financematch.auth.service;

import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberAgreement;
import com.financematch.auth.dto.SignupRequest;
import com.financematch.auth.dto.SignupResponse;
import com.financematch.auth.jwt.JwtProvider;
import com.financematch.auth.mapper.MemberMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 회원 계정을 만들고 약관 동의를 함께 저장한다.
     *
     * <p>응답의 accessToken 은 다음 단계인 자산연동을 인증하기 위한 것이다. 로그인이 아니므로 {@code
     * last_login_at} 은 갱신하지 않는다 — 가입 후 최초 로그인에서 첫 로그인으로 판별되어야 한다.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {

        // DB 를 타지 않는 검사를 먼저 한다.
        if (!request.getAgreements().hasAllRequiredAgreements()) {
            throw new ApiException(ErrorCode.CONSENT_REQUIRED);
        }

        if (memberMapper.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_EXISTS);
        }

        // 평문 비밀번호는 여기서 해시로 바꾼 뒤 domain 으로 넘긴다.
        Member member = Member.of(request, passwordEncoder.encode(request.getPassword()));
        memberMapper.insert(member);

        // insert 후 member.id 에 자동 생성된 PK 가 채워져 있다(useGeneratedKeys).
        memberMapper.insertAgreement(MemberAgreement.of(member.getId(), request.getAgreements()));

        return SignupResponse.of(member, jwtProvider.createAccessToken(member.getId()));
    }
}
