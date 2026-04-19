package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.repository.LibroJpaRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservaService(
    private val libroJpaRepository: LibroJpaRepository,
    private val usuarioJpaRepository: UsuarioJpaRepository,
    private val reservaJpaRepository: ReservaJpaRepository,
) {

    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()

    fun obtenerReservas(usuarioId: Int, tipo: TipoReserva): List<Reserva> {
        usuarioJpaRepository.findById(usuarioId)
            .orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }

        return when (tipo) {
            TipoReserva.HECHAS -> reservaJpaRepository.findByUsuarioId(usuarioId)
            TipoReserva.RECIBIDAS -> reservaJpaRepository.findByLibroDuenioId(usuarioId)
        }
    }

    fun obtenerReservasAnuales(usuarioId: Int): List<ReservaAnualUsuarioDTO> {
        usuarioJpaRepository.findById(usuarioId)
            .orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }

        return reservaJpaRepository.findReservasAnualesByUsuarioId(usuarioId).map {
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
            if (reservaJpaRepository.existsSuperposicion(dto.libroId, dto.fechaDesde, dto.fechaHasta)) {
                throw ConflictException("El libro ya está reservado para esas fechas.")
            }

            val libro = libroJpaRepository.findById(dto.libroId)
                .orElseThrow { NotFoundException("Libro.kt ${dto.libroId} no encontrado") }

            val solicitante = usuarioJpaRepository.findById(dto.usuarioId)
                .orElseThrow { NotFoundException("Usuario ${dto.usuarioId} no encontrado") }

            val reserva = Reserva(
                libro = libro,
                usuario = solicitante,
                fechaDesde = dto.fechaDesde,
                fechaHasta = dto.fechaHasta,
            )

            reserva.confirmar()
            libro.actualizoDisponibilidad(false)

            val reservasDelLibro = reservaJpaRepository.findByLibroId(libro.id).size
            val karmaGanado = libro.calcularBiblioKarmaTotal(
                usuario = solicitante,
                desde = dto.fechaDesde,
                hasta = dto.fechaHasta,
                cantidadReservasDelLibro = reservasDelLibro
            )

            solicitante.bibliokarmas += karmaGanado
            if (solicitante.id != libro.duenio.id) {
                libro.duenio.bibliokarmas += karmaGanado
                usuarioJpaRepository.save(libro.duenio)
            }

            libroJpaRepository.save(libro)
            usuarioJpaRepository.save(solicitante)
            reservaJpaRepository.save(reserva)

            return reserva
        }
    }
}