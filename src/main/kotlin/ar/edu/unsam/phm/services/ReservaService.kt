package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.mappers.toEntity
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

    fun obtenerReservas(usuarioId: Int): List<Reserva> {
        usuarioJpaRepository.findById(usuarioId)
            .orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }

        val reservasDomain = reservaJpaRepository.findByUsuarioIdOrLibroDuenioId(usuarioId).map { it.toDomain() }
        return reservasDomain
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
                .orElseThrow { NotFoundException("Libro ${dto.libroId} no encontrado") }

            val solicitante = usuarioJpaRepository.findById(dto.usuarioId)
                .orElseThrow { NotFoundException("Usuario ${dto.usuarioId} no encontrado") }

            val reservaDomain = Reserva(
                libro = libro,
                usuario = solicitante,
                fechaDesde = dto.fechaDesde,
                fechaHasta = dto.fechaHasta,
            )

            reservaDomain.confirmar()
            libro.actualizoDisponibilidad(false)

            libroJpaRepository.save(libro)

            solicitante.bibliokarmas = solicitante.bibliokarmas
            usuarioJpaRepository.save(solicitante)

            val reservaEntity = reservaDomain.toEntity(libro, solicitante)
            val savedReserva = reservaJpaRepository.save(reservaEntity)

            return savedReserva.toDomain()
        }
    }
}