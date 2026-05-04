package dev.gaonstack.gaonclimatecore

import dev.gaonstack.gaonclimatecore.domain.User
import dev.gaonstack.gaonclimatecore.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class GaonClimateCoreApplicationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val userRepository: UserRepository,
) {

    @Test
    fun contextLoads() {
    }

    @Test
    fun `admin device creation response is wrapped`() {
        userRepository.save(
            User(
                email = "test@example.com",
                name = "테스트 사용자",
            )
        )

        mockMvc.post("/admin/devices") {
            header("X-Admin-Token", "test-admin-token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@example.com",
                  "device_key": "abcdevice",
                  "location_name": "우리집"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.message") { doesNotExist() }
                jsonPath("$.data.device.device_key") { value("abcdevice") }
                jsonPath("$.data.device.name") { value("TEST_DEVICE") }
            }
    }

    @Test
    fun `error response is wrapped`() {
        mockMvc.post("/admin/devices") {
            header("X-Admin-Token", "wrong-token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "test@example.com",
                  "device_key": "error-device"
                }
            """.trimIndent()
        }
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("UNAUTHORIZED") }
                jsonPath("$.message") { value("관리자 토큰이 유효하지 않습니다.") }
                jsonPath("$.data") { doesNotExist() }
            }
    }
}
