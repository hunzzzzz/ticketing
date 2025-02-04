package com.hunzz.queueserver.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    ALREADY_REGISTERED_USER(HttpStatus.CONFLICT, "UQ-001", "already registered in queue")
}