package dev.gaonstack.gaonclimatecore.auth

// 이 어노테이션이 붙은 컨트롤러 메소드는 JwtAuthInterceptor 에서 JWT 액세스토큰을 검증함
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class JwtAuth
