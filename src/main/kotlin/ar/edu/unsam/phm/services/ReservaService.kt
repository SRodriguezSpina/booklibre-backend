package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ReservaService(
    private val libroRepository: LibroRepository,
    private val usuarioRepository: UsuarioRepository,
    private val reservaRepository: ReservaRepository
) {
    fun obtenerReservas(usuarioId: Int): List<ReservaDTO> {
        usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario $usuarioId no encontrado")
        val reservas = reservaRepository.getReservasDeUsuario(usuarioId)
        return reservas.map { it.toDTO(usuarioId) }
    }
    fun crearReserva(dto: CrearReservaDTO): Reserva {
        val libro = libroRepository.getById(dto.libroId)

        if (reservaRepository.existeSuperposicion(libro.id, dto.fechaDesde, dto.fechaHasta)) {
            throw ConflictException(
                "Lo sentimos, este libro ya se encuentra reservado para las fechas seleccionadas."
            )
        }

        val usuarioSolicitante = usuarioRepository.getById(dto.usuarioId)

        val reservaGuardada = reservaRepository.create(
            Reserva(libro = libro, usuario = usuarioSolicitante, fechaDesde = dto.fechaDesde, fechaHasta = dto.fechaHasta)
        )

        reservaGuardada.confirmar()

        usuarioRepository.update(usuarioSolicitante)
        if (libro.duenio.id != usuarioSolicitante.id) {
            usuarioRepository.update(libro.duenio)
        }

        return reservaGuardada
    }
}