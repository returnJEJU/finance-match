package com.financematch.couple.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.financematch.common.ErrorCode;
import com.financematch.config.RootConfig;
import com.financematch.couple.dto.CoupleProfileMessageResponse;
import com.financematch.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@WebAppConfiguration
@Transactional
class CoupleServiceTest {

    @Autowired
    private CoupleService coupleService;

    @Test
    void 한_명이_수정한_커플_한줄소개를_파트너도_조회한다() {

        // given
        String profileMessage = "함께 모으고 함께 투자하기";

        // when
        CoupleProfileMessageResponse updated =
                coupleService.updateProfileMessage(
                        1L,
                        profileMessage);
        CoupleProfileMessageResponse partnerView =
                coupleService.getProfileMessage(2L);

        // then
        assertEquals(profileMessage, updated.getProfileMessage());
        assertEquals(profileMessage, partnerView.getProfileMessage());
    }

    @Test
    void 연결된_커플이_없으면_한줄소개를_조회할_수_없다() {

        // when
        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> coupleService.getProfileMessage(9999L));

        // then
        assertEquals(ErrorCode.COUPLE_NOT_CONNECTED, exception.getErrorCode());
    }
}
