package dev.gaonstack.gaonclimatecore.api.response

import org.springframework.http.HttpStatus

// 비즈니스 상황을 식별하는 에러 코드. enum 이름이 곧 응답의 code 값으로 내려간다.
// 클라이언트는 HTTP 상태(분류)와 code(구체 상황)를 함께 보고 분기할 수 있다.
enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 인증 / 자격 증명
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // 계정 상태
    ACCOUNT_PENDING(HttpStatus.FORBIDDEN, "승인 대기 중인 계정입니다."),
    ACCOUNT_INACTIVE(HttpStatus.UNAUTHORIZED, "비활성 계정입니다."),

    // 액세스 토큰
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 액세스토큰입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스토큰입니다."),

    // 리프레시 토큰
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 리프레쉬 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레쉬 토큰입니다."),

    // 디바이스 클레임 코드
    INVALID_CLAIM_CODE(HttpStatus.NOT_FOUND, "유효하지 않은 클레임 코드입니다."),
    CLAIM_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "만료된 클레임 코드입니다."),
    CLAIM_CODE_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용된 클레임 코드입니다."),
}
