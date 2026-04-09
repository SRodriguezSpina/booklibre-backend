package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.EntityNotFoundException
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("!jpa")
class ReservaServiceInMemory(
    private val libroRepository: LibroRepository,
    private val usuarioRepository: UsuarioRepository,
    private val reservaRepository: ReservaRepository
) : ReservaService {
    private val locks = java.util.concurrent.ConcurrentHashMap<Int, Any>()

    override fun obtenerReservas(usuarioId: Int): List<ReservaDTO> {
        usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario $usuarioId no encontrado")
        val reservas = reservaRepository.getReservasDeUsuario(usuarioId)
        return reservas.map {
            val cantidadReservasDelLibro = reservaRepository.findByLibroId(it.libro.id).size
            val bibliokarma = it.libro.calcularBiblioKarmaTotal(
                usuario = it.usuario,
                desde = it.fechaDesde,
                hasta = it.fechaHasta,
                cantidadReservasDelLibro = cantidadReservasDelLibro,
            )
            it.toDTO(bibliokarma)
        }
    }

    override fun crearReserva(dto: CrearReservaDTO): Reserva {
        val lock = locks.computeIfAbsent(dto.libroId) { Any() }
        synchronized(lock) {
            if (reservaRepository.existeSuperposicion(dto.libroId, dto.fechaDesde, dto.fechaHasta)) {
                throw ConflictException("El libro ya está reservado para esas fechas.")
            }
            val libro = try {
                libroRepository.getById(dto.libroId)
            } catch (_: EntityNotFoundException) {
                throw NotFoundException("Libro ${dto.libroId} no encontrado")
            }
            val solicitante = usuarioRepository.getById(dto.usuarioId)
            val reserva = reservaRepository.create(
                Reserva(libro, solicitante, dto.fechaDesde, dto.fechaHasta)
            )
            reserva.confirmar()
            libro.actualizoDisponibilidad(false)
            usuarioRepository.update(solicitante)
            if (libro.duenio.id != solicitante.id) usuarioRepository.update(libro.duenio)
            return reserva
        }
    }
}