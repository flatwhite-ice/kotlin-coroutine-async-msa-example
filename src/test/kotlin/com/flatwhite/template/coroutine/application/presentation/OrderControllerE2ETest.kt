package com.flatwhite.template.coroutine.application.presentation

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
class OrderControllerE2ETest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `createOrder E2E test - should process in parallel and return result`() {
        // Given
        val userId = "USER123"
        val cartId = "CART456"
        val requestBody = """
            {
                "user_id": "$userId",
                "cart_id": "$cartId"
            }
        """.trimIndent()

        // When
        val duration = measureTimeMillis {
            webTestClient.post()
                .uri(OrderController.BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.user_id").isEqualTo(userId)
                .jsonPath("$.cart_id").isEqualTo(cartId)
                .jsonPath("$.user_name").exists()
                .jsonPath("$.cart_name").exists()
        }

        // Then
        println("[DEBUG_LOG] Total execution time: ${duration}ms")
        
        // TargetMockController에서 Cart(4s), User(2s) sleep이 있음.
        // 병렬 처리 시 약 4s 대 (네트워크 오버헤드 포함 5s 미만 예상)
        // 순차 처리 시 약 6s 대
        // 따라서 5.5s 미만이면 병렬 처리가 정상 동작한 것으로 간주
        assertTrue(duration < 5500, "Execution took too long ($duration ms), parallel processing might not be working")
        assertTrue(duration >= 4000, "Execution was too fast ($duration ms), expected at least 4000ms delay")
    }
}
