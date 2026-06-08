package dev.gaonstack.gaonclimatecore.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class JwtAuthInterceptor(
    private val jwtProvider: JwtProvider,
) : HandlerInterceptor {

    // @JwtAuth 가 붙은 메소드에 한해 JWT 액세스토큰을 검증하고, 검증된 사용자 정보를 request attribute 에 저장
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        if (!handler.hasMethodAnnotation(JwtAuth::class.java)) return true

        val userId = jwtProvider.validateAndGetUserId(request.getHeader("Authorization"))
        request.setAttribute(AuthRequestAttributes.JWT_USER, AuthenticatedJwtUser(userId))
        return true
    }
}
