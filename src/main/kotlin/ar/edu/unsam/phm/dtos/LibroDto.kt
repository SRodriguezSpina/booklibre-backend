package ar.edu.unsam.phm.dtos

import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.Genero
import java.time.LocalDate
import java.time.LocalDateTime
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Size

// Bonus: historial de clicks sobre los libros de un publicador
data class ClickLogDTO(
    val libroId: Int,
    val libroTitulo: String,
    val nombreUsuario: String,
    val fechaHora: String,
)

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
    
    @field:NotBlank(message = "El título es obligatorio")
    @field:Size(max = 200, message = "El título no puede superar los 200 caracteres")
    val titulo: String,
    
    @field:NotBlank(message = "El tipo de libro es obligatorio")
    val tipo: String,
    
    @field:NotBlank(message = "La descripción es obligatoria")
    @field:Size(max = 2000, message = "La descripción no debe exceder 2000 caracteres")
    val descripcion: String,
    
    val genero: Genero,
    
    @field:NotBlank(message = "El autor es obligatorio")
    @field:Size(max = 150, message = "El autor no puede superar los 150 caracteres")
    val autor: String,
    
    @field:Positive(message = "La cantidad de páginas debe ser positiva")
    @field:Max(value = 9999, message = "La cantidad de páginas no puede superar 9999")
    val paginas: Int,
    
    @field:NotBlank(message = "El ISBN es obligatorio")
    @field:Size(min = 10, max = 13, message = "El ISBN debe tener entre 10 y 13 caracteres")
    val isbn: String,
    
    val idioma: Idioma,
    
    @field:NotBlank(message = "La editorial es obligatoria")
    @field:Size(max = 150, message = "La editorial no puede superar los 150 caracteres")
    val editorial: String,
    
    @field:NotBlank(message = "El estado es obligatorio")
    val estado: String,
    
    @field:PastOrPresent(message = "La fecha de publicación no puede ser en el futuro")
    val fechaPublicacion: LocalDate,
    val fechaAgregado: LocalDateTime = LocalDateTime.now(),
    val propietarioId: Int = 0,
    val imagenUrl: String = "",
    val estoyReservado: Boolean = false,
    val rating: Double = 0.0,
    val cantidadReservas: Int = 0,
    val alquiladoPorId: Int? = null,
    val bibliokarma: Int? = null,
    val reservas: List<RangoReservaDTO> = emptyList(),
    val miReservaFechaHasta: String? = null,
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
    val totalPages: Int,
    val totalElements: Long = 0L,
)

data class HistorialPuntajeDTO(
    val fecha: LocalDateTime,
    val anterior: Int?,
    val nuevo: Int,
    val usuarioId: Int
)