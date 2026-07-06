package org.chasapi.activityreccomender.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RateLimitService {

    private final Cache cache;
    private final BucketFactory bucketFactory;

    public RateLimitService(CacheManager cacheManager,
                            BucketFactory bucketFactory) {

        this.cache = Objects.requireNonNull(
                cacheManager.getCache("rate-limits"));

        this.bucketFactory = bucketFactory;
    }

    public ConsumptionProbe consume(String key) {
        Bucket bucket = cache.get(key, () -> bucketFactory.create(key));
        return bucket.tryConsumeAndReturnRemaining(1);
    }
}
