package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.TasaConversionLibroDTO
import ar.edu.unsam.phm.repository.redis.TopLibrosCacheRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import org.springframework.stereotype.Service

@Service
class TasaConversionService(
    private val topLibrosCacheRepository: TopLibrosCacheRepository,
    private val reservaJpaRepository: ReservaJpaRepository,
    private val libroService: LibroService,
) {
    fun obtenerTasaConversion(): List<TasaConversionLibroDTO> {
        val cache = topLibrosCacheRepository.getCachedTopLibros()
            ?: libroService.actualizarCacheTopLibros()

        val top5Libros = cache.libros.take(5)
        if (top5Libros.isEmpty()) return emptyList()

        val libroIds = top5Libros.map { it.id }

        // Fetch reservation counts from PostgreSQL
        val reservasCounts = reservaJpaRepository.countReservasByLibroIds(libroIds)
            .associate { it.libroId to it.count }

        return top5Libros.map { libroCache ->
            val count = reservasCounts[libroCache.id]?.toInt() ?: 0
            TasaConversionLibroDTO.de(libroCache, count)
        }
    }
}
