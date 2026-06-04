package ar.edu.unsam.phm.repository.redis

import ar.edu.unsam.phm.domain.TopLibrosCache
import org.springframework.data.repository.CrudRepository

interface TopLibrosCacheRepository : CrudRepository<TopLibrosCache, String> {
    fun getCachedTopLibros(): TopLibrosCache? {
        return findById(TopLibrosCache.CACHE_KEY).orElse(null)
    }
}