package com.flatwhite.template.coroutine.application.presentation

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.flatwhite.template.coroutine.application.outbound.CartWebClient
import com.flatwhite.template.coroutine.application.outbound.UserWebClient
import com.flatwhite.template.coroutine.application.presentation.OrderController.Companion.BASE_URL
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RequestMapping(path = [BASE_URL])
@RestController
class OrderController(
    private val cartWebClient: CartWebClient,
    private val userWebClient: UserWebClient,
) {
    companion object {
        const val BASE_URL = "/api/orders"
        private val testUserId = "USER0001"
        private val testCartId = "CART0001"
    }

    @PostMapping("")
    suspend fun createOrder(
        @RequestBody request: OrderCreateRequest,
    ): Order =
        coroutineScope {
            val cartItemDeferred = async { cartWebClient.getCartItem(cartId = request.cartId) }
            val userDeferred = async { userWebClient.getUser(userId = request.userId) }

            val user =
                userDeferred
                    .await()
                    .also {
                        log.info { "user : $it" }
                    }

            val cart =
                cartItemDeferred
                    .await()
                    .also {
                        log.info { "cartItem : $it" }
                    }

            // TODO create order
            // val nearestStockDeferred = async { getNearestStock(address = userAddress, items = cartItems) }
            // val nearestStockId = nearestStockDeferred.await()
            // orderService.reserveStock(address = userAddress, stockId = nearestStockId)
            return@coroutineScope Order(
                userId = user!!.userId,
                userName = user.name,
                userAddress = user.address,
                cartId = cart!!.id,
                cartName = cart.name,
            )
        }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OrderCreateRequest(
    val userId: String,
    val cartId: String,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Order(
    val userId: String,
    val userName: String,
    val userAddress: String,
    val cartId: String,
    val cartName: String,
)
