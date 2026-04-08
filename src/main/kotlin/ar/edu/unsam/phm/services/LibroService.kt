package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse

interface LibroService {
    fun search(
        query: String,
        generos: String,
        page: Int,
        size: Int,
        usuarioId: Int?,
        paginasMin: Int?,
        paginasMax: Int?,
        fechaDesde: String?,
        fechaHasta: String?,
        isbn: String?,
        prestadoPor: String?,
        sortBy: String?,
    ): PagedResponse<LibroHomeDTO>

    fun getLibro(id: Int): LibroDTO
    fun getRating(libroId: Int): Double
    fun getCalificaciones(libroId: Int): List<CalificacionDTO>
    fun crearLibro(dto: LibroDTO, usuarioId: Int): LibroDTO
    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): LibroDTO
    fun agregarCalificacion(idLibro: Int, calificacion: Calificacion)
    fun eliminarLibro(libroId: Int, usuarioId: Int)
}
