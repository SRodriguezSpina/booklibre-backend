package ar.edu.unsam.phm.testing.inmemory.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.testing.inmemory.repository.EntityNotFoundException
import ar.edu.unsam.phm.testing.inmemory.repository.LibroRepository
import ar.edu.unsam.phm.testing.inmemory.repository.ReservaRepository
import ar.edu.unsam.phm.testing.inmemory.repository.UsuarioRepository
import java.time.LocalDate

class ReservaServiceInMemory(
    private val libroRepository: LibroRepository,
    private val usuarioRepository: UsuarioRepository,
    private val reservaRepository: ReservaRepository,
) {
    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()

    fun obtenerReservas(usuarioId: Int, tipo: TipoReserva): List<Reserva> {
        usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario $usuarioId no encontrado")
        val reservas = reservaRepository.getReservasDeUsuario(usuarioId)
        return when (tipo) {
            TipoReserva.HECHAS -> reservas.filter { it.usuario.id == usuarioId }
            TipoReserva.RECIBIDAS -> reservas.filter { it.libro.duenio.id == usuarioId }
        }
    }

    fun obtenerReservasAnuales(usuarioId: Int): List<ReservaAnualUsuarioDTO> {
        usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario $usuarioId no encontrado")

        val anioActual = LocalDate.now().year
        return reservaRepository.getReservasDeUsuario(usuarioId)
            .filter { it.usuario.id == usuarioId && it.fechaDesde.year == anioActual }
            .sortedBy { it.fechaDesde }
            .map {
                ReservaAnualUsuarioDTO(
                    reservaId = it.id,
                    usuarioId = it.usuario.id,
                    usuarioNombre = it.usuario.nombre,
                    libroId = it.libro.id,
                    libroTitulo = it.libro.titulo,
                    libroAutor = it.libro.autor,
                    fechaDesde = it.fechaDesde,
                    fechaHasta = it.fechaHasta,
                    anioReserva = it.fechaDesde.year,
                )
            }
    }

    fun crearReserva(dto: CrearReservaDTO): Reserva {
        val lock = locks.computeIfAbsent(dto.libroId) { Any() }
        synchronized(lock) {
            val libro = try {
                libroRepository.getById(dto.libroId)
            } catch (_: EntityNotFoundException) {
                throw NotFoundException("Libro.kt ${dto.libroId} no encontrado")
            }
            val solicitante = usuarioRepository.getById(dto.usuarioId)

            if (libro.duenio.id == solicitante.id) {
                throw BusinessException("No podés reservar tu propio libro")
            }

            if (!libro.estaDisponible()) {
                throw ConflictException("El libro no está disponible para préstamo")
            }

            val reserva = reservaRepository.create(
                Reserva(libro, solicitante, dto.fechaDesde, dto.fechaHasta),
            )
            reserva.confirmar()
            libro.actualizoDisponibilidad(false)
            usuarioRepository.update(solicitante)
            return reserva
        }
    }
}