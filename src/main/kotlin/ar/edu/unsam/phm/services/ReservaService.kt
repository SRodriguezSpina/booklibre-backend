package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
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
    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()
    fun obtenerReservas(usuarioId: Int, tipo: TipoReserva): List<Reserva> {
        usuarioRepository.getById(usuarioId)
        return when (tipo) {
            TipoReserva.HECHAS -> reservaRepository.getReservasHechas(usuarioId)
            TipoReserva.RECIBIDAS -> reservaRepository.getReservasRecibidas(usuarioId)
        }
    }
    fun crearReserva(dto: CrearReservaDTO): Reserva {
        val lock = locks.computeIfAbsent(dto.libroId) { Any() }
        synchronized(lock) {
            reservaRepository.existeSuperposicion(dto.libroId, dto.fechaDesde, dto.fechaHasta)
            val libro = libroRepository.getById(dto.libroId)
            val solicitante = usuarioRepository.getById(dto.usuarioId)
            val reserva = reservaRepository.create(
                Reserva(libro, solicitante, dto.fechaDesde, dto.fechaHasta)
            )
            reserva.confirmar()
            libro.actualizoDisponibilidad(false)
            usuarioRepository.update(solicitante)
            if (libro.duenio.id != solicitante.id) usuarioRepository.update(libro.duenio)
            return reserva
        }
    }
}