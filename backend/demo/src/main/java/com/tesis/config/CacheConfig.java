package com.tesis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                cache("estadisticasMatricula", Duration.ofMinutes(5)),
                cache("catalogos", Duration.ofMinutes(5)),
                cache("calendarioMatricula", Duration.ofMinutes(1)),
                cache("blogCategoriasPublicas", Duration.ofMinutes(1)),
                cache("blogPublicacionesPublicas", Duration.ofMinutes(1)),
                cache("blogPublicacionPublica", Duration.ofMinutes(1))
        ));
        return cacheManager;
    }

    private CaffeineCache cache(String name, Duration ttl) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(1_000)
                .build());
    }
}