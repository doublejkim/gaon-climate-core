package dev.gaonstack.gaonclimatecore.repository

import dev.gaonstack.gaonclimatecore.domain.AdminUser
import org.springframework.data.jpa.repository.JpaRepository

interface AdminUserRepository : JpaRepository<AdminUser, Long> {
    fun findByEmail(email: String): AdminUser?
}
