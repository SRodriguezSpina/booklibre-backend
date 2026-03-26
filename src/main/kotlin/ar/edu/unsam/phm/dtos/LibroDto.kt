package ar.edu.unsam.phm.dtos

import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.Genero
import java.time.LocalDate

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

data class LibroDTO(
    val id: Int,
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
    val propietarioId: Int,
    val imagenUrl: String,
    val estoyReservado: Boolean,
    val rating: Double,
    val cantidadReservas: Int,
    val alquiladoPorId: Int?,
    val bibliokarma: Int?,
    val reservas: List<RangoReservaDTO>,
    val cantidadCalificaciones: Int,
    val ultimasCalificaciones: List<CalificacionDTO>
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