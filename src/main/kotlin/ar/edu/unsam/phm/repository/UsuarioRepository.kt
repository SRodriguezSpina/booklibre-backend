package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import org.springframework.stereotype.Component

@Component
class UsuarioRepository(
    private val jpa: UsuarioJpaRepository
) {
    fun getById(id: Int): Usuario =
        jpa.findById(id).orElseThrow { NotFoundException("Usuario $id no encontrado") }

    fun findById(id: Int): Usuario? =
        jpa.findById(id).orElse(null)

    fun findByEmail(email: String): Usuario? = jpa.findByEmail(email)

    fun findByCelular(celular: String): Usuario? = jpa.findByCelular(celular)

    fun validarEmailDisponible(email: String, idActual: Int) {
        val existente = findByEmail(email)
        if (existente != null && existente.id != idActual)
            throw ConflictException("El email ya está en uso")
    }

    fun validarCelularDisponible(celular: String, idActual: Int) {
        val existente = findByCelular(celular)
        if (existente != null && existente.id != idActual)
            throw ConflictException("El celular ya está en uso")
    }

    fun save(usuario: Usuario): Usuario = jpa.save(usuario)

    fun resolverNombresCalificaciones(calificaciones: List<Calificacion>): Map<Int, String> =
        calificaciones.map { it.usuarioId }.toSet()
            .associateWith { id -> findById(id)?.nombre ?: "Usuario $id" }
}