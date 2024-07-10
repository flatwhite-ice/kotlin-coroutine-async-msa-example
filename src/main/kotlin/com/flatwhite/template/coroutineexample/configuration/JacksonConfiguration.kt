package com.flatwhite.template.coroutineexample.configuration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JacksonConfiguration {
    @Bean
    fun defaultWebObjectMapper() =
        jacksonObjectMapper().apply {
            registerModules(Jdk8Module())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        }

    @Primary
    @Bean
    fun snakeCaseWebObjectMapper() =
        jacksonObjectMapper().apply {
            registerModules(Jdk8Module())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        }
}
