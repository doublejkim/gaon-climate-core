package dev.gaonstack.gaonclimatecore.auth

import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.server.ResponseStatusException

@Component
class AuthenticatedPrincipalArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == AuthenticatedUser::class.java ||
            parameter.parameterType == AuthenticatedApiKey::class.java ||
            parameter.parameterType == AuthenticatedJwtUser::class.java ||
            parameter.parameterType == AuthenticatedAdmin::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val attributeName = when (parameter.parameterType) {
            AuthenticatedUser::class.java -> AuthRequestAttributes.USER
            AuthenticatedApiKey::class.java -> AuthRequestAttributes.API_KEY
            AuthenticatedJwtUser::class.java -> AuthRequestAttributes.JWT_USER
            AuthenticatedAdmin::class.java -> AuthRequestAttributes.ADMIN
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.")
        }

        return webRequest.getAttribute(attributeName, RequestAttributes.SCOPE_REQUEST)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.")
    }
}
