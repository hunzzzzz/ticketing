package com.hunzz.queueserver.controller

import com.hunzz.queueserver.dto.RankResponse
import com.hunzz.queueserver.service.QueueService
import com.hunzz.queueserver.service.TokenService
import com.hunzz.queueserver.utility.ConcertCodeMapper.mapper
import org.springframework.context.annotation.Description
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
    private val tokenService: TokenService,
    private val queueService: QueueService
) {
    @Description("토큰을 검증하여, 예매 페이지로 진입할 수 있는지 여부를 판단한다.")
    @GetMapping("/allowed")
    fun isAllowed(
        @RequestParam concertId: Long,
        @RequestParam userId: Long,
        @RequestParam token: String
    ): Mono<Boolean> {
        return tokenService.isAllowed(concertId = concertId, userId = userId, token = token)
    }

    @Description("대기 순위를 조회한다.")
    @GetMapping("/rank")
    fun rank(
        @RequestParam concertId: Long,
        @RequestParam userId: Long
    ): Mono<RankResponse> {
        return queueService.getRank(concertId = concertId, userId = userId)
    }

    @Description("토큰을 생성하고, 쿠키에 넣어준다.")
    @GetMapping("/create-token")
    fun touch(
        @RequestParam concertId: Long,
        @RequestParam userId: Long,
        exchange: ServerWebExchange
    ): Mono<*> {
        return Mono.defer { tokenService.generateToken(concertId = concertId, userId = userId) }
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