package com.flatwhite.template.coroutine.base.exception

import com.flatwhite.template.coroutine.base.error.ErrorResponse
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(value = [HttpResponseException::class])
    fun handleHttpResponseException(ex: HttpResponseException): ResponseEntity<ErrorResponse> =
        ResponseEntity<ErrorResponse>(
            ErrorResponse(errorCode = ex.errorCode, errorMessage = ex.errorMessage),
            HttpStatusCode.valueOf(ex.statusCode),
        )

    // TODO customize service exception
}
