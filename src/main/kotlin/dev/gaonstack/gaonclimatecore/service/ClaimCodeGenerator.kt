package dev.gaonstack.gaonclimatecore.service

import org.springframework.stereotype.Component
import java.security.SecureRandom

// 디바이스 클레임 코드 생성기. 사람이 입력하기 쉽도록 혼동되는 문자(0/O, 1/I/L, U)를 제외한
// 알파벳으로 GAON-XXXX-XXXX 형태(8자리)의 코드를 만든다.
@Component
class ClaimCodeGenerator {
    private val random = SecureRandom()

    fun generate(): String {
        val body = (1..BODY_LENGTH)
            .map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .joinToString("")
        return "$PREFIX-${body.substring(0, 4)}-${body.substring(4, 8)}"
    }

    companion object {
        private const val PREFIX = "GAON"
        private const val BODY_LENGTH = 8
        // 혼동되는 문자 제외: 0,O,1,I,L,U
        private const val ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ23456789"
    }
}
