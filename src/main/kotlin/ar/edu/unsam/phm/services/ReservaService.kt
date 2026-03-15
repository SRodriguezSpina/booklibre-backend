package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service

@Service
class ReservaService( val usuarioRepository: UsuarioRepository) {
    fun obtenerReservas(usuarioId: Int): List<ReservaDTO> {
        val usuario = usuarioRepository.findById(usuarioId)
        return usuario?.todasLasReservas()?.map { it.toDTO(usuarioId) } ?: emptyList()
    }
}