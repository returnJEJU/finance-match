package com.financematch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 루트 컨텍스트 설정: DataSource, MyBatis, 트랜잭션, 서비스 빈.
 *
 * <p>과정 규정상 Spring Boot 가 아니므로 자동 설정이 없다. 필요한 빈은 모두 여기서 명시적으로 등록한다.
 *
 * <p>학습 프로젝트는 {@code @MapperScan}/{@code @ComponentScan} 에 도메인을 하나씩 나열했지만, 우리는
 * 도메인이 계속 추가되므로 {@code com.financematch} 하위를 전체 스캔한다. 팀원이 자기 도메인 패키지를 추가하면
 * config 를 수정하지 않아도 자동으로 잡힌다.
 */
@Configuration
@PropertySource("classpath:application-${spring.profiles.active:local}.properties")
@MapperScan(basePackages = "com.financematch")
@ComponentScan(
        basePackages = "com.financematch",
        // 컨트롤러와 예외 처리기는 서블릿 컨텍스트(WebConfig)에서 스캔하므로 루트에서는 제외한다.
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = {Controller.class, RestControllerAdvice.class}))
@EnableTransactionManagement
@Slf4j
public class RootConfig {

    /**
     * {@code @Value} 의 {@code ${...}} 치환기. {@code @PropertySource} 로 읽은 값을 {@code @Value} 에서
     * 쓰려면 필요하다. BeanFactoryPostProcessor 라서 반드시 static 으로 선언한다.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("finance-match-pool");
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        // 도메인 VO 타입 별칭 (하위 패키지 전체)
        factory.setTypeAliasesPackage("com.financematch");
        // 매퍼 XML 위치. 도메인 매퍼는 resources/mappers/ 아래에 둔다.
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mappers/**/*.xml"));

        org.apache.ibatis.session.Configuration mybatisConfig =
                new org.apache.ibatis.session.Configuration();
        // DB 의 snake_case 컬럼을 자바의 camelCase 필드로 자동 매핑
        mybatisConfig.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(mybatisConfig);

        return factory.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
