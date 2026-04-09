package ar.edu.unsam.phm.services.jpa

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.mappers.toEntity
import ar.edu.unsam.phm.persistence.repositories.LibroJpaRepository
import ar.edu.unsam.phm.persistence.repositories.ReservaJpaRepository
import ar.edu.unsam.phm.persistence.repositories.UsuarioJpaRepository
import ar.edu.unsam.phm.services.ReservaService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Profile("jpa")
class ReservaServiceJpa(
    private val libroJpaRepository: LibroJpaRepository,
    private val usuarioJpaRepository: UsuarioJpaRepository,
    private val reservaJpaRepository: ReservaJpaRepository
) : ReservaService {

    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()

    override fun obtenerReservas(usuarioId: Int): List<ReservaDTO> {
        usuarioJpaRepository.findById(usuarioId)
            .orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }

        val reservasDomain = reservaJpaRepository.findByUsuarioIdOrLibroDuenioId(usuarioId).map { it.toDomain() }

        return reservasDomain.map { reservaDomain ->
            val cantidadReservasDelLibro = reservaJpaRepository.findByLibroId(reservaDomain.libro.id).size
            val bibliokarma = reservaDomain.libro.calcularBiblioKarmaTotal(
                usuario = reservaDomain.usuario,
                desde = reservaDomain.fechaDesde,
                hasta = reservaDomain.fechaHasta,
                cantidadReservasDelLibro = cantidadReservasDelLibro,
            )
            reservaDomain.toDTO(bibliokarma)
        }
    }

    @Transactional
    override fun crearReserva(dto: CrearReservaDTO): Reserva {
        val lock = locks.computeIfAbsent(dto.libroId) { Any() }
        synchronized(lock) {
            if (reservaJpaRepository.existsSuperposicion(dto.libroId, dto.fechaDesde, dto.fechaHasta)) {
                throw ConflictException("El libro ya está reservado para esas fechas.")
            }

            val libroEntity = libroJpaRepository.findById(dto.libroId)
                .orElseThrow { NotFoundException("Libro ${dto.libroId} no encontrado") }

            val solicitanteEntity = usuarioJpaRepository.findById(dto.usuarioId)
                .orElseThrow { NotFoundException("Usuario ${dto.usuarioId} no encontrado") }

            val libroDomain = libroEntity.toDomain()
            val solicitanteDomain = solicitanteEntity.toDomain()

            val reservaDomain = Reserva(
                libro = libroDomain,
                usuario = solicitanteDomain,
                fechaDesde = dto.fechaDesde,
                fechaHasta = dto.fechaHasta
            )

            reservaDomain.confirmar()
            libroDomain.actualizoDisponibilidad(false)

            libroEntity.disponibilidad = libroDomain.disponibilidad.name
            libroJpaRepository.save(libroEntity)

            solicitanteEntity.bibliokarmas = solicitanteDomain.bibliokarmas
            usuarioJpaRepository.save(solicitanteEntity)

            val reservaEntity = reservaDomain.toEntity(libroEntity, solicitanteEntity)
            val savedReserva = reservaJpaRepository.save(reservaEntity)

            return savedReserva.toDomain()
        }
    }
}