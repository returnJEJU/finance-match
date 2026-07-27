package com.financematch.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 기동 확인용 엔드포인트.
 *
 * <p>비즈니스 로직도 DB 접근도 없다. <b>응답이 온다는 사실 자체</b>가 정보다. 이 엔드포인트가 응답하면
 * 루트 컨텍스트({@code DataSource}·Flyway·MyBatis)와 서블릿 컨텍스트(컨트롤러 스캔·JSON 변환)가 모두
 * 정상 기동했다는 뜻이다. 빈 생성이 하나라도 실패하면 컨텍스트가 뜨지 않아 여기까지 도달하지 못한다.
 *
 * <p>"API 가 안 된다" 는 상황에서 서버 문제인지 내 코드 문제인지 가르는 기준점으로 쓴다. 배포 후에는
 * 로드밸런서·모니터링의 상태 확인 대상이 되므로 지우지 않는다.
 */
@RestController
public class HealthController {

    // DispatcherServlet 이 /api/* 에 매핑돼 있어 경로에 /api 를 다시 쓰지 않는다.
    // 실제 주소는 /api/health.
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }
}
