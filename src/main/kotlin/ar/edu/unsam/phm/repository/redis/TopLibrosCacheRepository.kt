package ar.edu.unsam.phm.repository.redis

import ar.edu.unsam.phm.domain.TopLibrosCache
import org.springframework.data.repository.CrudRepository

interface TopLibrosCacheRepository : CrudRepository<TopLibrosCache, String>