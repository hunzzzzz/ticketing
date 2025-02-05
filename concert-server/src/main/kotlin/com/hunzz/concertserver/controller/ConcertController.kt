package com.hunzz.concertserver.controller

import com.hunzz.concertserver.utility.ConcertCodeMapper.mapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLEncoder

@Controller
@RequestMapping("/concerts")
class ConcertController(
    @Value("\${host.url.me}")
    val myUrl: String,

    @Value("\${host.url.queue-server}")
    val queueServerUrl: String
) {
    @GetMapping("/{concertId}")
    fun move(
        model: Model,
        @PathVariable concertId: Long,
        @RequestParam userId: Long
    ): String {
        model.addAttribute("myUrl", myUrl)
        model.addAttribute("userId", userId)
        model.addAttribute("concertId", concertId)

        return mapper[concertId] ?: "error-page"
    }

    @GetMapping("/{concertId}/ticket")
    fun move(
        @PathVariable concertId: Long
    ): String {
        return mapper[concertId]?.let { "$it-ticket" } ?: "error-page"
    }

    @GetMapping("/{concertId}/ticket/check")
    fun check(
        @PathVariable concertId: Long,
        @RequestParam userId: Long,
        request: HttpServletRequest
    ): String {
        // 기본 세팅
        val restTemplate = RestTemplate()
        val redirectUrl = "${myUrl}/concerts/${concertId}/ticket"
        val encodedRedirectUrl = URLEncoder.encode(redirectUrl, "UTF-8")

        // 쿠키에서 토큰 값을 가져온다.
        val token = request.cookies?.firstOrNull { it.name == "${mapper[concertId]}-ticket" }?.value

        // 예매 페이지에 진입이 가능한지 여부를 확인한다.
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

        // 허용 상태면, 예매 페이지로 진입한다.
        return if (isAllowed) mapper[concertId]?.let { "$it-ticket" } ?: "error-page"
        // 대기 상태면, 대기 페이지로 리다이렉트한다.
        else "redirect:${queueServerUrl}/wait?concertId=${concertId}&userId=${userId}&redirectUrl=${encodedRedirectUrl}"
    }
}