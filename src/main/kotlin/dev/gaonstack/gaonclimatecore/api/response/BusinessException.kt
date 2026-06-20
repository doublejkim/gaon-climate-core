package dev.gaonstack.gaonclimatecore.api.response

// 비즈니스 규칙 위반을 나타내는 예외. ErrorCode 를 운반해 응답의 code/HTTP 상태/메시지를 결정한다.
// message 를 별도로 넘기면 기본 메시지 대신 사용한다(같은 code 로 상황별 안내 문구를 달리할 때).
class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
