package dev.gaonstack.gaonclimatecore.repository

import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.gaonstack.gaonclimatecore.domain.Device
import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.domain.UserApiKey
import org.springframework.stereotype.Repository

@Repository
class AdminUserLookupRepository(
    private val queryFactory: JPAQueryFactory,
) {
    private val user = PathBuilder(User::class.java, "user")
    private val device = PathBuilder(Device::class.java, "device")
    private val apiKey = PathBuilder(UserApiKey::class.java, "apiKey")

    fun findUser(userId: Long?, email: String?): User? {
        val query = queryFactory
            .selectFrom(user)

        if (userId != null) {
            query.where(user.getNumber("id", Long::class.java).eq(userId))
        } else {
            query.where(user.getString("email").eq(email))
        }

        return query.fetchOne()
    }

    fun findDevices(userId: Long): List<Device> =
        queryFactory
            .selectFrom(device)
            .where(device.get("user", User::class.java).getNumber("id", Long::class.java).eq(userId))
            .orderBy(device.getNumber("id", Long::class.java).asc())
            .fetch()

    fun findApiKeys(userId: Long): List<UserApiKey> =
        queryFactory
            .selectFrom(apiKey)
            .where(apiKey.get("user", User::class.java).getNumber("id", Long::class.java).eq(userId))
            .orderBy(apiKey.getNumber("id", Long::class.java).asc())
            .fetch()
}
