package com.financematch.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.mock.env.MockEnvironment;

/**
 * 환경에 따라 갈리는 설정 분기 검증.
 *
 * <p>여기서 갈리는 값들은 <b>로컬에서는 절대 타지 않는 운영 쪽 경로</b>다. 로컬은 Flyway 자동 적용이
 * 켜져 있고 Redis 비밀번호가 비어 있어서, 반대쪽 분기는 배포해봐야 처음 실행된다. 배포 당일에 처음
 * 도는 코드를 남겨두지 않으려고 여기서 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class ConfigBeanTest {

    @Mock private DataSource dataSource;

    /**
     * 운영은 {@code flyway.enabled=false} 다. 인스턴스가 여러 개 뜰 때 서로 마이그레이션을 실행해
     * 경합하는 것을 막기 위함이며, 스키마 적용은 배포 파이프라인이 담당한다.
     *
     * <p>이 분기가 깨져 운영에서 migrate() 가 돌면 <b>배포와 동시에 스키마가 바뀐다.</b>
     */
    @Test
    void 자동_마이그레이션이_꺼져_있으면_DB_에_손대지_않는다() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("flyway.enabled", "false");

        assertNotNull(new RootConfig().flyway(dataSource, env));

        // migrate() 를 부르면 DataSource 에서 커넥션을 가져간다. 아무 호출도 없어야 한다.
        verifyNoInteractions(dataSource);
    }

    @Test
    void Redis_비밀번호가_있으면_설정에_담는다() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("redis.host", "redis.internal");
        env.setProperty("redis.port", "6380");
        env.setProperty("redis.password", "s3cret");

        RedisConnectionFactory factory = new RedisConfig(env).redisConnectionFactory();

        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) factory;
        assertEquals("redis.internal", lettuce.getHostName());
        assertEquals(6380, lettuce.getPort());
        assertEquals("s3cret", lettuce.getPassword());
    }

    /** 로컬은 비밀번호 없이 뜬다. 빈 문자열을 그대로 넣으면 인증을 시도해 접속이 실패한다. */
    @Test
    void Redis_비밀번호가_비어_있으면_설정하지_않는다() {
        MockEnvironment env = new MockEnvironment();

        LettuceConnectionFactory lettuce =
                (LettuceConnectionFactory) new RedisConfig(env).redisConnectionFactory();

        assertEquals("localhost", lettuce.getHostName());
        assertEquals(6379, lettuce.getPort());
        assertEquals(null, lettuce.getPassword());
    }
}
