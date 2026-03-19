package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.ReservaRepository
import org.springframework.stereotype.Service

@Service
class ReservaService(
    private val reservaRepository: ReservaRepository
) {
    fun obtenerReservas(usuarioId: Int): List<ReservaDTO> {
        val reservas = reservaRepository.getReservasDeUsuario(usuarioId)
        return reservas.map { it.toDTO(usuarioId) }
    }
}