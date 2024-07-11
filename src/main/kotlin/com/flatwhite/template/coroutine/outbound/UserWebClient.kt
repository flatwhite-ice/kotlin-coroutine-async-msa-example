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
class UserWebClient(
    @Qualifier("defaultWebClientBuilder")
    private val defaultWebClientBuilder: WebClient.Builder,
    private val defaultCircuitBreakerRegistry: CircuitBreakerRegistry,
    private val defaultRetryRegistry: RetryRegistry,
) {
    companion object {
        const val USER_CLIENT = "user-client"
        const val USER_CLIENT_CIRCUIT_BREAKER = USER_CLIENT
        const val USER_CLIENT_RETRY_REGISTRY = USER_CLIENT
    }

    suspend fun getUser(userId: String) = this.requestUser(userId = userId).awaitSingleOrNull()

    fun requestUser(userId: String): Mono<UserResponse> =
        defaultWebClientBuilder
            .build()
            .get()
            .uri("/api/users/$userId")
            .headers { } // TODO header setting
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                response.bodyToMono(UserResponse::class.java).map { body ->
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
                response.bodyToMono(UserResponse::class.java).map { body ->
                    log.error { "[response code : ${response.statusCode()}] response : $body" }

                    HttpResponseException(
                        statusCode = 500,
                        httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                        errorCode = "INTERNAL_SERVER_ERROR",
                        errorMessage = "internal server error",
                    )
                }
            }.bodyToMono(UserResponse::class.java)
            .transform(CircuitBreakerOperator.of(defaultCircuitBreakerRegistry.circuitBreaker(USER_CLIENT_CIRCUIT_BREAKER)))
            .transform(RetryOperator.of(defaultRetryRegistry.retry(USER_CLIENT_RETRY_REGISTRY)))
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
data class UserResponse(
    val userId: String,
    val name: String,
    val address: String,
)
