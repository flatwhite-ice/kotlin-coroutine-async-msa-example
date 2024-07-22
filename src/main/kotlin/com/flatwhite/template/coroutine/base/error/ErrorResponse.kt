package com.flatwhite.template.coroutine.base.error

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ErrorResponse(
    val errorCode: String,
    val errorMessage: String,
)
