package dev.gaonstack.gaonclimatecore.validation

import dev.gaonstack.gaonclimatecore.api.dto.AdminUserLookupRequest
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AdminUserLookupValidator::class])
annotation class ValidAdminUserLookup(
    val message: String = "user_id 또는 email 중 하나는 필요하며, email은 올바른 형식이어야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out jakarta.validation.Payload>> = [],
)

class AdminUserLookupValidator : ConstraintValidator<ValidAdminUserLookup, AdminUserLookupRequest> {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    override fun isValid(
        value: AdminUserLookupRequest?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) {
            return false
        }

        val hasUserId = value.userId != null
        val email = value.email?.trim()
        val hasEmail = !email.isNullOrBlank()

        if (!hasUserId && !hasEmail) {
            return false
        }

        return !hasEmail || emailRegex.matches(email)
    }
}
