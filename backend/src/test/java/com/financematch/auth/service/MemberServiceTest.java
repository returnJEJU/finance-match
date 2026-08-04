package com.financematch.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.financematch.auth.domain.Member;
import com.financematch.auth.domain.MemberStatus;
import com.financematch.auth.dto.MemberProfileResponse;
import com.financematch.auth.dto.WithdrawRequest;
import com.financematch.auth.dto.WithdrawResponse;
import com.financematch.auth.mapper.MemberMapper;
import com.financematch.common.ErrorCode;
import com.financematch.couple.service.CoupleService;
import com.financematch.exception.ApiException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 회원탈퇴. 되돌리기 어려운 동작이라 각 거절 사유와 검사 순서를 고정한다. */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ENCODED_PASSWORD = "$2a$10$abcdefghijklmnopqrstuv";
    private static final String RAW_PASSWORD = "Pw123456!";
    private static final String CONFIRMATION_TEXT = "회원 탈퇴";

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock private MemberMapper memberMapper;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private CoupleService coupleService;

    @InjectMocks private MemberService memberService;

    @Captor private ArgumentCaptor<LocalDateTime> withdrawnAtCaptor;

    @Test
    void 내_프로필은_회원_ID_와_이름을_응답한다() {
        when(memberMapper.findById(MEMBER_ID)).thenReturn(member(MemberStatus.ACTIVE));

        MemberProfileResponse response = memberService.getProfile(MEMBER_ID);

        assertEquals(MEMBER_ID, response.getId());
        assertEquals("홍길동", response.getName());
    }

    @Test
    void 내_프로필_조회에서_회원이_없으면_MEMBER_NOT_FOUND_이다() {
        when(memberMapper.findById(MEMBER_ID)).thenReturn(null);

        ApiException e =
                assertThrows(ApiException.class, () -> memberService.getProfile(MEMBER_ID));

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void 탈퇴하면_회원_ID_와_상태와_처리_시각을_응답한다() throws Exception {
        given성공();

        WithdrawResponse response = memberService.withdraw(MEMBER_ID, request(CONFIRMATION_TEXT));

        assertEquals(MEMBER_ID, response.getMemberId());
        assertEquals(MemberStatus.WITHDRAWN, response.getStatus());
        verify(coupleService).disconnectCoupleIfConnected(MEMBER_ID);
        verify(memberMapper).withdraw(eq(MEMBER_ID), any(LocalDateTime.class));
    }

    /**
     * 응답으로 알려준 시각과 DB 에 저장하는 값이 같아야 한다. 서로 다르면 클라이언트가 조회한 탈퇴 시각이
     * 응답과 어긋난다.
     */
    @Test
    void 응답의_처리_시각과_저장한_시각이_같다() throws Exception {
        given성공();

        WithdrawResponse response = memberService.withdraw(MEMBER_ID, request(CONFIRMATION_TEXT));

        verify(memberMapper).withdraw(eq(MEMBER_ID), withdrawnAtCaptor.capture());
        assertEquals(withdrawnAtCaptor.getValue(), response.getWithdrawnAt());
    }

    /**
     * {@code member.deleted_at} 은 소수점 이하가 없는 datetime 이다. 자르지 않고 넘기면 MySQL 이
     * 반올림해 응답과 저장값이 최대 1초까지 어긋난다.
     */
    @Test
    void 처리_시각은_초_단위로_잘린다() throws Exception {
        given성공();

        WithdrawResponse response = memberService.withdraw(MEMBER_ID, request(CONFIRMATION_TEXT));

        assertEquals(0, response.getWithdrawnAt().getNano());
    }

    @Test
    void 없는_회원이면_MEMBER_NOT_FOUND_이다() throws Exception {
        when(memberMapper.findByIdIncludingWithdrawn(MEMBER_ID)).thenReturn(null);

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> memberService.withdraw(MEMBER_ID, request(CONFIRMATION_TEXT)));

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, e.getErrorCode());
        verifyNoInteractions(coupleService);
        verify(memberMapper, never()).withdraw(any(), any());
    }

    @Test
    void 이미_탈퇴한_회원이면_MEMBER_ALREADY_WITHDRAWN_이다() throws Exception {
        when(memberMapper.findByIdIncludingWithdrawn(MEMBER_ID))
                .thenReturn(member(MemberStatus.WITHDRAWN));

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> memberService.withdraw(MEMBER_ID, request(CONFIRMATION_TEXT)));

        assertEquals(ErrorCode.MEMBER_ALREADY_WITHDRAWN, e.getErrorCode());
        verifyNoInteractions(coupleService);
        verify(memberMapper, never()).withdraw(any(), any());
    }

    @Test
    void 확인_문구가_다르면_INVALID_CONFIRMATION_이다() throws Exception {
        when(memberMapper.findByIdIncludingWithdrawn(MEMBER_ID))
                .thenReturn(member(MemberStatus.ACTIVE));

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> memberService.withdraw(MEMBER_ID, request("탈퇴할래요")));

        assertEquals(ErrorCode.INVALID_CONFIRMATION, e.getErrorCode());
        verifyNoInteractions(coupleService);
    }

    /**
     * 검사 순서를 고정한다. BCrypt 검증은 무차별 대입을 늦추려고 일부러 느리게 만든 연산이라, 문자열 비교로
     * 걸러낼 수 있는 요청에는 치르지 않는다.
     */
    @Test
    void 확인_문구가_틀리면_비밀번호는_검증하지_않는다() throws Exception {
        when(memberMapper.findByIdIncludingWithdrawn(MEMBER_ID))
                .thenReturn(member(MemberStatus.ACTIVE));

        assertThrows(
                ApiException.class, () -> memberService.withdraw(MEMBER_ID, request("탈퇴할래요")));

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(coupleService);
    }

    @Test
    void 비밀번호가_다르면_INVALID_PASSWORD_이다() throws Exception {
        when(memberMapper.findByIdIncludingWithdrawn(MEMBER_ID))
                .thenReturn(member(MemberStatus.ACTIVE));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> memberService.withdraw(MEMBER_ID, request(CONFIRMATION_TEXT)));

        assertEquals(ErrorCode.INVALID_PASSWORD, e.getErrorCode());
        verifyNoInteractions(coupleService);
        verify(memberMapper, never()).withdraw(any(), any());
    }

    /** 조회와 갱신 사이에 다른 요청이 먼저 탈퇴시킨 경우. UPDATE 가 0행이 된다. */
    @Test
    void 갱신된_행이_없으면_MEMBER_ALREADY_WITHDRAWN_이다() throws Exception {
        when(memberMapper.findByIdIncludingWithdrawn(MEMBER_ID))
                .thenReturn(member(MemberStatus.ACTIVE));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(memberMapper.withdraw(eq(MEMBER_ID), any(LocalDateTime.class))).thenReturn(0);

        ApiException e =
                assertThrows(
                        ApiException.class,
                        () -> memberService.withdraw(MEMBER_ID, request(CONFIRMATION_TEXT)));

        assertEquals(ErrorCode.MEMBER_ALREADY_WITHDRAWN, e.getErrorCode());
    }

    private void given성공() {
        when(memberMapper.findByIdIncludingWithdrawn(MEMBER_ID))
                .thenReturn(member(MemberStatus.ACTIVE));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(memberMapper.withdraw(eq(MEMBER_ID), any(LocalDateTime.class))).thenReturn(1);
    }

    /** setter 가 없는 domain 이라 JSON 으로 만든다({@code MemberTest} 와 같은 방식). */
    private Member member(MemberStatus status) {
        try {
            return objectMapper.readValue(
                    """
                    {
                      "id": 1,
                      "email": "hong@kb.com",
                      "password": "%s",
                      "name": "홍길동",
                      "gender": "F",
                      "birthDate": "1995-03-21",
                      "status": "%s"
                    }
                    """
                            .formatted(ENCODED_PASSWORD, status.name()),
                    Member.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private WithdrawRequest request(String confirmationText) throws Exception {
        return objectMapper.readValue(
                """
                { "password": "%s", "confirmationText": "%s" }
                """
                        .formatted(RAW_PASSWORD, confirmationText),
                WithdrawRequest.class);
    }
}
