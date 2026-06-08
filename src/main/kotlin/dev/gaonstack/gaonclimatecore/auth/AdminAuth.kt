package dev.gaonstack.gaonclimatecore.auth

// 이 어노테이션이 붙은 컨트롤러 메소드는 AdminJwtAuthInterceptor 에서 관리자 JWT 를 검증함
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class AdminAuth
