package com.financematch.config;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * web.xml 을 대체하는 서블릿 초기화 (Servlet 3.0+ 자동 감지).
 *
 * <p>학습 프로젝트와 동일하게 Java Config 방식이다. 모든 API 는 {@code /api} prefix 아래에 매핑된다.
 */
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        // 보안 설정(SecurityConfig)은 다음 단계에서 이 배열에 추가한다.
        return new Class<?>[] {RootConfig.class, RedisConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] {WebConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {"/api/*"};
    }
}
