package com.hunzz.queueserver.dto

data class ProceedQueueResponse(
    val requestedCount: Long,
    val allowedCount: Long
)