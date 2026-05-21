package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.CalificacionDocument
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroFiltros
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.readmodels.CalificacionDetalleView
import ar.edu.unsam.phm.repository.mongo.CalificacionMongoRepository
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import org.springframework.stereotype.Component
import kotlin.math.ceil

@Component
class LibroRepository(
    private val mongo: LibroMongoRepository,
    private val calificacionesMongo: CalificacionMongoRepository,
    private val usuarioJpa: UsuarioJpaRepository,
    private val historialJpa: HistorialPuntajeJpaRepository,
) {

    private data class CalificacionDetalleMongoView(
        override val usuarioId: Int,
        override val nombreUsuario: String,
        override val valor: Int,
        override val comentario: String,
        override val totalCalificaciones: Long,
    ) : CalificacionDetalleView

    fun getById(id: Int): Libro =
        mongo.findByIdAndActivoTrue(id)
            ?: throw NotFoundException("Libro $id no encontrado")

    fun getByIdIncluyendoInactivos(id: Int): Libro =
        mongo.findByBusinessId(id)
            ?: throw NotFoundException("Libro $id no encontrado")

    fun getLibros(): List<Libro> = mongo.findAll().filter { it.activo }

    fun getTodosLosLibrosDeUsuario(usuarioId: Int): List<Libro> =
        mongo.findByDuenioId(usuarioId)

    fun getLibrosDeUsuario(usuarioId: Int): List<Libro> =
        mongo.findByDuenioIdAndActivoTrue(usuarioId)

    fun save(libro: Libro): Libro {
        if (libro.id == 0) {
            libro.id = (mongo.findAll().maxOfOrNull { it.id } ?: 0) + 1
        }
        return mongo.save(libro)
    }

    fun delete(libro: Libro) {
        val persisted = mongo.findByBusinessId(libro.id)
            ?: throw NotFoundException("Libro ${libro.id} no encontrado")
        mongo.delete(persisted)
    }

    fun buscarPaginado(filtros: LibroFiltros, page: Int, size: Int): PagedResponse<Libro> {
        val query = filtros.query.trim().lowercase()

        var items = mongo.findAll().asSequence()
            .filter { it.activo }
            .filter { libro ->
                query.isBlank() ||
                    libro.titulo.lowercase().contains(query) ||
                    libro.autor.lowercase().contains(query) ||
                    libro.descripcion.lowercase().contains(query)
            }

        if (filtros.generos.isNotEmpty()) {
            val generos = filtros.generos.map { it.trim().uppercase() }.toSet()
            items = items.filter { it.genero.name in generos }
        }
        filtros.paginasMin?.let { min -> items = items.filter { it.cantidadPaginas >= min } }
        filtros.paginasMax?.let { max -> items = items.filter { it.cantidadPaginas <= max } }
        filtros.isbn?.takeIf { it.isNotBlank() }?.let { isbn ->
            items = items.filter { it.isbn == isbn }
        }
        filtros.prestadoPor?.takeIf { it.isNotBlank() }?.let { ownerName ->
            val needle = ownerName.lowercase()
            items = items.filter { it.duenio.nombre.lowercase().contains(needle) }
        }
        filtros.excluirDuenioId?.let { ownerId ->
            items = items.filter { it.duenio.id != ownerId }
        }
        filtros.libroIdsReservados?.takeIf { it.isNotEmpty() }?.let { ids ->
            items = items.filterNot { it.id in ids }
        }

        val sorted = when (filtros.sortBy.lowercase()) {
            "autor" -> items.sortedBy { it.autor.lowercase() }
            "calificacion" -> items.sortedByDescending { it.calificacion }
            else -> items.sortedBy { it.titulo.lowercase() }
        }.toList()

        val total = sorted.size
        val from = (page * size).coerceAtMost(total)
        val to = (from + size).coerceAtMost(total)
        val totalPages = if (total == 0) 0 else ceil(total.toDouble() / size).toInt()
        return PagedResponse(sorted.subList(from, to), page, totalPages)
    }

    fun getHistorialPuntajes(libroId: Int): List<HistorialPuntaje> =
        historialJpa.getHistorialPuntajes(libroId)

    fun getCalificacionesDetalle(libroId: Int, limite: Int? = 2): List<CalificacionDetalleView> {
        val total = calificacionesMongo.countByLibroId(libroId)
        val todas = calificacionesMongo.findByLibroIdOrderByFechaDesc(libroId)
        val seleccion = if (limite == null) todas else todas.take(limite)
        return seleccion.map { cal ->
            CalificacionDetalleMongoView(
                usuarioId = cal.usuarioId,
                nombreUsuario = usuarioJpa.findById(cal.usuarioId).orElse(null)?.nombre
                    ?: "Usuario ${cal.usuarioId}",
                valor = cal.valor,
                comentario = cal.comentario,
                totalCalificaciones = total,
            )
        }
    }

    fun usuarioYaCalifico(libroId: Int, usuarioId: Int): Boolean =
        calificacionesMongo.existsByLibroIdAndUsuarioId(libroId, usuarioId)

    fun agregarCalificacion(libro: Libro, calificacion: Calificacion): Libro {
        calificacionesMongo.save(
            CalificacionDocument(
                libroId = libro.id,
                usuarioId = calificacion.usuarioId,
                valor = calificacion.valor,
                comentario = calificacion.comentario,
                fecha = calificacion.fecha,
            ),
        )

        val todas = calificacionesMongo.findByLibroIdOrderByFechaDesc(libro.id)
        val promedio = if (todas.isEmpty()) 0.0 else todas.map { it.valor }.average()

        libro.agregarCalificacion(calificacion)
        libro.actualizarPromedioCalificacion(promedio)

        return save(libro)
    }

    fun actualizarActivoPorDuenio(usuarioId: Int, activo: Boolean) {
        val libros = mongo.findByDuenioId(usuarioId)
        libros.forEach { it.activo = activo }
        if (libros.isNotEmpty()) mongo.saveAll(libros)
    }

    fun liberarReservasVencidas(now: java.time.LocalDateTime): Int = 0

    fun contarLibrosCollecionables(): Long =
        mongo.contarPorTipo("ar.edu.unsam.phm.domain.LibroColeccionable")
}