package ar.edu.unsam.phm.dtos

data class LibroDTO(
    val id: Int,
    val titulo: String,
    val autor: String,
    val tipo: String, // "Común", "Con dedicatoria", "Coleccionable"
    val paginas: Int,
    val estado: String,
    val propietarioId: Int,
    val alquiladoPorId: Int?,
    val fechaDevolucion: String?,
    val bibliokarma: Int? // Opcional para mostrar el karma que dio/dará
)