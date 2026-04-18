package ar.edu.unsam.phm.dtos

import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.Genero
import java.time.LocalDate
import java.time.LocalDateTime

data class RangoReservaDTO(
    val fechaDesde: String,
    val fechaHasta: String
)

data class CalificacionDTO(
    val usuarioId: Int,
    val nombreUsuario: String,
    val valor: Int,
    val comentario: String
)
data class NewCalificacionDTO(
    val usuarioId: Int,
    val valor: Int,
    val comentario: String
)

data class LibroDTO(
    val id: Int = 0,
    val titulo: String,
    val tipo: String,
    val descripcion: String,
    val genero: Genero,
    val autor: String,
    val paginas: Int,
    val isbn: String,
    val idioma: Idioma,
    val editorial: String,
    val estado: String,
    val fechaPublicacion: LocalDate,
    val fechaAgregado: LocalDate = LocalDate.now(),
    val propietarioId: Int = 0,
    val imagenUrl: String = "",
    val estoyReservado: Boolean = false,
    val rating: Double = 0.0,
    val cantidadReservas: Int = 0,
    val alquiladoPorId: Int? = null,
    val bibliokarma: Int? = null,
    val reservas: List<RangoReservaDTO> = emptyList(),
    val cantidadCalificaciones: Int = 0,
    val ultimasCalificaciones: List<CalificacionDTO> = emptyList()
)

data class LibroHomeDTO(
    val id: Int,
    val imagenUrl: String,
    val genero: Genero,
    val titulo: String,
    val autor: String,
    val calificacion: Double,
    val isbn: String,
    val idioma: Idioma,
    val tipo: String,
    val bibliokarma: Int?,
    val estado: String,
    val duenio: String
)

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val totalPages: Int
)

data class HistorialPuntajeDTO(
    val fecha: LocalDateTime,
    val anterior: Int?,
    val nuevo: Int,
    val usuarioId: Int
)