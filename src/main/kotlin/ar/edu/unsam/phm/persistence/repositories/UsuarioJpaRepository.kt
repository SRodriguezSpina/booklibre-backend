package ar.edu.unsam.phm.persistence.repositories

import ar.edu.unsam.phm.persistence.entities.UsuarioEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioJpaRepository : JpaRepository<UsuarioEntity, Int> {
    fun findByEmail(email: String): UsuarioEntity?
    fun findByCelular(celular: String): UsuarioEntity?
    fun existsByEmail(email: String): Boolean
    fun existsByCelular(celular: String): Boolean
}

