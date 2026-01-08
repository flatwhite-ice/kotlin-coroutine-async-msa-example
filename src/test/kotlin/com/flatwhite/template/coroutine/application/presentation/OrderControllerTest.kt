package com.flatwhite.template.coroutine.application.presentation

import com.flatwhite.template.coroutine.application.outbound.CartResponse
import com.flatwhite.template.coroutine.application.outbound.CartWebClient
import com.flatwhite.template.coroutine.application.outbound.UserResponse
import com.flatwhite.template.coroutine.application.outbound.UserWebClient
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(OrderController::class)
class OrderControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var cartWebClient: CartWebClient

    @MockkBean
    private lateinit var userWebClient: UserWebClient

    @Test
    fun `createOrder should return order when request is valid`() {
        // Given
        val userId = "USER0001"
        val cartId = "CART0001"
        val userResponse = UserResponse(userId = userId, name = "John Doe", address = "Seoul")
        val cartResponse = CartResponse(id = cartId, name = "My Cart")

        coEvery { userWebClient.getUser(userId) } returns userResponse
        coEvery { cartWebClient.getCartItem(cartId) } returns cartResponse

        val requestBody = """
            {
                "user_id": "$userId",
                "cart_id": "$cartId"
            }
        """.trimIndent()

        // When & Then
        webTestClient.post()
            .uri(OrderController.BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.user_id").isEqualTo(userId)
            .jsonPath("$.user_name").isEqualTo("John Doe")
            .jsonPath("$.user_address").isEqualTo("Seoul")
            .jsonPath("$.cart_id").isEqualTo(cartId)
            .jsonPath("$.cart_name").isEqualTo("My Cart")
    }

    @Test
    fun `createOrder should return error when user service fails`() {
        // Given
        val userId = "USER_ERROR"
        val cartId = "CART0001"
        
        coEvery { userWebClient.getUser(userId) } throws RuntimeException("User service down")
        coEvery { cartWebClient.getCartItem(cartId) } returns CartResponse(id = cartId, name = "My Cart")

        val requestBody = """
            {
                "user_id": "$userId",
                "cart_id": "$cartId"
            }
        """.trimIndent()

        // When & Then
        webTestClient.post()
            .uri(OrderController.BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().is5xxServerError
    }
}
