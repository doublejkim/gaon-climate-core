package dev.gaonstack.gaonclimatecore.auth

import dev.gaonstack.gaonclimatecore.api.response.ApiResponse
import dev.gaonstack.gaonclimatecore.api.response.apiErrorCode
import dev.gaonstack.gaonclimatecore.service.AuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper

@Component
class AuthenticationFilter(
    private val authService: AuthService,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            when {
                request.isDeviceRegistrationRequest() -> {
                    val user = authService.requireUser(request.getHeader("Authorization"))
                    request.setAttribute(
                        AuthRequestAttributes.USER,
                        AuthenticatedUser(
                            userId = user.id ?: throw ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "사용자 정보가 올바르지 않습니다.",
                            ),
                        ),
                    )
                }

                request.isClimateMeasurementRequest() -> {
                    val apiKey = authService.requireApiKey(request.getHeader("Authorization"))
                    request.setAttribute(AuthRequestAttributes.API_KEY, apiKey)
                }

                request.isClimateReadRequest() -> {
                    val apiKey = authService.requireApiKey(request.getHeader("Authorization"))
                    request.setAttribute(AuthRequestAttributes.API_KEY, apiKey)
                }
            }

            filterChain.doFilter(request, response)
        } catch (exception: ResponseStatusException) {
            response.status = exception.statusCode.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = Charsets.UTF_8.name()
            objectMapper.writeValue(
                response.writer,
                ApiResponse.error(apiErrorCode(exception.statusCode), exception.reason),
            )
        }
    }

    private fun HttpServletRequest.isDeviceRegistrationRequest(): Boolean =
        method.equals("POST", ignoreCase = true) && requestURI == "/devices/register"

    private fun HttpServletRequest.isClimateMeasurementRequest(): Boolean =
        method.equals("POST", ignoreCase = true) && requestURI.startsWith("/climate/")

    private fun HttpServletRequest.isClimateReadRequest(): Boolean =
        method.equals("GET", ignoreCase = true) && requestURI.startsWith("/climate/")
}
