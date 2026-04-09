package ar.edu.unsam.phm.testing.inmemory.repository

import ar.edu.unsam.phm.domain.Reserva
import java.time.LocalDate

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

    fun existeSuperposicion(libroId: Int, desde: LocalDate, hasta: LocalDate): Boolean {
        val reservasQueChocan = search { reserva ->
            reserva.libro.id == libroId && reserva.sePisaCon(desde, hasta)
        }
        return reservasQueChocan.isNotEmpty()
    }
}