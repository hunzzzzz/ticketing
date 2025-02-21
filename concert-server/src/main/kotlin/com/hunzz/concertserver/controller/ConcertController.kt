package com.hunzz.concertserver.controller

import com.hunzz.concertserver.utility.ConcertCodeMapper.mapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Controller
@RequestMapping("/concerts")
class ConcertController(
    @Value("\${host.url.me}")
    val myUrl: String,

    @Value("\${host.url.queue-server}")
    val queueServerUrl: String
) {
    @GetMapping("/{concertId}/ticket/check")
    fun check(
        @PathVariable concertId: Long,
        @RequestParam userId: Long,
        request: HttpServletRequest
    ): ResponseEntity<String> {
        // 쿠키에서 토큰 값을 가져온다.
        val token = request.cookies?.firstOrNull {
            it.name == "${mapper[concertId]}-ticket"
        }?.value

        // 토큰 값이 존재하면, queue-server로 요청을 보내어 토큰의 유효성 여부를 확인한다.
        val restTemplate = RestTemplate()
        var isAllowed = false

        if (token != null) {
            val uri = UriComponentsBuilder
                .fromUriString(queueServerUrl)
                .path("/queue/allowed")
                .queryParam("concertId", concertId)
                .queryParam("userId", userId)
                .queryParam("token", token)
                .encode()
                .build()
                .toUri()
            isAllowed = restTemplate.getForObject(uri, Boolean::class.java) ?: false
        }

        // 토큰이 유효한 경우, 예매 페이지로 바로 진입한다.
        // 토큰이 존재하지 않거나 유효하지 않은 경우, 대기 페이지로 진입한다.
        val body = if (isAllowed) "/concerts/${concertId}/ticket"
        else {
            val redirectUrl = "${myUrl}/concerts/${concertId}/ticket"
            "${queueServerUrl}/wait?concertId=${concertId}&userId=${userId}&redirectUrl=${redirectUrl}"
        }

        return ResponseEntity.ok(body)
    }
}