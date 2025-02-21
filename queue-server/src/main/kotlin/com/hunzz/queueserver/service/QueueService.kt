package com.hunzz.queueserver.service

import com.hunzz.queueserver.dto.ProceedQueueResponse
import com.hunzz.queueserver.dto.RankResponse
import com.hunzz.queueserver.exception.ErrorCode.ALREADY_REGISTERED_USER
import com.hunzz.queueserver.exception.QueueException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Description
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class QueueService(
    @Value("\${scheduler.enabled}")
    private val isSchedulerEnabled: Boolean,

    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    companion object {
        const val ALLOWING_USER_COUNT = 10L

        fun getKeyOfWaitQueue(concertId: Long) = "queue:$concertId:wait"
        fun getKeyOfProceedQueue(concertId: Long) = "queue:$concertId:proceed"
    }

    private fun add(key: String, value: String): Mono<Boolean> {
        val now = Instant.now().epochSecond.toDouble()

        return redisTemplate.opsForZSet().add(key, value, now)
    }

    private fun rankOfWaitQueue(concertId: Long, userId: Long): Mono<Long> {
        return redisTemplate.opsForZSet()
            .rank(getKeyOfWaitQueue(concertId = concertId), userId.toString())
            .defaultIfEmpty(-1L)
            .map { rankValue -> if (rankValue != -1L) rankValue + 1 else rankValue }
    }

    @Description("userId를 대기열에 등록하고, 대기 순위를 리턴한다.")
    fun register(concertId: Long, userId: Long): Mono<RankResponse> {
        return add(key = getKeyOfWaitQueue(concertId = concertId), value = userId.toString()) // 토큰 생성
            .filter { isSucceed -> isSucceed } // 등록 성공 여부 필터링
            .switchIfEmpty(Mono.error(QueueException(ALREADY_REGISTERED_USER))) // 이미 등록된 유저 예외 처리
            .flatMap { rankOfWaitQueue(concertId = concertId, userId = userId) } // 대기 순위 조회
            .map { rank -> RankResponse(rank = rank) } // RankResponse 객체 생성
    }

    @Description("현재 대기 순위를 리턴한다.")
    fun getRank(concertId: Long, userId: Long): Mono<RankResponse> {
        return rankOfWaitQueue(concertId = concertId, userId = userId)
            .map { rank -> RankResponse(rank = rank) }
    }

    @Description("${ALLOWING_USER_COUNT}명의 유저에게 예매 페이지로의 진입을 허용한다.")
    fun allow(concertId: Long, count: Long): Mono<ProceedQueueResponse> {
        // 대기 큐에서 userId를 삭제하고, 허용 큐에 userId를 삽입한다.
        return redisTemplate.opsForZSet()
            .popMin(getKeyOfWaitQueue(concertId = concertId), count)
            .flatMap {
                add(
                    key = getKeyOfProceedQueue(concertId = concertId),
                    value = it.value.toString()
                )
            }
            .count()
            .map { ProceedQueueResponse(requestedCount = count, allowedCount = it) }
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 3000)
    fun scheduledAllow() {
        if (!isSchedulerEnabled) return

        // concertId를 추출한 다음, 3초에 한 번 씩 10명에게 예매 기회를 제공한다.
        redisTemplate.scan(
            ScanOptions.scanOptions()
                .match("queue:*:wait")
                .count(100)
                .build()
        ).map { key -> key.split(":")[1] }
            .flatMap { concertId -> allow(concertId = concertId.toLong(), count = ALLOWING_USER_COUNT) }
            .subscribe()
    }
}