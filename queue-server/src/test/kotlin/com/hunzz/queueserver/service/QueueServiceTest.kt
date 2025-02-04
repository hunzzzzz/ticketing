package com.hunzz.queueserver.service

import com.hunzz.queueserver.config.EmbeddedRedis
import com.hunzz.queueserver.dto.ProceedQueueResponse
import com.hunzz.queueserver.dto.RankResponse
import com.hunzz.queueserver.exception.QueueException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.context.ActiveProfiles
import reactor.test.StepVerifier

@ActiveProfiles("test")
@SpringBootTest(classes = [EmbeddedRedis::class])
class QueueServiceTest {
    @Autowired
    lateinit var queueService: QueueService

    @Autowired
    lateinit var redisTemplate: ReactiveRedisTemplate<String, String>

    @BeforeEach
    fun clean() {
        redisTemplate.connectionFactory.reactiveConnection
            .serverCommands().flushAll().subscribe()
    }

    @Test
    fun register() {
        StepVerifier.create(queueService.register(1, 100))
            .expectNext(RankResponse(1))
            .verifyComplete()

        StepVerifier.create(queueService.register(1, 200))
            .expectNext(RankResponse(2))
            .verifyComplete()

        StepVerifier.create(queueService.register(1, 300))
            .expectNext(RankResponse(3))
            .verifyComplete()
    }

    @Test
    fun already_registered_user() {
        StepVerifier.create(
            queueService.register(1, 100)
                .then(queueService.register(1, 100))
        )
            .expectError(QueueException::class.java)
            .verify()
    }

    @Test
    fun rank() {
        StepVerifier.create(
            queueService.register(1, 100)
                .then(queueService.getRank(1, 100))
        ).expectNext(RankResponse(1))
            .verifyComplete()
    }

    @Test
    fun emptyRank() {
        StepVerifier.create(queueService.getRank(1, 100))
            .expectNext(RankResponse(-1))
            .verifyComplete()
    }

    @Test
    fun allow1() {
        StepVerifier.create(queueService.allow(1, 3))
            .expectNext(ProceedQueueResponse(3, 0))
            .verifyComplete()
    }

    @Test
    fun allow2() {
        StepVerifier.create(
            queueService.register(1, 100)
                .then(queueService.register(1, 200))
                .then(queueService.register(1, 300))
                .then(queueService.allow(1, 3))
        ).expectNext(ProceedQueueResponse(3, 3))
            .verifyComplete()
    }

    @Test
    fun allow3() {
        StepVerifier.create(
            queueService.register(1, 100)
                .then(queueService.register(1, 200))
                .then(queueService.register(1, 300))
                .then(queueService.register(1, 400))
                .then(queueService.register(1, 500))
                .then(queueService.allow(1, 3))
        ).expectNext(ProceedQueueResponse(3, 3))
            .verifyComplete()
    }

    @Test
    fun isAllowedByToken() {
        StepVerifier.create(
            queueService.isAllowedByToken(
                concertId = 1,
                userId = 100,
                token = "82ecf7c3902a78c3f1e73a1843bdb8d33dce26c82a272f421707b19a2e88db4c"
            )
        ).expectNext(true)
            .verifyComplete()
    }

    @Test
    fun generateToken() {
        StepVerifier.create(queueService.generateToken(1, 100))
            .expectNext("82ecf7c3902a78c3f1e73a1843bdb8d33dce26c82a272f421707b19a2e88db4c")
            .verifyComplete()
    }
}