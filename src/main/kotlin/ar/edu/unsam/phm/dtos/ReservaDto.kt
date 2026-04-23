package ar.edu.unsam.phm.dtos
import java.time.LocalDate

data class CrearReservaDTO(
    val libroId: Int,
    val usuarioId: Int,
    val fechaDesde: LocalDate,
    val fechaHasta: LocalDate
)

data class ReservaDTO(
    val id: Int,
    val prestamoId: Int,
    val titulo: String,
    val autor: String,
    val estado: String,
    val propietarioId: Int,
    val imagenUrl: String,
    val alquiladoPorId: Int?,
    val fechaDesde: String?,
    val fechaHasta: String?,
    val rating: Double,
    val bibliokarma: Int?,
    val disponibilidad: Boolean?,
    val libroActivo: Boolean
)

data class ReservaAnualUsuarioDTO(
    val reservaId: Int,
    val usuarioId: Int,
    val usuarioNombre: String,
    val libroId: Int,
    val libroTitulo: String,
    val libroAutor: String,
    val fechaDesde: LocalDate,
    val fechaHasta: LocalDate,
    val anioReserva: Int,
)

data class UsuarioReservasDevueltasDTO(
    val usuarioId: Int,
    val usuarioNombre: String,
    val cantidadReservasDevueltas: Int?
)

data class UsuarioConMasReservasDTO(
    val usuarioId: Int,
    val usuarioNombre: String,
    val cantidadReservas: Long
)