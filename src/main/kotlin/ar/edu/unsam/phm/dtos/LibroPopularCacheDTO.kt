package ar.edu.unsam.phm.dtos

import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma

data class LibroPopularCacheDTO(
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
    val duenio: String,
    val clicks: Int
) {
    fun toHomeDTO(bibliokarma: Int? = this.bibliokarma): LibroHomeDTO = LibroHomeDTO(
        id = id,
        imagenUrl = imagenUrl,
        genero = genero,
        titulo = titulo,
        autor = autor,
        calificacion = calificacion,
        isbn = isbn,
        idioma = idioma,
        tipo = tipo,
        bibliokarma = bibliokarma,
        estado = estado,
        duenio = duenio
    )
}
