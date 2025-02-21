package com.hunzz.queueserver.service

import org.springframework.context.annotation.Description
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.MessageDigest

@Service
class TokenService {
    @Description("SHA-256 해시 알고리즘을 사용하여 토큰을 생성한다.")
    fun generateToken(concertId: Long, userId: Long): Mono<String> {
        return Mono.fromCallable {
            val digest = MessageDigest.getInstance("SHA-256")
            val data = "concert:$concertId:$userId"

            digest.digest(data.toByteArray()) // 문자열(의 바이트 배열)에 대한 해시 값 계산
                .joinToString("") { "%02x".format(it) } // 16진수로 변환
        }
    }

    @Description("토큰의 유효성을 검증한다.")
    fun isAllowed(concertId: Long, userId: Long, token: String): Mono<Boolean> {
        return generateToken(concertId = concertId, userId = userId)
            .map { it == token }
    }
}