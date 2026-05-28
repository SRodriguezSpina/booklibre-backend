package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.PagedResponse
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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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

    fun obtenerReservas(usuarioId: Int, tipo: TipoReserva, pagina: Int = 0, tamanio: Int = 10): PagedResponse<ReservaDTO> {
        usuarioJpa.findById(usuarioId).orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }
        val paginacion = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaDesde"))
        val paginaReservas = when (tipo) {
            TipoReserva.HECHAS    -> reservaJpa.findByUsuarioId(usuarioId, paginacion)
            TipoReserva.RECIBIDAS -> {
                val librosDelUsuario = libroRepository.findLibroIdsByDuenioId(usuarioId)
                if (librosDelUsuario.isEmpty()) return PagedResponse(emptyList(), page, 0, 0L)
                reservaJpa.findByLibroIdIn(librosDelUsuario, paginacion)
            }
        }
        val reservas = paginaReservas.content
        val idsLibros = reservas.map { it.libroId }
        val librosPorId = libroRepository.findAllByIds(idsLibros).associateBy { it.id }
        val ahora = LocalDateTime.now()
        // Batch: one query for all reserved libro IDs right now
        val librosReservadosAhora = reservaJpa.obtenerIdsLibrosReservados(ahora, ahora)
        // Batch: one MongoDB query for all already-rated libro IDs by this user
        val librosYaCalificados = calificacionesMongo
            .findByLibroIdInAndUsuarioId(idsLibros, usuarioId)
            .map { it.libroId }
            .toSet()
        val contenido = reservas.mapNotNull { reserva ->
            val libro = librosPorId[reserva.libroId] ?: return@mapNotNull null
            reserva.toDTO(
                libro = libro,
                estaDisponible = reserva.libroId !in librosReservadosAhora,
                yaCalificadoPorUsuario = reserva.libroId in librosYaCalificados,
            )
        }
        val totalPaginas = if (paginaReservas.totalElements == 0L) 0 else paginaReservas.totalPages
        return PagedResponse(contenido, page, totalPaginas, paginaReservas.totalElements)
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

            // Un usuario no puede reservar un libro si ya tiene una reserva activa sobre él
            if (reservaJpa.existsByLibroIdAndUsuarioIdAndFechaHastaAfter(dto.libroId, dto.usuarioId, LocalDateTime.now())) {
                throw BusinessException("Ya tenés una reserva activa para este libro")
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