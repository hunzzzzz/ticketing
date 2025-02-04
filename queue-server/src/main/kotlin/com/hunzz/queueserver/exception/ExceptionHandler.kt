package com.hunzz.queueserver.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import reactor.core.publisher.Mono

@RestControllerAdvice
class ExceptionHandler {
    @ExceptionHandler(QueueException::class)
    fun handleQueueException(exception: QueueException): Mono<ResponseEntity<ErrorResponse>> {
        return Mono.just(
            ResponseEntity
                .status(exception.errorCode.status)
                .body(ErrorResponse(code = exception.errorCode.code, message = exception.errorCode.message))
        )
    }
}