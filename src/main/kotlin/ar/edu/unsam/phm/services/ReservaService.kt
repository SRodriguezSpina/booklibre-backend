package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.dtos.UsuarioConMasReservasDTO
import ar.edu.unsam.phm.dtos.UsuarioReservasDevueltasDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.mongo.CalificacionMongoRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ReservaService(
    private val libroRepository: LibroRepository,
    private val usuarioJpa: UsuarioJpaRepository,
    private val reservaJpa: ReservaJpaRepository,
    private val calificacionesMongo: CalificacionMongoRepository,
) {

    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()

    fun obtenerReservas(usuarioId: Int, tipo: TipoReserva): List<ReservaDTO> {
        usuarioJpa.findById(usuarioId).orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }
        val reservas = when (tipo) {
            TipoReserva.HECHAS    -> reservaJpa.findByUsuarioId(usuarioId)
            TipoReserva.RECIBIDAS -> {
                val librosDelUsuario = libroRepository.findLibroIdsByDuenioId(usuarioId)
                if (librosDelUsuario.isEmpty()) emptyList()
                else reservaJpa.findByLibroIdIn(librosDelUsuario)
            }
        }
        val librosPorId = libroRepository.findAllByIds(reservas.map { it.libroId }).associateBy { it.id }
        val ahora = LocalDateTime.now()
        return reservas.mapNotNull { reserva ->
            val libro = librosPorId[reserva.libroId] ?: return@mapNotNull null
            val estaDisponible = !reservaJpa.estaReservadoEnPeriodo(reserva.libroId, ahora, ahora)
            val yaCalificado = calificacionesMongo.existsByLibroIdAndUsuarioId(
                reserva.libroId, reserva.usuario.id,
            )
            reserva.toDTO(libro = libro, estaDisponible = estaDisponible, yaCalificadoPorUsuario = yaCalificado)
        }
    }

    fun obtenerIdsLibrosReservados(fechaDesdeStr: String?, fechaHastaStr: String?): Set<Int>? {
        if (fechaDesdeStr == null || fechaHastaStr == null) return null
        val desde = LocalDate.parse(fechaDesdeStr).atStartOfDay()
        val hasta = LocalDate.parse(fechaHastaStr).atTime(23, 59, 59)
        if (!desde.isBefore(hasta)) {
            throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
        }
        return reservaJpa.obtenerIdsLibrosReservados(desde, hasta)
    }

    fun obtenerReservasAnuales(usuarioId: Int): List<ReservaAnualUsuarioDTO> {
        usuarioJpa.findById(usuarioId).orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }
        return reservaJpa.findReservasAnualesByUsuarioId(usuarioId).map {
            ReservaAnualUsuarioDTO(
                reservaId     = it.reservaId,
                usuarioId     = it.usuarioId,
                usuarioNombre = it.usuarioNombre,
                libroId       = it.libroId,
                libroTitulo   = it.libroTitulo,
                libroAutor    = it.libroAutor,
                fechaDesde    = it.fechaDesde.toLocalDate(),
                fechaHasta    = it.fechaHasta.toLocalDate(),
                anioReserva   = it.anioReserva,
            )
        }
    }

    fun obtenerUsuariosConReservasDevueltas(): List<UsuarioReservasDevueltasDTO> =
        reservaJpa.findUsuariosConReservasDevueltas().map {
            UsuarioReservasDevueltasDTO(
                usuarioId                = it.usuarioId,
                usuarioNombre            = it.usuarioNombre,
                cantidadReservasDevueltas = it.cantidadReservasDevueltas,
            )
        }

    @Transactional(readOnly = true)
    fun obtenerUsuariosConMasDeNReservas(minReservas: Int): List<UsuarioConMasReservasDTO> =
        reservaJpa.findUsuariosConMasDeNReservas(minReservas).map {
            UsuarioConMasReservasDTO(
                usuarioId       = it.usuarioId,
                usuarioNombre   = it.usuarioNombre,
                cantidadReservas = it.cantidadReservas,
            )
        }

    @Transactional
    fun crearReserva(dto: CrearReservaDTO): Reserva {
        if (dto.fechaDesde.isBefore(LocalDateTime.now())) {
            throw BusinessException("La fecha de inicio no puede ser en el pasado")
        }
        if (!dto.fechaHasta.isAfter(dto.fechaDesde)) {
            throw BusinessException("La fecha de fin debe ser posterior a la fecha de inicio")
        }

        val lock = locks.computeIfAbsent(dto.libroId) { Any() }
        synchronized(lock) {
            val libro = libroRepository.getById(dto.libroId)
            val solicitante = usuarioJpa.findById(dto.usuarioId)
                .orElseThrow { NotFoundException("Usuario ${dto.usuarioId} no encontrado") }

            if (libro.duenio.id == solicitante.id) {
                throw BusinessException("No podés reservar tu propio libro")
            }

            // Disponibilidad: el libro no puede reservarse si ya existe una reserva
            // que se solape con el período solicitado
            if (reservaJpa.estaReservadoEnPeriodo(dto.libroId, dto.fechaDesde, dto.fechaHasta)) {
                throw BusinessException("El libro ya está reservado en ese período")
            }

            val reserva = Reserva(
                libroId    = libro.id,
                usuario    = solicitante,
                fechaDesde = dto.fechaDesde,
                fechaHasta = dto.fechaHasta,
            )
            reserva.confirmar(libro)

            libro.duenio.sumaLibroReservado()
            solicitante.sumaLibroLeido()
            usuarioJpa.save(libro.duenio)
            usuarioJpa.save(solicitante)
            return reservaJpa.save(reserva)
        }
    }

    // Scheduled para limpiar locks de libros liberados (sin efecto en reservas ya persistidas)
    @Scheduled(fixedDelay = 60_000)
    fun limpiarLocks() {
        locks.clear()
    }
}