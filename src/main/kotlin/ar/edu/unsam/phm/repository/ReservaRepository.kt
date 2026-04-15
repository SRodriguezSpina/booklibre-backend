package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ReservaRepository : CommonRepository<Reserva>() {
    fun findByLibroId(libroId: Int): List<Reserva> {
        return lista.filter { reserva -> reserva.libro.id == libroId }
    }

//    fun getReservasDeUsuario(usuarioId: Int): List<Reserva> {
//        return lista.filter { reserva ->
//            val esDuenio = reserva.libro.duenio.id == usuarioId
//            val esSolicitante = reserva.usuario.id == usuarioId
//            esDuenio || esSolicitante
//        }
//    }
    fun getReservasHechas(usuarioId: Int): List<Reserva> {
        return lista.filter {it.usuario.id == usuarioId }
    }
    fun getReservasRecibidas(usuarioId: Int): List<Reserva> {
        return lista.filter {it.libro.duenio.id == usuarioId }
    }

    fun existeSuperposicion(libroId: Int, desde: LocalDate, hasta: LocalDate) {
        val reservasQueChocan = this.search { reserva ->
            reserva.libro.id == libroId && reserva.sePisaCon(desde, hasta)
        }.isNotEmpty()

        if (reservasQueChocan) {
            throw ConflictException("El libro ya está reservado para esas fechas.")
        }
    }

    fun obtenerIdsLibrosReservados(fechaDesdeStr: String?, fechaHastaStr: String?): Set<Int>? {
        if (fechaDesdeStr == null || fechaHastaStr == null) return null

        val desde = LocalDate.parse(fechaDesdeStr)
        val hasta = LocalDate.parse(fechaHastaStr)

        if (!desde.isBefore(hasta)) {
            throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
        }

        return lista
            .filter { it.sePisaCon(desde, hasta) }
            .map { it.libro.id }
            .toSet()
    }
}