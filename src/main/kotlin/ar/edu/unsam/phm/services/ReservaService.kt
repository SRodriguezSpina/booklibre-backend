package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.ReservaDTO

interface ReservaService {
    fun obtenerReservas(usuarioId: Int): List<ReservaDTO>
    fun obtenerReservasAnuales(usuarioId: Int): List<ReservaAnualUsuarioDTO>
    fun crearReserva(dto: CrearReservaDTO): Reserva
}