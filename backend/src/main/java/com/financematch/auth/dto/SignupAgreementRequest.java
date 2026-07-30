package com.financematch.auth.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 약관 동의 묶음.
 *
 * <p>여기서는 "값이 왔는지"만 검증한다. 필수 4종이 모두 {@code true} 인지는 {@code AuthService} 에서
 * 확인해 {@code CONSENT_REQUIRED} 로 응답한다 — 어노테이션 검증으로는 에러 코드를 지정할 수 없다.
 */
@Getter
@NoArgsConstructor
public class SignupAgreementRequest {

    @NotNull(message = "마이데이터 서비스 이용약관 동의 여부가 필요합니다.")
    private Boolean mydataTerms;

    @NotNull(message = "개인정보 수집·이용 동의 여부가 필요합니다.")
    private Boolean privacy;

    @NotNull(message = "자산정보 연동 동의 여부가 필요합니다.")
    private Boolean assetLink;

    @NotNull(message = "자산 궁합 매칭·상대방 공유 동의 여부가 필요합니다.")
    private Boolean coupleShare;

    /** 선택 항목이라 값이 오지 않을 수 있다. 저장 시 null 은 false 로 다룬다. */
    private Boolean marketing;

    /** 필수 4종에 모두 동의했는지. */
    public boolean hasAllRequiredAgreements() {
        return Boolean.TRUE.equals(mydataTerms)
                && Boolean.TRUE.equals(privacy)
                && Boolean.TRUE.equals(assetLink)
                && Boolean.TRUE.equals(coupleShare);
    }
}
