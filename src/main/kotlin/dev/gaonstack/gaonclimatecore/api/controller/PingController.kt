package dev.gaonstack.gaonclimatecore.api.controller

import dev.gaonstack.gaonclimatecore.api.dto.PingResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/ping")
class PingController {
    // 통신 테스트: 인증 없이 호출 가능하며 서버 응답 여부 확인용으로 pong 메시지와 응답 시각 반환
    @GetMapping
    fun ping(): PingResponse =
        PingResponse(
            message = "pong",
            timestamp = LocalDateTime.now(),
        )
}
