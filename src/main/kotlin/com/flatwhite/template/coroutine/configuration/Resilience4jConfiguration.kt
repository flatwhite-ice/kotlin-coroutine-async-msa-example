package com.flatwhite.template.coroutine.configuration

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.TimeoutException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.io.IOException
import java.time.Duration

@Configuration
class Resilience4jConfiguration {
    private val circuitBreakerConfig =
        CircuitBreakerConfig
            .custom()
            .failureRateThreshold(50f)
            .waitDurationInOpenState(Duration.ofMillis(5000L))
            .permittedNumberOfCallsInHalfOpenState(2)
            .minimumNumberOfCalls(2)
            .slidingWindowSize(2)
            .recordExceptions(
                IOException::class.java,
                WebClientRequestException::class.java,
                TimeoutException::class.java,
                java.util.concurrent.TimeoutException::class.java,
                HttpServerErrorException::class.java,
                ConnectTimeoutException::class.java,
                java.lang.IllegalArgumentException::class.java,
                WebClientResponseException::class.java,
                HttpServerErrorException.InternalServerError::class.java,
            ).build()

    @Bean
    fun circuitBreakerRegistry(): CircuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig)

    private val retryConfig: RetryConfig =
        RetryConfig
            .custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            // .retryOnResult { response: HttpServletResponse -> response.status == 500 }
            // .retryOnException { e: Throwable? -> e is WebClientRequestException }
            .retryExceptions(
                IOException::class.java,
                TimeoutException::class.java,
                WebClientResponseException::class.java,
                HttpServerErrorException.InternalServerError::class.java,
            )
            // .ignoreExceptions(IgnoreException::class.java)
            .build()

    @Bean
    fun retryRegistry(): RetryRegistry = RetryRegistry.of(retryConfig)
}
