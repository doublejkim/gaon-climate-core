package dev.gaonstack.gaonclimatecore.auth

import dev.gaonstack.gaonclimatecore.service.AdminAuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminJwtAuthInterceptor(
    private val adminAuthService: AdminAuthService,
) : HandlerInterceptor {

    // @AdminAuth 가 붙은 메소드에 한해 관리자 JWT 를 검증하고, 검증된 관리자 정보를 request attribute 에 저장
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        if (!handler.hasMethodAnnotation(AdminAuth::class.java)) return true

        val admin = adminAuthService.requireAdmin(request.getHeader("Authorization"))
        request.setAttribute(AuthRequestAttributes.ADMIN, admin)
        return true
    }
}
