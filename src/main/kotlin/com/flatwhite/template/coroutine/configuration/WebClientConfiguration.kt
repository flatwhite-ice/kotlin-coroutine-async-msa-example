package com.flatwhite.template.coroutine.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.ReactorResourceFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate

private val log = KotlinLogging.logger {}

@Configuration
class WebClientConfiguration(
    private val reactorResourceFactory: ReactorResourceFactory,
    @Qualifier("snakeCaseWebObjectMapper")
    private val objectMapper: ObjectMapper,
    @Value("\${webclient.default-client.base-url}")
    private val baseUrl: String,
    @Value("\${webclient.default-client.proxy-url:}")
    private val proxyUrl: String,
) {
    companion object {
        private const val CONNECTION_PROVIDER_NAME = "default-web-client"
    }

    @Bean
    fun defaultWebClientCodecConfigurer(): Consumer<ClientCodecConfigurer> =
        Consumer { clientDefaultCodecsConfigurer ->
            clientDefaultCodecsConfigurer
                .defaultCodecs()
                .jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
            clientDefaultCodecsConfigurer
                .defaultCodecs()
                .jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
        }

    @Bean
    fun defaultClientHttpConnector() =
        ReactorClientHttpConnector(
            reactorResourceFactory.apply {
                isUseGlobalResources = false
                connectionProvider =
                    ConnectionProvider
                        .builder(CONNECTION_PROVIDER_NAME)
                        .maxConnections(1024)
                        .maxIdleTime(Duration.ofSeconds(5L))
                        .maxLifeTime(Duration.ofSeconds(5L))
                        .pendingAcquireTimeout(Duration.ofSeconds(5L))
                        .lifo()
                        .build()
            },
        ) { httpClient: HttpClient ->
            httpClient
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
                .option(ChannelOption.TCP_NODELAY, true)
                .doOnConnected { connection ->
                    connection.addHandlerFirst(
                        ReadTimeoutHandler(5L, TimeUnit.SECONDS),
                    )
                    connection.addHandlerFirst(
                        WriteTimeoutHandler(5L, TimeUnit.SECONDS),
                    )
                }.let {
                    if (proxyUrl.isNullOrBlank().not()) {
                        it.proxy { proxyProvider ->
                            runCatching {
                                val proxyUrl = URI.create(proxyUrl)
                                proxyProvider
                                    .type(ProxyProvider.Proxy.HTTP)
                                    .host(proxyUrl.host)
                                    .port(proxyUrl.port)
                                    .nonProxyHostsPredicate(Predicate.isEqual("localhost"))
                            }.getOrDefault(it)
                        }
                    } else {
                        it
                    }
                }.keepAlive(true)
        }

    @Bean
    fun defaultWebClientBuilder() =
        WebClient
            .builder()
            .codecs(defaultWebClientCodecConfigurer())
            .baseUrl(baseUrl)
            // .filter() //logging customFilter
            .clientConnector(defaultClientHttpConnector())
}
