package dev.gaonstack.gaonclimatecore.service

import java.time.Duration

// 디바이스 클레임 코드 저장소(포트). 발급(웹)과 사용(디바이스)이 서로 다른 요청이라
// 코드를 단기 보관해야 한다. 구현은 in-process 캐시(Caffeine)이며,
// 다중 인스턴스 전환 시 동일 인터페이스로 Redis 구현체만 추가하면 된다.
interface ClaimCodeStore {
    // 코드를 ttl 동안 보관한다. 만료되면 자동 소멸한다.
    fun issue(code: String, userId: Long, ttl: Duration)

    // 코드 존재 여부(고유값 생성 시 충돌 확인용)
    fun exists(code: String): Boolean

    // 코드를 1회용으로 사용한다: 조회와 동시에 소멸시키며, 없거나 만료됐으면 null
    fun consume(code: String): Long?
}
