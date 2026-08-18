package com.financematch.auth.service;

import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberAgreement;
import com.financematch.auth.dto.LoginRequest;
import com.financematch.auth.dto.LoginResponse;
import com.financematch.auth.dto.SignupRequest;
import com.financematch.auth.dto.SignupResponse;
import com.financematch.auth.dto.TokenResponse;
import com.financematch.auth.jwt.JwtProvider;
import com.financematch.auth.jwt.RefreshTokenStore;
import com.financematch.auth.jwt.TokenBlacklist;
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
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklist tokenBlacklist;

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

        return SignupResponse.of(
                member,
                jwtProvider.createAccessToken(member.getId()),
                issueRefreshToken(member.getId()));
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
                member,
                jwtProvider.createAccessToken(member.getId()),
                issueRefreshToken(member.getId()),
                isFirstLogin,
                progress);
    }

    /**
     * refresh 토큰으로 새 토큰 한 쌍을 발급한다.
     *
     * <p>서명·만료 검증만으로는 부족하다. 서버가 보관 중인 값과 같은지 반드시 확인한다 — 로그아웃한
     * 토큰과 이미 한 번 쓴 토큰은 서명이 멀쩡해도 거절되어야 하기 때문이다.
     *
     * <p><b>쓴 토큰은 즉시 새것으로 바꾼다(회전).</b> 저장소에는 회원당 하나만 두므로, 새로 저장하는
     * 순간 방금 쓴 토큰은 자동으로 무효가 된다. 토큰이 새어 나가도 원래 사용자가 다음 재발급을 하는
     * 순간 공격자의 것은 못 쓰게 된다.
     *
     * <p>DB 를 건드리지 않아 {@code @Transactional} 을 붙이지 않는다.
     */
    public TokenResponse reissue(String refreshToken) {

        Long memberId = jwtProvider.getMemberIdFromRefreshToken(refreshToken);

        if (!refreshTokenStore.matches(memberId, refreshToken)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return TokenResponse.of(
                jwtProvider.createAccessToken(memberId), issueRefreshToken(memberId));
    }

    /**
     * refresh 토큰을 발급하고 저장소에 기록한다.
     *
     * <p>발급과 저장은 항상 붙어 다녀야 한다. 저장을 빠뜨리면 재발급 때 대조에 실패해 로그인이 바로
     * 풀리므로, 두 동작을 한 자리에 묶어 둔다.
     */
    private String issueRefreshToken(Long memberId) {
        String refreshToken = jwtProvider.createRefreshToken(memberId);
        refreshTokenStore.save(memberId, refreshToken, jwtProvider.getRefreshTokenValidityMs());
        return refreshToken;
    }

    /**
     * 로그아웃. 서버가 보관하던 refresh 토큰을 지우고, 쓰던 access 토큰을 폐기 목록에 올린다.
     *
     * <p>둘 다 해야 실제로 로그아웃이 된다. refresh 만 지우면 남은 access 토큰(최대 1시간)이 계속
     * 통하고, access 만 막으면 refresh 로 새 access 를 받아버린다.
     *
     * <p>access 토큰은 만료까지 남은 시간만 막는다. 그 뒤에는 어차피 통하지 않으므로 목록에 남겨둘
     * 이유가 없다({@link TokenBlacklist}).
     *
     * <p>DB 를 건드리지 않아 {@code @Transactional} 을 붙이지 않는다.
     *
     * @param accessToken 요청에 실려 온 access 토큰. 없으면 폐기 등록은 건너뛰고 refresh 만 지운다.
     */
    public void logout(Long memberId, String accessToken) {

        refreshTokenStore.delete(memberId);

        if (accessToken != null) {
            try {
                tokenBlacklist.add(
                        jwtProvider.getJti(accessToken), jwtProvider.getRemainingMs(accessToken));

                // 여기까지 왔다면 필터가 이미 검증한 토큰이라 정상적으로는 나지 않는다. 다만 토큰
                // 해석이 실패했다고 로그아웃 자체를 실패시킬 이유는 없다 — refresh 는 이미 지웠고,
                // 해석되지 않는 토큰은 어차피 인증을 통과하지 못한다.
            } catch (ApiException e) {
                log.debug("로그아웃 중 access 토큰 해석 실패 — memberId={}", memberId);
            }
        }

        log.info("로그아웃 — memberId={}", memberId);
    }
}
