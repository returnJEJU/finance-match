package com.financematch.auth.service;

import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberAgreement;
import com.financematch.auth.dto.LoginRequest;
import com.financematch.auth.dto.LoginResponse;
import com.financematch.auth.dto.SignupRequest;
import com.financematch.auth.dto.SignupResponse;
import com.financematch.auth.jwt.JwtProvider;
import com.financematch.auth.mapper.MemberMapper;
import com.financematch.common.ErrorCode;
import com.financematch.exception.ApiException;
import com.financematch.onboarding.dto.OnboardingStatusResponse;
import com.financematch.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 진행 상태 판단은 온보딩 쪽 것을 그대로 쓴다. 같은 판단을 auth 에 복제하면 "커플 연결" 정의가 바뀔 때
     * 한쪽만 고쳐 로그인과 온보딩 화면이 서로 다른 답을 하게 된다.
     */
    private final OnboardingService onboardingService;

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

    /**
     * 이메일·비밀번호를 검증하고 토큰과 다음 화면 판단용 정보를 돌려준다.
     *
     * <p>"없는 이메일"·"비밀번호 불일치"·"탈퇴 회원"을 모두 {@code INVALID_CREDENTIALS} 로 응답한다.
     * 구분해서 알려주면 응답만 비교해 가입된 이메일 목록을 만들 수 있다(계정 열거).
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {

        Member member = memberMapper.findByEmail(request.getEmail());

        if (member == null
                || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 갱신하면 사라지는 값이므로 갱신 전에 판단한다.
        boolean isFirstLogin = member.getLastLoginAt() == null;

        // 본인이 확정된 뒤에만 조회한다. 실패하는 로그인에 불필요한 쿼리를 돌리지 않는다.
        OnboardingStatusResponse progress = onboardingService.getOnboardingStatus(member.getId());
        memberMapper.updateLastLoginAt(member.getId());

        return LoginResponse.of(
                member, jwtProvider.createAccessToken(member.getId()), isFirstLogin, progress);
    }

    /**
     * 로그아웃.
     *
     * <p>JWT 는 서버가 로그인 상태를 들고 있지 않으므로 지울 세션이 없다. 실제 무효화는 클라이언트가 저장된
     * accessToken 을 삭제해 이뤄지고, 서버는 호출 사실만 남기고 성공을 응답한다. 토큰 자체는 만료(1시간)
     * 전까지 유효하다.
     *
     * <p>즉시 무효화가 필요해지면 이 자리에 Redis 블랙리스트를 얹는다 — 토큰의 {@code jti} 를 남은
     * 유효시간을 TTL 로 저장하고, {@code JwtAuthenticationFilter} 가 조회해 걸러낸다. 그때 계약(경로·요청·
     * 응답)은 그대로 두고 서버 내부만 바뀐다.
     *
     * <p>DB 를 건드리지 않아 {@code @Transactional} 을 붙이지 않는다.
     */
    public void logout(Long memberId) {
        log.info("로그아웃 — memberId={}", memberId);
    }
}
