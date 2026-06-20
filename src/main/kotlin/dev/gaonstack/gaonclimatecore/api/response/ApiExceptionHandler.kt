package dev.gaonstack.gaonclimatecore.api.response

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.ConstraintViolationException

@RestControllerAdvice
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    // 비즈니스 예외: code 에 ErrorCode 이름(비즈니스 상황 코드)을 그대로 내려준다.
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(exception.errorCode.status)
            .body(ApiResponse.error(exception.errorCode.name, exception.message))

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ResponseEntity<ApiResponse<Nothing>> {
        val statusCode = exception.statusCode
        val message = exception.reason ?: "요청을 처리할 수 없습니다."
        return ResponseEntity
            .status(statusCode)
            .body(ApiResponse.error(apiErrorCode(statusCode), message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        exception: HttpMessageNotReadableException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        // 원본 메시지에는 역직렬화 대상 타입/필드 등 내부 정보가 섞여 있어 클라이언트에 그대로 노출하지 않는다.
        log.warn("요청 본문을 해석할 수 없습니다.", exception)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.name, "요청 본문 형식이 올바르지 않습니다."))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        exception: ConstraintViolationException,
    ): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.name, exception.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val message = exception.bindingResult.allErrors.firstOrNull()?.defaultMessage
            ?: "요청 값이 올바르지 않습니다."

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(HttpStatus.BAD_REQUEST.name, message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ApiResponse<Nothing>> {
        // 내부 예외 메시지(SQL/스키마/스택 등)는 정보 노출 위험이 있어 클라이언트에 내려주지 않는다.
        // 상세 원인은 서버 로그로만 남기고, 클라이언트에는 일반화된 메시지를 반환한다.
        log.error("처리되지 않은 예외가 발생했습니다.", exception)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.name, "서버 내부 오류가 발생했습니다."))
    }
}
