package ar.edu.unsam.phm.dtos

import ar.edu.unsam.phm.Genero
import ar.edu.unsam.phm.Idioma
import java.time.LocalDate

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
    val propietarioId: Int,
    val imagenUrl: String,
    val alquiladoPorId: Int?,
    val fechaDevolucion: String?,
    val bibliokarma: Int? // Opcional para mostrar el karma que dio/dará
)