package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.Reserva
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ReservaRepository : CommonRepository<Reserva>() {
    fun getReservasDeUsuario(usuarioId: Int): List<Reserva> {
        return lista.filter { reserva ->
            val esDuenio = reserva.libro.duenio.id == usuarioId
            val esSolicitante = reserva.usuario.id == usuarioId

            esDuenio || esSolicitante
        } // Realiza el filtro en el repositorio
    }

    fun existeSuperposicion(libroId: Int, desde: LocalDate, hasta: LocalDate): Boolean {
        val reservasQueChocan = this.search { reserva ->
            reserva.libro.id == libroId && reserva.sePisaCon(desde, hasta)
        }
        return reservasQueChocan.isNotEmpty()
    }
}