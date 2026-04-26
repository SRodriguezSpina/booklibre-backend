package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroFiltros
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.readmodels.CalificacionDetalleView
import org.springframework.stereotype.Component
import kotlin.math.ceil

@Component
class LibroRepository(private val jpa: LibroJpaRepository,
                      private val historialJpa: HistorialPuntajeJpaRepository) {

    fun getById(id: Int): Libro =
        jpa.findByIdAndActivoTrue(id)
            .orElseThrow { NotFoundException("Libro $id no encontrado") }

    fun getByIdIncluyendoInactivos(id: Int): Libro =
        jpa.findById(id)
            .orElseThrow { NotFoundException("Libro $id no encontrado") }

    fun getLibros(): List<Libro> = jpa.findAllByActivoTrue()

    fun getTodosLosLibrosDeUsuario(usuarioId: Int): List<Libro> =
        jpa.findByDuenioId(usuarioId)

    fun getLibrosDeUsuario(usuarioId: Int): List<Libro> =
        jpa.findByDuenioIdAndActivoTrue(usuarioId)

    fun save(libro: Libro): Libro = jpa.save(libro)

    fun delete(libro: Libro) = jpa.deleteById(libro.id)

    fun buscarPaginado(filtros: LibroFiltros, page: Int, size: Int): PagedResponse<Libro> {
        val resultados = jpa.buscarIdsPaginados(
            query       = filtros.query,
            generos     = filtros.generos.takeIf { it.isNotEmpty() }?.toTypedArray(),
            paginasMin  = filtros.paginasMin,
            paginasMax  = filtros.paginasMax,
            isbn        = filtros.isbn,
            prestadoPor = filtros.prestadoPor,
            excluirId   = filtros.excluirDuenioId,
            excluirIds  = filtros.libroIdsReservados?.toTypedArray(),
            sortBy      = filtros.sortBy,
            limit       = size,
            offset      = page * size,
        )
        val total       = resultados.firstOrNull()?.total ?: 0L
        val totalPages  = ceil(total.toDouble() / size).toInt()
        val ids         = resultados.map { it.libroId }
        val librosPorId = jpa.findAllById(ids).associateBy { it.id }
        return PagedResponse(ids.mapNotNull { librosPorId[it] }, page, totalPages)
    }

    fun getHistorialPuntajes(libroId: Int): List<HistorialPuntaje> =
        historialJpa.findByLibroIdOrderByFechaActualizacionDesc(libroId)

    fun getCalificacionesDetalle(libroId: Int, limite: Int = 2): List<CalificacionDetalleView> =
        jpa.findCalificacionesDetalle(libroId, limite)
}