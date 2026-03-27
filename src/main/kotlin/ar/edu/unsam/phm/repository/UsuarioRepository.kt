package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.Usuario
import org.springframework.stereotype.Component

@Component
class UsuarioRepository : CommonRepository<Usuario>() {
    fun findById(id: Int): Usuario? = lista.find { it.id == id }
    fun findByEmail(email: String): Usuario? = lista.find { it.getCredencial().email.equals(email, ignoreCase = true) }
    fun findByCelular(celular: String): Usuario? = lista.find { it.celular == celular }

    fun findReservasByLibroId(libroId: Int): List<Reserva> =
        lista
            .flatMap { it.reservas }
            .filter { it.libro.id == libroId }
            .distinctBy { it.id }

    fun resolverNombresCalificaciones(calificaciones: List<Calificacion>): Map<Int, String> =
        calificaciones
            .map { it.usuarioId }
            .toSet()
            .associateWith { idCalificador ->
                findById(idCalificador)?.nombre ?: "Usuario $idCalificador"
            }
}
