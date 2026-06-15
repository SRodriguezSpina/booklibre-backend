package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.dtos.UsuarioBibliokarmaDTO
import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive

@RedisHash("top_bibliokarmas")
data class TopBibliokarmasCache(
    @Id
    val id: String = CACHE_KEY,

    val usuarios: List<UsuarioBibliokarmaDTO> = emptyList(),

    @TimeToLive
    val ttl: Long = TTL_SECONDS
) {
    companion object {
        const val CACHE_KEY = "TOP_5"
        const val TTL_SECONDS = 600L
    }
}