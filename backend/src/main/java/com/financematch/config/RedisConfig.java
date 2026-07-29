package com.financematch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정.
 *
 * <p>용도: refresh 토큰 저장, 로그아웃 블랙리스트, 기준값·상품 목록 캐시.
 *
 * <p>클라이언트는 Spring Data Redis 의 기본인 Lettuce 를 사용한다.
 */
@Configuration
public class RedisConfig {

    private final Environment env;

    public RedisConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(
                        env.getProperty("redis.host", "localhost"),
                        env.getProperty("redis.port", Integer.class, 6379));

        String password = env.getProperty("redis.password", "");
        if (!password.isBlank()) {
            config.setPassword(password);
        }

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
