package com.flatwhite.template.coroutine.mock.presentation

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.flatwhite.template.coroutine.base.error.ErrorResponse
import com.flatwhite.template.coroutine.mock.presentation.TargetMockController.Companion.BASE_URL
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 *  target microservice side controller
 */
@RequestMapping(path = [BASE_URL])
@RestController
class TargetMockController {
    companion object {
        const val BASE_URL = "/api"
    }

    @GetMapping("/carts/{cart-id}")
    fun getCartItem(
        @PathVariable(value = "cart-id") cartId: String,
    ): CartResponse {
        Thread.sleep(4000L)
        return CartResponse(
            id = cartId,
            name = "($cartId)의 카트",
        )
    }

    @GetMapping("/users/{user-id}")
    fun getUser(
        @PathVariable(value = "user-id") userId: String,
    ): UserResponse {
        Thread.sleep(2000L)
        return UserResponse(
            userId = userId,
            name = "($userId)인 사람의 이름",
            address = "($userId)인 사람의 주소",
        )
    }

    @GetMapping("/errors/{status-code}")
    fun getErrorResponse(
        @PathVariable(value = "status-code") statusCode: String,
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity<ErrorResponse>(
            ErrorResponse(
                errorCode = statusCode,
                errorMessage = "($statusCode)의 에러응답",
            ),
            HttpStatusCode.valueOf(statusCode.toInt()),
        )
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CartResponse(
    val id: String,
    val name: String,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UserResponse(
    val userId: String,
    val name: String,
    val address: String,
)
