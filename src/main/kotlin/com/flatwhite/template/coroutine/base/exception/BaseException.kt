package com.flatwhite.template.coroutine.base.exception

import org.springframework.http.HttpStatus

open class BaseException(
    override val message: String?,
    override val cause: Throwable?,
) : RuntimeException()

class HttpResponseException(
    val statusCode: Int,
    val httpStatus: HttpStatus,
    val errorCode: String,
    val errorMessage: String,
    cause: Throwable? = null,
) : BaseException(message = errorMessage, cause = cause)
