package ar.edu.unsam.phm.testing.inmemory.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.testing.inmemory.repository.EntityNotFoundException
import ar.edu.unsam.phm.testing.inmemory.repository.LibroRepository
import ar.edu.unsam.phm.testing.inmemory.repository.ReservaRepository
import ar.edu.unsam.phm.testing.inmemory.repository.UsuarioRepository
import java.time.LocalDateTime

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
            TipoReserva.RECIBIDAS -> reservas.filter {
                val libro = libroRepository.getById(it.libroId)
                libro.duenio.id == usuarioId
            }
        }
    }

    fun obtenerReservasAnuales(usuarioId: Int): List<ReservaAnualUsuarioDTO> {
        usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario $usuarioId no encontrado")

        val anioActual = LocalDateTime.now().year
        return reservaRepository.getReservasDeUsuario(usuarioId)
            .filter { it.usuario.id == usuarioId && it.fechaDesde.year == anioActual }
            .sortedBy { it.fechaDesde }
            .map {
                val libro = libroRepository.getById(it.libroId)
                ReservaAnualUsuarioDTO(
                    reservaId = it.id,
                    usuarioId = it.usuario.id,
                    usuarioNombre = it.usuario.nombre,
                    libroId = libro.id,
                    libroTitulo = libro.titulo,
                    libroAutor = libro.autor,
                    fechaDesde = it.fechaDesde.toLocalDate(),
                    fechaHasta = it.fechaHasta.toLocalDate(),
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

            val sePisa = reservaRepository.findByLibroId(libro.id).any {
                it.sePisaCon(dto.fechaDesde, dto.fechaHasta)
            }
            if (sePisa) {
                throw BusinessException("El libro ya está reservado en ese período")
            }

            val reserva = reservaRepository.create(
                Reserva(libro.id, solicitante, dto.fechaDesde, dto.fechaHasta),
            )
            reserva.confirmar(libro)
            usuarioRepository.update(solicitante)
            return reserva
        }
    }
}