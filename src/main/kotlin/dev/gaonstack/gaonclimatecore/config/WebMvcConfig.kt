package dev.gaonstack.gaonclimatecore.config

import dev.gaonstack.gaonclimatecore.auth.AuthenticatedPrincipalArgumentResolver
import dev.gaonstack.gaonclimatecore.auth.JwtAuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authenticatedPrincipalArgumentResolver: AuthenticatedPrincipalArgumentResolver,
    private val jwtAuthInterceptor: JwtAuthInterceptor,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedPrincipalArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(jwtAuthInterceptor)
    }
}
