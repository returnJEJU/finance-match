package com.financematch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 서블릿 컨텍스트 설정: MVC, 컨트롤러, JSON 변환, CORS.
 *
 * <p>컨트롤러({@code @RestController})와 예외 처리기({@code @RestControllerAdvice})만 스캔한다.
 */
@Configuration
@EnableWebMvc
@ComponentScan(
        basePackages = "com.financematch",
        useDefaultFilters = false,
        includeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = {RestController.class, RestControllerAdvice.class}))
public class WebConfig implements WebMvcConfigurer {

    private final Environment env;

    public WebConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // LocalDateTime 등 java.time 타입을 ISO-8601 문자열로 직렬화 (FE Zod 스키마와 계약 일치)
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper()));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 개발 중 Vite dev server(5173) 호출 허용. 운영 오리진은 prod 프로퍼티로 지정.
        String origins = env.getProperty("cors.allowed-origins", "http://localhost:5173");

        registry.addMapping("/**")
                .allowedOrigins(origins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
