package com.financematch.auth.domain;

import com.financematch.auth.dto.SignupRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원. {@code member} 테이블 한 행에 대응한다.
 *
 * <p>{@code status}·{@code kb_star_savings_eligible} 등 나머지 컬럼은 DB 기본값을 쓰므로 여기에 두지
 * 않는다. {@code last_login_at} 은 로그인에서만 갱신한다(회원가입은 건드리지 않는다).
 */
@Getter
@NoArgsConstructor
public class Member {

    @Setter // MyBatis가 자동 생성된 PK를 넣기 위해 필요
    private Long id;

    private String email;
    private String password;
    private String name;
    private Gender gender;
    private LocalDate birthDate;

    /** 조회 시에만 채워진다. NULL 이면 아직 로그인한 적이 없다는 뜻(첫 로그인 판별용). */
    private LocalDateTime lastLoginAt;

    /**
     * 회원가입 요청을 저장용 domain 객체로 변환한다.
     *
     * @param encodedPassword BCrypt 로 해시된 비밀번호. 평문을 넘기지 않는다.
     */
    public static Member of(SignupRequest request, String encodedPassword) {

        Member member = new Member();
        member.email = request.getEmail();
        member.password = encodedPassword;
        member.name = request.getName();
        member.gender = request.getGender();
        member.birthDate = request.getBirthDate();

        return member;
    }
}
