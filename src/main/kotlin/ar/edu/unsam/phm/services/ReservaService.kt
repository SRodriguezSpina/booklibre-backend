package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.ReservaFecha
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
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ReservaService(
    private val libroRepository: LibroMongoRepository,
    private val usuarioJpa: UsuarioJpaRepository,
    private val reservaJpa: ReservaJpaRepository,
) {

    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()

    fun obtenerReservas(usuarioId: Int, tipo: TipoReserva, pagina: Int = 0, tamanio: Int = 10): PagedResponse<ReservaDTO> {
        usuarioJpa.findById(usuarioId).orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }
        val paginacion = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaDesde"))
        val paginaReservas = when (tipo) {
            TipoReserva.HECHAS    -> reservaJpa.findByUsuarioId(usuarioId, paginacion)
            TipoReserva.RECIBIDAS -> {
                val librosDelUsuario = libroRepository.findLibroIdsByDuenioId(usuarioId)
                if (librosDelUsuario.isEmpty()) return PagedResponse(emptyList(), pagina, 0, 0L)
                reservaJpa.findByLibroIdIn(librosDelUsuario, paginacion)
            }
        }
        val reservas = paginaReservas.content
        val idsLibros = reservas.map { it.libroId }
        val librosPorId = libroRepository.findAllByIds(idsLibros).associateBy { it.id }
        val ahora = LocalDateTime.now()
        // Batch: one query for all reserved libro IDs right now
        val librosReservadosAhora = reservaJpa.obtenerIdsLibrosReservados(ahora, ahora)
        val contenido = reservas.mapNotNull { reserva ->
            val libro = librosPorId[reserva.libroId] ?: return@mapNotNull null
            val estaDisponible = reserva.libroId !in librosReservadosAhora
            val yaCalificado = libroRepository.usuarioYaCalifico(
                reserva.libroId, reserva.usuario.id,
            )
            reserva.toDTO(
                libro = libro,
                estaDisponible = estaDisponible,
                yaCalificadoPorUsuario = yaCalificado,
            )
        }
        val totalPaginas = if (paginaReservas.totalElements == 0L) 0 else paginaReservas.totalPages
        return PagedResponse(contenido, pagina, totalPaginas, paginaReservas.totalElements)
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
        val usuario = usuarioJpa.findById(usuarioId).orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }
        val reservas = reservaJpa.findByUsuarioId(usuarioId).sortedBy { it.fechaDesde }
        val libroIds = reservas.map { it.libroId }.distinct()
        val librosPorId = libroRepository.findAllByIds(libroIds).associateBy { it.id }

        return reservas.map { reserva ->
            val libro = librosPorId[reserva.libroId]
            ReservaAnualUsuarioDTO(
                reservaId     = reserva.id,
                usuarioId     = usuario.id,
                usuarioNombre = usuario.nombre,
                libroId       = reserva.libroId,
                libroTitulo   = libro?.titulo ?: "Desconocido",
                libroAutor    = libro?.autor ?: "Desconocido",
                fechaDesde    = reserva.fechaDesde.toLocalDate(),
                fechaHasta    = reserva.fechaHasta.toLocalDate(),
                anioReserva   = reserva.fechaDesde.year,
            )
        }
    }

    fun obtenerUsuariosConReservasDevueltas(): List<UsuarioReservasDevueltasDTO> =
        reservaJpa.findUsuariosConReservasDevueltas().map {
            UsuarioReservasDevueltasDTO(
                usuarioId                = it.usuarioId,
                usuarioNombre            = it.usuarioNombre,
                cantidadReservas = it.cantidadReservasDevueltas,
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
        val lock = locks.computeIfAbsent(dto.libroId) { Any() }
        synchronized(lock) {
            val libro = libroRepository.getByIdCompleto(dto.libroId)
                ?: throw NotFoundException("Libro ${dto.libroId} no encontrado")
            val solicitante = usuarioJpa.findById(dto.usuarioId)
                .orElseThrow { NotFoundException("Usuario ${dto.usuarioId} no encontrado") }

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

            val duenioJpa = usuarioJpa.findById(libro.duenio.id)
                .orElseThrow { NotFoundException("Usuario dueño ${libro.duenio.id} no encontrado") }
            duenioJpa.sumaLibroReservado()
            solicitante.sumaLibroLeido()
            usuarioJpa.save(duenioJpa)
            usuarioJpa.save(solicitante)

            // Guardar reserva en Mongo y limpiar las finalizadas/vencidas
            val ahora = LocalDateTime.now()
            libro.reservas.removeIf { it.fechaHasta.isBefore(ahora) }
            libro.reservas.add(ReservaFecha(dto.fechaDesde, dto.fechaHasta))
            libroRepository.save(libro)

            return reservaJpa.save(reserva)
        }
    }

    // Scheduled para limpiar locks de libros liberados (sin efecto en reservas ya persistidas)
    @Scheduled(fixedDelay = 60_000)
    fun limpiarLocks() {
        locks.clear()
    }
}