package com.hunzz.queueserver.controller

import com.hunzz.queueserver.service.QueueService
import com.hunzz.queueserver.service.TokenService
import com.hunzz.queueserver.utility.ConcertCodeMapper.mapper
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.result.view.Rendering
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Controller
class WaitController(
    private val tokenService: TokenService,
    private val queueService: QueueService
) {
    @GetMapping("/wait")
    fun wait(
        @RequestParam concertId: Long,
        @RequestParam userId: Long,
        @RequestParam redirectUrl: String,
        exchange: ServerWebExchange
    ): Mono<Rendering> {
        // 쿠키에서 토큰 값을 가져온다.
        val token = exchange.request.cookies
            .getFirst("${mapper[concertId]}-ticket")?.value ?: ""

        // 예매 페이지로 진입 가능 여부를 조회한다.
        return tokenService.isAllowed(concertId = concertId, userId = userId, token = token)
            .filter { allowed -> allowed }
            // 진입이 가능한 경우, 예매 페이지로 리다이렉트한다.
            .flatMap { Mono.just(Rendering.redirectTo(redirectUrl).build()) }
            // 진입이 불가능한 경우, 대기 페이지로 이동한다.
            .switchIfEmpty(
                queueService.register(concertId = concertId, userId = userId) // 대기열 등록
                    .onErrorResume { queueService.getRank(concertId = concertId, userId = userId) } // 예외 처리
                    .map { response ->
                        Rendering.view("waiting-room.html")
                            .modelAttribute("concertId", concertId)
                            .modelAttribute("userId", userId)
                            .modelAttribute("rank", response.rank)
                            .build()
                    }
            )

    }
}