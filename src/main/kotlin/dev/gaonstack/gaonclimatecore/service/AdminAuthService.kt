package dev.gaonstack.gaonclimatecore.service

import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateRequest
import dev.gaonstack.gaonclimatecore.api.dto.AdminCreateResponse
import dev.gaonstack.gaonclimatecore.api.dto.AdminLoginRequest
import dev.gaonstack.gaonclimatecore.api.dto.AdminLoginResponse
import dev.gaonstack.gaonclimatecore.auth.AdminJwtProvider
import dev.gaonstack.gaonclimatecore.auth.AuthenticatedAdmin
import dev.gaonstack.gaonclimatecore.domain.AdminUser
import dev.gaonstack.gaonclimatecore.repository.AdminUserRepository
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class AdminAuthService(
    private val adminUserRepository: AdminUserRepository,
    private val adminJwtProvider: AdminJwtProvider,
    // 유저 비밀번호와 동일한 pepper 롤링 정책을 재사용
    @Value("\${app.password.peppers}")
    private val peppersRaw: String,
) {
    private val peppers: List<String> by lazy { peppersRaw.split(",").map { it.trim() } }

    // 3.3.1. 관리자 계정 생성: 부트스트랩 토큰(X-Admin-Token)으로 인증된 호출에서 신규 관리자 등록
    @Transactional
    fun createAdmin(request: AdminCreateRequest): AdminCreateResponse {
        val email = request.email?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email 값이 필요합니다.")
        val password = request.password?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password 값이 필요합니다.")
        val role = when (val raw = request.role?.trim()?.takeIf { it.isNotBlank() }) {
            null -> AdminUser.ROLE_ADMIN
            AdminUser.ROLE_ADMIN, AdminUser.ROLE_SUPER_ADMIN -> raw
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 role 값입니다.")
        }

        if (adminUserRepository.findByEmail(email) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 관리자 계정입니다.")
        }

        val pepperIndex = peppers.size - 1
        val pepper = peppers[pepperIndex]
        val passwordHash = BCrypt.hashpw(password + pepper, BCrypt.gensalt())
        val now = LocalDateTime.now()

        val admin = adminUserRepository.save(
            AdminUser(
                email = email,
                password = passwordHash,
                passwordKeyIndex = pepperIndex,
                role = role,
                createdAt = now,
                updatedAt = now,
            )
        )

        return AdminCreateResponse(
            id = admin.id ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "관리자 정보가 올바르지 않습니다."),
            email = admin.email,
            role = admin.role,
            status = admin.status,
            createdAt = admin.createdAt,
        )
    }

    // 3.0.1. 관리자 로그인: email + password 검증 후 관리자 JWT 액세스토큰 발급
    @Transactional(readOnly = true)
    fun login(request: AdminLoginRequest): AdminLoginResponse {
        val email = request.email?.trim()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "email 값이 필요합니다.")
        val password = request.password?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password 값이 필요합니다.")

        val admin = adminUserRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.")

        if (admin.status != AdminUser.STATUS_ACTIVE) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "비활성 관리자 계정입니다.")
        }

        val pepper = peppers.getOrNull(admin.passwordKeyIndex)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "인증 설정 오류입니다.")

        if (!BCrypt.checkpw(password + pepper, admin.password)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        val adminId = admin.id
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "관리자 정보가 올바르지 않습니다.")

        return AdminLoginResponse(accessToken = adminJwtProvider.createAccessToken(adminId, admin.role))
    }

    // 관리자 JWT 검증 후 활성 관리자 여부를 DB 에서 재확인 — 상태 비활성화 시 즉시 차단(토큰 무효화)
    @Transactional(readOnly = true)
    fun requireAdmin(authorization: String?): AuthenticatedAdmin {
        val adminId = adminJwtProvider.validateAndGetAdminId(authorization)
        val admin = adminUserRepository.findById(adminId).orElse(null)

        if (admin == null || admin.status != AdminUser.STATUS_ACTIVE) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 관리자입니다.")
        }

        return AuthenticatedAdmin(
            adminId = admin.id ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 정보가 올바르지 않습니다."),
            role = admin.role,
        )
    }
}
