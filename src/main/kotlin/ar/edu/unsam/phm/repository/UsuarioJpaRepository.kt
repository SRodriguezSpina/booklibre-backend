package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Usuario
import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioJpaRepository : JpaRepository<Usuario, Int> {
    fun findByEmail(email: String): Usuario?
    fun findByCelular(celular: String): Usuario?
    fun existsByEmail(email: String): Boolean
    fun existsByCelular(celular: String): Boolean
    fun validarEmailDisponible(email: String, idActual: Int) {
        // no se puede hacer acá — va en el repositorio wrapper
    }
}