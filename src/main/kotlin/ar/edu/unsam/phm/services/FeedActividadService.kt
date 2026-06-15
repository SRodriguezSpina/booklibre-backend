package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.LibroAgregadoDTO
import ar.edu.unsam.phm.dtos.ReservaConfirmadaDTO
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

@Service
class FeedActividadService(
    private val libroMongoRepository: LibroMongoRepository,
    private val reservaJpaRepository: ReservaJpaRepository,
) {
    fun obtenerFeed(): List<Any> {
        val eventosLibros = libroMongoRepository.findTopNByFechaAgregadoDesc(5)
            .map { libro ->
                LibroAgregadoDTO(
                    libroId      = libro.id,
                    titulo       = libro.titulo,
                    autor        = libro.autor,
                    duenioNombre = libro.duenio.nombre,
                    fecha        = libro.fechaAgregado.format(FORMATTER),
                )
            }

        val reservas = reservaJpaRepository.findTopNRecentesConUsuario(PageRequest.of(0, 5))

        // Resolución de títulos: una única query a MongoDB con todos los libroIds
        val libroIds = reservas.map { it.libroId }.distinct()
        val titulosPorId = libroMongoRepository.findAllByIds(libroIds).associate { it.id to it.titulo }

        val eventosReservas = reservas.map { reserva ->
                ReservaConfirmadaDTO(
                    reservaId    = reserva.id,
                    libroId      = reserva.libroId,
                    libroTitulo  = titulosPorId[reserva.libroId] ?: "Libro #${reserva.libroId}",
                    usuarioId    = reserva.usuario.id,
                    usuarioNombre = reserva.usuario.nombre,
                    fecha        = reserva.fechaDesde.format(FORMATTER),
                )
            }

        return (eventosLibros + eventosReservas)
            .sortedByDescending { evento ->
                when (evento) {
                    is LibroAgregadoDTO    -> LocalDateTime.parse(evento.fecha, FORMATTER)
                    is ReservaConfirmadaDTO -> LocalDateTime.parse(evento.fecha, FORMATTER)
                    else -> LocalDateTime.MIN
                }
            }
            .take(5)
    }
}
