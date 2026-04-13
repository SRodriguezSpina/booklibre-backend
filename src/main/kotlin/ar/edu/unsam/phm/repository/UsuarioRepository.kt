package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import org.springframework.stereotype.Component

@Component
class UsuarioRepository : CommonRepository<Usuario>() {
    override fun getById(id: Int): Usuario =
        lista.find { it.id == id }
            ?: throw NotFoundException("Usuario $id no encontrado")

    fun findByEmail(email: String): Usuario? =
        lista.find { it.getCredencial().email.equals(email, ignoreCase = true) }

    fun findByCelular(celular: String): Usuario? =
        lista.find { it.celular == celular }

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

    fun resolverNombresCalificaciones(calificaciones: List<Calificacion>): Map<Int, String> =
        calificaciones.map { it.usuarioId }.toSet()
            .associateWith { id -> getById(id).nombre }
}
