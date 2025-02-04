package com.hunzz.concertserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ConcertServerApplication

fun main(args: Array<String>) {
    runApplication<ConcertServerApplication>(*args)
}
