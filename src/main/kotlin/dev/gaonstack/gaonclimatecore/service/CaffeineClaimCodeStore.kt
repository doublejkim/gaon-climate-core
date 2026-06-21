package dev.gaonstack.gaonclimatecore.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import org.springframework.stereotype.Component
import java.time.Duration

// ClaimCodeStore 의 in-process 구현. 단일 인스턴스 운영에 적합하며 별도 서버가 필요 없다.
// 코드별 TTL 을 honor 하고, 만료된 항목은 Caffeine 이 자동으로 evict 한다.
@Component
class CaffeineClaimCodeStore : ClaimCodeStore {
    // value: userId, 만료는 코드별 ttl 을 따른다(Expiry)
    private val cache: Cache<String, Entry> = Caffeine.newBuilder()
        .expireAfter(object : Expiry<String, Entry> {
            override fun expireAfterCreate(key: String, value: Entry, currentTime: Long): Long =
                value.ttlNanos

            override fun expireAfterUpdate(
                key: String,
                value: Entry,
                currentTime: Long,
                currentDuration: Long,
            ): Long = value.ttlNanos

            override fun expireAfterRead(
                key: String,
                value: Entry,
                currentTime: Long,
                currentDuration: Long,
            ): Long = currentDuration
        })
        .build()

    override fun issue(code: String, userId: Long, ttl: Duration) {
        cache.put(code, Entry(userId = userId, ttlNanos = ttl.toNanos()))
    }

    override fun exists(code: String): Boolean = cache.getIfPresent(code) != null

    // asMap().remove 는 원자적이라 "조회+소멸"을 락 없이 1회용으로 처리한다(Redis 의 GETDEL 과 대응)
    override fun consume(code: String): Long? = cache.asMap().remove(code)?.userId

    private data class Entry(val userId: Long, val ttlNanos: Long)
}
