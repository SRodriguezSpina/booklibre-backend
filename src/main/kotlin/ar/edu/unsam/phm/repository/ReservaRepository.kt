package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.Reserva
import org.springframework.stereotype.Component

@Component
class ReservaRepository : CommonRepository<Reserva>() {
    fun getReservasDeUsuario(usuarioId: Int): List<Reserva> {
        return lista.filter { reserva ->
            val esDuenio = reserva.libro.duenio.id == usuarioId
            val esSolicitante = reserva.usuario.id == usuarioId

            esDuenio || esSolicitante
        }
    }
}