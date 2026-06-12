package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Usuario
import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioJpaRepository : JpaRepository<Usuario, Int> {
    fun findByEmail(email: String): Usuario?
    fun findByCelular(celular: String): Usuario?
    fun findTop5ByOrderByBibliokarmasDesc(): MutableList<Usuario>
}