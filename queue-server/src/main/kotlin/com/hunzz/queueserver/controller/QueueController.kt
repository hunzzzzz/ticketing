package com.hunzz.queueserver.controller

import com.hunzz.queueserver.dto.RankResponse
import com.hunzz.queueserver.service.QueueService
import com.hunzz.queueserver.utility.ConcertCodeMapper.mapper
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration

@RestController
@RequestMapping("/queue")
class QueueController(
    private val queueService: QueueService
) {
    @GetMapping("/allowed")
    fun isAllowed(
        @RequestParam concertId: Long,
        @RequestParam userId: Long,
        @RequestParam token: String
    ): Mono<Boolean> {
        return queueService.isAllowedByToken(concertId = concertId, userId = userId, token = token)
    }

    @GetMapping("/rank")
    fun rank(
        @RequestParam concertId: Long,
        @RequestParam userId: Long
    ): Mono<RankResponse> {
        return queueService.getRank(concertId = concertId, userId = userId)
    }

    @GetMapping("/touch")
    fun touch(
        @RequestParam concertId: Long,
        @RequestParam userId: Long,
        exchange: ServerWebExchange
    ): Mono<*> {
        return Mono.defer { queueService.generateToken(concertId = concertId, userId = userId) }
            .map { token ->
                exchange.response.addCookie(
                    ResponseCookie.from("${mapper[concertId]}-ticket", token)
                        .maxAge(Duration.ofMinutes(5))
                        .path("/")
                        .build()
                )
            }
    }
}