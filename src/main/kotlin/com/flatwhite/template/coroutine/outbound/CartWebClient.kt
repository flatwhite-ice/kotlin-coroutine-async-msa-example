package com.flatwhite.template.coroutine.outbound

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.flatwhite.template.coroutine.base.exception.HttpResponseException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.RetryRegistry
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Component
class CartWebClient(
    @Qualifier("defaultWebClientBuilder")
    private val defaultWebClientBuilder: WebClient.Builder,
    private val defaultCircuitBreakerRegistry: CircuitBreakerRegistry,
    private val defaultRetryRegistry: RetryRegistry,
) {
    companion object {
        const val CART_CLIENT = "cart-client"
        const val CART_CLIENT_CIRCUIT_BREAKER = CART_CLIENT
        const val CART_CLIENT_RETRY_REGISTRY = CART_CLIENT
    }

    suspend fun getCartItem(cartId: String) = this.cartItem(cartId = cartId).awaitSingleOrNull()

    fun cartItem(cartId: String): Mono<CartResponse> =
        defaultWebClientBuilder
            .build()
            .get()
            .uri("/api/carts/$cartId")
            .headers { } // TODO header setting
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                response.bodyToMono(CartResponse::class.java).map { body ->
                    log.error { "[response code : ${response.statusCode()}] response : $body" }

                    when (response.statusCode()) {
                        HttpStatus.NOT_FOUND -> {
                            HttpResponseException(
                                statusCode = response.statusCode().value(),
                                httpStatus = HttpStatus.NOT_FOUND,
                                errorCode = "NOT_FOUND",
                                errorMessage = "not found",
                            )
                        }
                        else -> {
                            HttpResponseException(
                                statusCode = response.statusCode().value(),
                                httpStatus = HttpStatus.BAD_REQUEST,
                                errorCode = "BAD_REQUEST",
                                errorMessage = "bad request",
                            )
                        }
                    }
                }
            }.onStatus({ status -> status.is5xxServerError }) { response ->
                response.bodyToMono(CartResponse::class.java).map { body ->
                    log.error { "[response code : ${response.statusCode()}] response : $body" }

                    HttpResponseException(
                        statusCode = 500,
                        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                        errorCode = "INTERNAL_SERVER_ERROR",
                        errorMessage = "internal server error",
                    )
                }
            }.bodyToMono(CartResponse::class.java)
            .transform(CircuitBreakerOperator.of(defaultCircuitBreakerRegistry.circuitBreaker(CART_CLIENT_CIRCUIT_BREAKER)))
            .transform(RetryOperator.of(defaultRetryRegistry.retry(CART_CLIENT_RETRY_REGISTRY)))
            .onErrorMap {
                when (it) {
                    is HttpResponseException -> throw it
                    else -> {
                        throw HttpResponseException(
                            statusCode = 500,
                            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                            errorCode = "UNKNOWN_ERROR",
                            errorMessage = "unknown error",
                        )
                    }
                }
            }
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CartResponse(
    val id: String,
    val name: String,
)
