package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.Reserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
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
        val estaOcupado = reservaRepository.existeSuperposicion(libro.id, dto.fechaDesde, dto.fechaHasta)

        if (estaOcupado) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Lo sentimos, este libro ya se encuentra reservado para las fechas seleccionadas."
            )
        }

        val usuario = usuarioRepository.getById(dto.usuarioId)
        val nuevaReserva = Reserva(
            libro = libro,
            usuario = usuario,
            fechaDesde = dto.fechaDesde,
            fechaHasta = dto.fechaHasta
        )

        usuario.agregarReserva(nuevaReserva)
        usuarioRepository.update(usuario)
        return reservaRepository.create(nuevaReserva)
    }
}