package org.chasapi.activityreccomender.config;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    CacheManager syncCacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();

        manager.setCaches(List.of(
                new CaffeineCache(
                        "rate-limits",
                        Caffeine.newBuilder()
                                .maximumSize(10000)
                                .expireAfterWrite(Duration.ofHours(1))
                                .build()
                )
        ));

        return manager;
    }

    @Bean
    @Primary
    public CaffeineCacheManager asyncCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.setAsyncCacheMode(true);

        manager.registerCustomCache(
                "locationData",
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterAccess(Duration.ofHours(1))
                        .buildAsync()
        );

        manager.registerCustomCache(
                "WeatherData",
                Caffeine.newBuilder()
                        .maximumSize(5000)
                        .expireAfterWrite(Duration.ofMinutes(15))
                        .buildAsync()
        );

        manager.registerCustomCache(
                "AI_Response",
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterAccess(Duration.ofMinutes(15))
                        .build()
        );

        return manager;
    }
}


