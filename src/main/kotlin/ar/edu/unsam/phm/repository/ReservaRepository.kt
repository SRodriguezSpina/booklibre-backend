package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.exceptions.ConflictException
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ReservaRepository : CommonRepository<Reserva>() {
    fun findByLibroId(libroId: Int): List<Reserva> {
        return lista.filter { reserva -> reserva.libro.id == libroId }
    }

    fun getReservasDeUsuario(usuarioId: Int): List<Reserva> {
        return lista.filter { reserva ->
            val esDuenio = reserva.libro.duenio.id == usuarioId
            val esSolicitante = reserva.usuario.id == usuarioId
            esDuenio || esSolicitante
        }
    }

    fun existeSuperposicion(libroId: Int, desde: LocalDate, hasta: LocalDate) {
        val reservasQueChocan = this.search { reserva ->
            reserva.libro.id == libroId && reserva.sePisaCon(desde, hasta)
        }.isNotEmpty()

        if (reservasQueChocan) {
            throw ConflictException("El libro ya está reservado para esas fechas.")
        }
    }
}