package ar.edu.unsam.phm.repository.redis

import ar.edu.unsam.phm.domain.TopBibliokarmasCache
import org.springframework.data.repository.CrudRepository

interface TopBibliokarmasCacheRepository : CrudRepository<TopBibliokarmasCache, String> {
    fun getCachedTopBibliokarmas(): TopBibliokarmasCache? {
        return findById(TopBibliokarmasCache.CACHE_KEY).orElse(null)
    }
}