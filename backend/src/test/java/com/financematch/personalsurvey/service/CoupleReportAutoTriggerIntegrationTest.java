package com.financematch.personalsurvey.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financematch.config.RootConfig;
import com.financematch.personalsurvey.dto.PersonalSurveyRequest;
import com.financematch.report.mapper.ReportMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * "커플 양쪽 다 개인 설문을 끝내면 리포트가 자동 생성된다"(CoupleReportTriggerListener)를 실제 Spring
 * 컨텍스트 + 실제 로컬 DB로 검증하는 통합 테스트다. HTTP 컨트롤러 대신 서비스 계층
 * ({@link PersonalSurveyService#save}) 을 직접 호출한다 — 실제 요청·인증(JWT) 없이도 같은 트랜잭션
 * 커밋 → 이벤트 발행 → 비동기 리스너 흐름을 그대로 재현할 수 있어서다.
 *
 * <p>{@code TaxStrategyPipelineExperimentTest}와 같은 이유로 기본 스킵 — 실제 DB를 지우고 다시
 * 쓰며, 두 번째 저장 직후 별도 스레드에서 LLM 3회 호출까지 실제로 발생한다(수십 초~수 분).
 *
 * <pre>{@code
 * ./gradlew test --tests "*CoupleReportAutoTriggerIntegrationTest*" -DrunLlmExperiment=true
 * }</pre>
 *
 * <p>사전 조건: couple_id=1(김하나=1·이두리=2)의 personal_survey·compatibility_result·report 가
 * 미리 비어있어야 한다(이 테스트가 끝에 정리하지만, 실행 도중 실패하면 수동 정리가 필요할 수 있다).
 */
@EnabledIfSystemProperty(named = "runLlmExperiment", matches = "true")
class CoupleReportAutoTriggerIntegrationTest {

    private static AnnotationConfigWebApplicationContext context;
    private static PersonalSurveyService personalSurveyService;
    private static ReportMapper reportMapper;

    @BeforeAll
    static void setUp() {
        // RootConfig 의 컴포넌트 스캔이 (com.financematch.config 전체를 훑다 보니) WebConfig 도
        // 같이 주워서 @EnableWebMvc 관련 빈을 만드는데, 그건 ServletContext 가 있어야 한다 — 실제
        // 배포에서는 ContextLoaderListener 가 진짜 ServletContext 를 주지만, 이 테스트는 순수 JVM
        // 실행이라 MockServletContext 로 그 요구조건만 충족시켜준다.
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(RootConfig.class);
        context.refresh();

        personalSurveyService = context.getBean(PersonalSurveyService.class);
        reportMapper = context.getBean(ReportMapper.class);
    }

    @AfterAll
    static void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void 파트너까지_설문을_끝내면_리포트가_자동으로_생긴다() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        PersonalSurveyRequest memberARequest =
                mapper.readValue(
                        """
                        {
                          "annualIncome": 45000000,
                          "monthlyAvailableAmount": 1000000,
                          "financialAssetRatio": "UNDER_50",
                          "investmentExperiences": ["LOW_RISK","MODERATE_LOW_RISK","MODERATE_RISK"],
                          "financialKnowledge": "MEDIUM",
                          "capitalPreservationAttitude": "UNDER_50"
                        }
                        """,
                        PersonalSurveyRequest.class);

        PersonalSurveyRequest memberBRequest =
                mapper.readValue(
                        """
                        {
                          "annualIncome": 75000000,
                          "monthlyAvailableAmount": 1500000,
                          "financialAssetRatio": "UNDER_80",
                          "investmentExperiences": ["LOW_RISK","MODERATE_HIGH_RISK","MODERATE_LOW_RISK","MODERATE_RISK"],
                          "financialKnowledge": "HIGH",
                          "capitalPreservationAttitude": "UNDER_10"
                        }
                        """,
                        PersonalSurveyRequest.class);

        System.out.println("=== 회원1(김하나) 설문 저장 ===");
        personalSurveyService.save(1L, memberARequest);
        System.out.println("아직 파트너(회원2) 미완료 — 리포트 없어야 정상: "
                + reportMapper.findReportRowByMemberId(1L));

        System.out.println("=== 회원2(이두리) 설문 저장 (여기서 자동 트리거 발행) ===");
        personalSurveyService.save(2L, memberBRequest);

        System.out.println("=== 백그라운드 리포트 생성 대기 (최대 3분, 5초 간격 폴링) ===");
        Instant deadline = Instant.now().plus(Duration.ofMinutes(3));
        Object row = null;
        while (Instant.now().isBefore(deadline)) {
            row = reportMapper.findReportRowByMemberId(1L);
            if (row != null) {
                break;
            }
            Thread.sleep(5000);
            System.out.println("... 아직 안 생김, 계속 대기");
        }

        System.out.println("결과 = " + row);
        assertNotNull(row, "3분 안에 리포트가 자동 생성되지 않았다");
    }
}
