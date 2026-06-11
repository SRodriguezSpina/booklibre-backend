package ar.edu.unsam.phm.domain

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class LibrosClicks(
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        const val CACHE_KEY = "libros_clicks"
    }

    fun registrarClick(libroId: Int) {
        redisTemplate.opsForZSet().incrementScore(CACHE_KEY, libroId.toString(), 1.0)
    }

    fun getTopLibrosIds(limit: Int = 10): List<Int> {
        return redisTemplate.opsForZSet()
            .reverseRange(CACHE_KEY, 0, (limit - 1).toLong())
            ?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    fun getLibroMasClickeadoIdYScore(): Pair<Int, Long>? {
        val topElement = redisTemplate.opsForZSet().reverseRangeWithScores(CACHE_KEY, 0, 0)?.firstOrNull()
        if (topElement != null && topElement.score != null && topElement.score!! > 0) {
            val id = topElement.value?.toIntOrNull() ?: return null
            return Pair(id, topElement.score!!.toLong())
        }
        return null
    }
}
