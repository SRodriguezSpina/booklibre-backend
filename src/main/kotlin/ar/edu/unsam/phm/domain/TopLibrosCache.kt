package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.LibroPopularCacheDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import org.springframework.data.annotation.Id
import kotlin.math.ceil

@RedisHash("top_libros")
data class TopLibrosCache(
    @Id
    val id: String = CACHE_KEY,
    val libros: List<LibroPopularCacheDTO> = emptyList(),
    val totalElements: Long = 0L,
    @TimeToLive
    val ttl: Long = 3600L,
) {
    companion object {
        const val CACHE_KEY = "HOME"
        const val PAGE_SIZE = 10
    }
    fun calcularTotalPages(): Int =
        if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / PAGE_SIZE).toInt()

    fun toPagedResponse(content: List<LibroHomeDTO>): PagedResponse<LibroHomeDTO> =
        PagedResponse(content, page = 0, totalPages = calcularTotalPages(), totalElements = totalElements)
}
