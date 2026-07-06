package org.chasapi.activityreccomender.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
public class CacheConfiguration {

    @Bean
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();

        manager.setCaches(List.of(
                new CaffeineCache(
                        "rate-limits",
                        Caffeine.newBuilder()
                                .maximumSize(10000)
                                .expireAfterWrite(Duration.ofHours(1))
                                .build()
                ),
                new CaffeineCache(
                        "locationCache",
                        Caffeine.newBuilder()
                                .maximumSize(10000)
                                .expireAfterAccess(Duration.ofHours(1))
                                .build()
                ),
                new CaffeineCache(
                        "AI_Response",
                        Caffeine.newBuilder()
                                .maximumSize(10000)
                                .expireAfterAccess(Duration.ofHours(15))
                                .build()
                ),
                new CaffeineCache(
                        "WeatherData",
                        Caffeine.newBuilder()
                                .maximumSize(10000)
                                .expireAfterAccess(Duration.ofMinutes(15))
                                .build()
                )

        ));

        return manager;
    }
}