package dev.gaonstack.gaonclimatecore.config

import dev.gaonstack.gaonclimatecore.auth.AdminJwtAuthInterceptor
import dev.gaonstack.gaonclimatecore.auth.AuthenticatedPrincipalArgumentResolver
import dev.gaonstack.gaonclimatecore.auth.JwtAuthInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authenticatedPrincipalArgumentResolver: AuthenticatedPrincipalArgumentResolver,
    private val jwtAuthInterceptor: JwtAuthInterceptor,
    private val adminJwtAuthInterceptor: AdminJwtAuthInterceptor,
    @Value("\${app.cors.allowed-origins}")
    private val allowedOrigins: String,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedPrincipalArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(jwtAuthInterceptor)
        registry.addInterceptor(adminJwtAuthInterceptor)
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*allowedOrigins.split(",").map { it.trim() }.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}
