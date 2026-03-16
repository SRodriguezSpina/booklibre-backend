package ar.edu.unsam.phm.dtos

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
    val bibliokarma: Int?
)