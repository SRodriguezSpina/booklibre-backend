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
        jpa.findById(id).orElseThrow { NotFoundException("Libro.kt $id no encontrado") }

    fun getLibros(): List<Libro> = jpa.findAll()

    fun getLibrosDeUsuario(usuarioId: Int): List<Libro> =
        jpa.findByDuenioId(usuarioId)

    fun save(libro: Libro): Libro = jpa.save(libro)

    fun delete(libro: Libro) = jpa.deleteById(libro.id)

    fun buscar(filtros: LibroFiltros): List<Libro> =
        getLibros()
            .filter { filtros.excluirDuenioId == null || it.duenio.id != filtros.excluirDuenioId }
            .filter { it.cumpleFiltros(filtros) }
            .sortedBy { libro ->
                when (filtros.sortBy) {
                    "autor"  -> libro.autor.lowercase()
                    "duenio" -> libro.duenio.nombre.lowercase()
                    else     -> libro.titulo.lowercase()
                }
            }

    fun buscarPaginado(filtros: LibroFiltros, page: Int, size: Int): PagedResponse<Libro> {
        val filtrados = buscar(filtros)
        val totalPages = ceil(filtrados.size.toDouble() / size).toInt()
        return PagedResponse(filtrados.drop(page * size).take(size), page, totalPages)
    }
    fun getHistorialPuntajes(libroId: Int): List<HistorialPuntaje> =
        historialJpa.findByLibroIdOrderByFechaActualizacionDesc(libroId)

    fun getCalificacionesDetalle(libroId: Int, limite: Int = 2): List<CalificacionDetalleView> =
        jpa.findCalificacionesDetalle(libroId, limite)
}