package com.financematch.auth.domain;

import com.financematch.auth.dto.SignupAgreementRequest;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 약관 동의. {@code member_agreement} 테이블 한 행에 대응한다.
 *
 * <p>{@code uk_agreement_member} 제약이 있어 회원당 1행이다. {@code agreed_at} 은 NOT NULL 인데 DB
 * 기본값이 없으므로 여기서 채운다.
 */
@Getter
@NoArgsConstructor
public class MemberAgreement {

    @Setter // MyBatis가 자동 생성된 PK를 넣기 위해 필요
    private Long id;

    private Long memberId;
    private Boolean agreeMydataTerms;
    private Boolean agreePrivacy;
    private Boolean agreeAssetLink;
    private Boolean agreeCoupleShare;
    private Boolean agreeMarketing;
    private LocalDateTime agreedAt;

    /** 약관 동의 요청을 저장용 domain 객체로 변환한다. 선택 항목인 마케팅 동의는 null 을 false 로 다룬다. */
    public static MemberAgreement of(Long memberId, SignupAgreementRequest request) {

        MemberAgreement agreement = new MemberAgreement();
        agreement.memberId = memberId;
        agreement.agreeMydataTerms = request.getMydataTerms();
        agreement.agreePrivacy = request.getPrivacy();
        agreement.agreeAssetLink = request.getAssetLink();
        agreement.agreeCoupleShare = request.getCoupleShare();
        agreement.agreeMarketing = Boolean.TRUE.equals(request.getMarketing());
        agreement.agreedAt = LocalDateTime.now();

        return agreement;
    }
}
