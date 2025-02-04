package com.hunzz.queueserver.service

import com.hunzz.queueserver.dto.ProceedQueueResponse
import com.hunzz.queueserver.dto.RankResponse
import com.hunzz.queueserver.exception.ErrorCode.ALREADY_REGISTERED_USER
import com.hunzz.queueserver.exception.QueueException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.Instant

@Service
class QueueService(
    @Value("\${scheduler.enabled}")
    private val isSchedulerEnabled: Boolean,

    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    companion object {
        const val ALLOWING_USER_COUNT = 5L

        fun getKeyOfWaitQueue(concertId: Long) = "queue:$concertId:wait"
        fun getKeyOfProceedQueue(concertId: Long) = "queue:$concertId:proceed"
    }

    private fun add(key: String, value: String, score: Double): Mono<Boolean> {
        return redisTemplate.opsForZSet().add(key, value, score)
    }

    private fun rankOfWaitQueue(concertId: Long, userId: Long): Mono<Long> {
        return redisTemplate.opsForZSet()
            .rank(getKeyOfWaitQueue(concertId = concertId), userId.toString())
            .defaultIfEmpty(-1L)
            .map { rankValue -> if (rankValue != -1L) rankValue + 1 else rankValue }
    }

    fun register(concertId: Long, userId: Long): Mono<RankResponse> {
        // 기본 세팅
        val now = Instant.now().epochSecond.toDouble()

        // userId를 대기열에 등록하고, 대기 순위를 리턴한다.
        return add(key = getKeyOfWaitQueue(concertId = concertId), value = userId.toString(), score = now)
            .filter { isSucceed -> isSucceed }
            .switchIfEmpty(Mono.error(QueueException(ALREADY_REGISTERED_USER)))
            .flatMap { rankOfWaitQueue(concertId = concertId, userId = userId) }
            .map { rank -> RankResponse(rank = rank) }
    }

    fun getRank(concertId: Long, userId: Long): Mono<RankResponse> {
        // 현재 대기 순위를 리턴한다.
        return rankOfWaitQueue(concertId = concertId, userId = userId)
            .map { rank -> RankResponse(rank = rank) }
    }

    fun generateToken(concertId: Long, userId: Long): Mono<String> {
        return Mono.fromCallable {
            // SHA-256 방식을 이용하여 해시 토큰을 만든다.
            val digest = MessageDigest.getInstance("SHA-256")
            val data = "concert:$concertId:$userId"

            digest.digest(data.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }

    fun isAllowedByToken(concertId: Long, userId: Long, token: String): Mono<Boolean> {
        // 토큰을 조사하여, 예매 페이지로 진입할 수 있는지 여부를 리턴한다.
        return generateToken(concertId = concertId, userId = userId)
            .map { it == token }
    }

    fun allow(concertId: Long, count: Long): Mono<ProceedQueueResponse> {
        // 기본 세팅
        val now = Instant.now().epochSecond.toDouble()

        // 대기열에서 userId를 삭제하고, 예매 가능 큐에 userId를 추가한다.
        return redisTemplate.opsForZSet()
            .popMin(getKeyOfWaitQueue(concertId = concertId), count)
            .flatMap {
                add(
                    key = getKeyOfProceedQueue(concertId = concertId),
                    value = it.value.toString(),
                    score = now
                )
            }
            .count()
            .map { ProceedQueueResponse(requestedCount = count, allowedCount = it) }
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 3000)
    fun scheduledAllow() {
        if (!isSchedulerEnabled) return

        // concertId를 추출한 다음, 3초에 한 번 씩 5명에게 예매 기회를 제공한다.
        val a = redisTemplate.scan(
            ScanOptions.scanOptions()
                .match("queue:*:wait")
                .count(100)
                .build()
        ).map { key -> key.split(":")[1] }
            .flatMap { concertId -> allow(concertId = concertId.toLong(), count = ALLOWING_USER_COUNT) }
            .subscribe()
    }
}