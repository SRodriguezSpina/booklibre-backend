package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.dtos.LibroHomeDTO
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.annotation.Id

@RedisHash("top_libros")
data class TopLibrosCache(
    @Id
    val id: String = "HOME",
    val libros: MutableList<LibroHomeDTO> = mutableListOf()
)