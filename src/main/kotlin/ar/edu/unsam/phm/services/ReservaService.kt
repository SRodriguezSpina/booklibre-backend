package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.UsuarioConMasReservasDTO
import ar.edu.unsam.phm.dtos.UsuarioReservasDevueltasDTO
import ar.edu.unsam.phm.repository.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

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

    fun obtenerIdsLibrosReservados(fechaDesdeStr: String?, fechaHastaStr: String?): Set<Int>? {
        if (fechaDesdeStr == null || fechaHastaStr == null) return null
        val desde = LocalDate.parse(fechaDesdeStr).atStartOfDay()
        val hasta = LocalDate.parse(fechaHastaStr).atTime(23, 59, 59)
        if (!desde.isBefore(hasta)) {
            throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
        }
        return reservaRepository.obtenerIdsLibrosReservados(desde, hasta)
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
                fechaDesde = it.fechaDesde.toLocalDate(),
                fechaHasta = it.fechaHasta.toLocalDate(),
                anioReserva = it.anioReserva,
            )
        }
    }

    fun obtenerUsuariosConReservasDevueltas(): List<UsuarioReservasDevueltasDTO> {
        return reservaRepository.findUsuariosConReservasDevueltas().map {
            UsuarioReservasDevueltasDTO(
                usuarioId = it.usuarioId,
                usuarioNombre = it.usuarioNombre,
                cantidadReservasDevueltas = it.cantidadReservasDevueltas
            )
        }
    }

    @Transactional(readOnly = true)
    fun obtenerUsuariosConMasDeNReservas(minReservas: Int): List<UsuarioConMasReservasDTO> =
        reservaRepository.findUsuariosConMasDeNReservas(minReservas).map {
            UsuarioConMasReservasDTO(
                usuarioId = it.usuarioId,
                usuarioNombre = it.usuarioNombre,
                cantidadReservas = it.cantidadReservas
            )
        }

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    fun liberarLibrosConReservaVencida() {
        libroRepository.liberarReservasVencidas(LocalDateTime.now())
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
            libro.duenio.sumaLibroReservado()
            solicitante.sumaLibroLeido()
            usuarioRepository.save(libro.duenio)
            usuarioRepository.save(solicitante)
            reservaRepository.save(reserva)
            return reserva
        }
    }
}