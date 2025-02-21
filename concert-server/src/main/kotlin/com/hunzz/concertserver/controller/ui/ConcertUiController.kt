package com.hunzz.concertserver.controller.ui

import com.hunzz.concertserver.utility.ConcertCodeMapper.mapper
import org.springframework.context.annotation.Description
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/concerts")
class ConcertUiController {
    @Description("콘서트 정보 페이지로 이동")
    @GetMapping("/{concertId}")
    fun move(
        model: Model,
        @PathVariable concertId: Long,
        @RequestParam userId: Long
    ): String {
        model.addAttribute("userId", userId)
        model.addAttribute("concertId", concertId)

        return mapper[concertId] ?: "error-page"
    }

    @Description("콘서트 예매 페이지로 이동")
    @GetMapping("/{concertId}/ticket")
    fun move(
        @PathVariable concertId: Long
    ): String {
        return mapper[concertId]?.let { "$it-ticket" } ?: "error-page"
    }
}