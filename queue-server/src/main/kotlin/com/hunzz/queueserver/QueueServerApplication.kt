package com.hunzz.queueserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class QueueServerApplication

fun main(args: Array<String>) {
    runApplication<QueueServerApplication>(*args)
}
