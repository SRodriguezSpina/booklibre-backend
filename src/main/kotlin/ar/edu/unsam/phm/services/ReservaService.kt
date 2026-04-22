package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservaService(
    private val libroRepository: LibroRepository,
    private val usuarioRepository: UsuarioRepository,
    private val reservaRepository: ReservaRepository,
) {

    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()

    fun obtenerReservas(usuarioId: Int, tipo: TipoReserva): List<Reserva> {
        usuarioRepository.getById(usuarioId)
        return when (tipo) {
            TipoReserva.HECHAS -> reservaRepository.findByUsuarioId(usuarioId)
            TipoReserva.RECIBIDAS -> reservaRepository.findByLibroDuenioId(usuarioId)
        }
    }

    fun obtenerReservasAnuales(usuarioId: Int): List<ReservaAnualUsuarioDTO> {
        usuarioRepository.getById(usuarioId)
        return reservaRepository.findReservasAnualesByUsuarioId(usuarioId).map {
            ReservaAnualUsuarioDTO(
                reservaId = it.reservaId,
                usuarioId = it.usuarioId,
                usuarioNombre = it.usuarioNombre,
                libroId = it.libroId,
                libroTitulo = it.libroTitulo,
                libroAutor = it.libroAutor,
                fechaDesde = it.fechaDesde,
                fechaHasta = it.fechaHasta,
                anioReserva = it.anioReserva,
            )
        }
    }

    @Transactional
    fun crearReserva(dto: CrearReservaDTO): Reserva {
        val lock = locks.computeIfAbsent(dto.libroId) { Any() }
        synchronized(lock) {
            val libro = libroRepository.getById(dto.libroId)
            val solicitante = usuarioRepository.getById(dto.usuarioId)

            if (libro.duenio.id == solicitante.id) {
                throw BusinessException("No podés reservar tu propio libro")
            }

            if (!libro.estaDisponible()) {
                throw BusinessException("El libro no está disponible para préstamo")
            }

            val reserva = Reserva(
                libro = libro,
                usuario = solicitante,
                fechaDesde = dto.fechaDesde,
                fechaHasta = dto.fechaHasta,
            )
            reserva.confirmar()
            libro.actualizoDisponibilidad(false)

            libroRepository.save(libro)
            usuarioRepository.save(solicitante)
            reservaRepository.save(reserva)
            return reserva
        }
    }
}