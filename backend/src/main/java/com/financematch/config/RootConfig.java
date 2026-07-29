package com.financematch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.apache.ibatis.annotations.Mapper;

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
@MapperScan(
        basePackages = "com.financematch",
        annotationClass = Mapper.class
)
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

    /**
     * Flyway — DB 스키마 마이그레이션.
     *
     * <p>Spring Boot 가 아니라 자동 실행이 없으므로 여기서 명시적으로 등록한다. 애플리케이션이 DB 를 쓰기
     * 전에 스키마가 최신이어야 하므로, {@code sqlSessionFactory} 가 이 빈에 {@code @DependsOn} 으로
     * 의존한다.
     *
     * <p>로컬은 기동 시 자동 적용({@code flyway.enabled=true}), 운영은 배포 파이프라인이 {@code
     * ./gradlew flywayMigrate -Pprod} 로 적용한다(기본 false). 인스턴스가 여러 개 뜰 때의 경합과 의도치
     * 않은 스키마 변경을 막기 위함이다.
     */
    @Bean
    public Flyway flyway(DataSource dataSource, Environment env) {
        String locations = env.getProperty("flyway.locations", "classpath:db/migration");

        Flyway flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations(locations.split("\\s*,\\s*"))
                        .encoding(StandardCharsets.UTF_8)
                        .cleanDisabled(true)
                        .validateOnMigrate(true)
                        .load();

        if (env.getProperty("flyway.enabled", Boolean.class, true)) {
            MigrateResult result = flyway.migrate();
            // 적용된 게 없으면 targetSchemaVersion 이 null 이라 기존 버전을 그대로 쓴다.
            String version =
                    result.targetSchemaVersion != null
                            ? result.targetSchemaVersion
                            : result.initialSchemaVersion;
            log.info(
                    "Flyway 마이그레이션 완료 — 적용 {}건, 스키마 버전 {}",
                    result.migrationsExecuted,
                    version);
        } else {
            log.info("Flyway 자동 마이그레이션 비활성(flyway.enabled=false) — 배포 파이프라인이 적용한다.");
        }
        return flyway;
    }

    @Bean
    @DependsOn("flyway")
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        // 도메인 VO 타입 별칭 (하위 패키지 전체)
        factory.setTypeAliasesPackage("com.financematch");
        // 매퍼 XML 위치. 도메인 매퍼는 resources/mappers/ 아래에 둔다.
        // classpath*: 는 mappers/ 가 아직 없어도 예외 없이 빈 배열을 돌려준다
        // (classpath: 는 디렉터리가 없으면 FileNotFoundException).
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mappers/**/*.xml"));

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
