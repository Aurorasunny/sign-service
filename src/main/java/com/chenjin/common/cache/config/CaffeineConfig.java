package com.chenjin.common.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine配置
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 11:42
 **/
@Configuration
public class CaffeineConfig {

    /**
     * 将caffeine配置注入spring-cache进行管理
     */
    @Bean
    public CacheManager cacheManager() {
        Caffeine<Object, Object> cache = Caffeine.newBuilder()
                .initialCapacity(5)
                .recordStats()
                .maximumSize(10);
        CaffeineCacheManager manager = new CaffeineCacheManager("caffeine");
        manager.setCaffeine(cache);
        return manager;
    }

}
