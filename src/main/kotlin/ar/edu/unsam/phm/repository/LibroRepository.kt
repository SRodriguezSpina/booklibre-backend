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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import java.util.regex.Pattern
import kotlin.math.ceil

@Component
class LibroRepository(
    private val mongo: LibroMongoRepository,
    private val calificacionesMongo: CalificacionMongoRepository,
    private val usuarioJpa: UsuarioJpaRepository,
    private val historialJpa: HistorialPuntajeJpaRepository,
    private val mongoTemplate: MongoTemplate,
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

    fun findAllByIds(ids: Collection<Int>): List<Libro> =
        if (ids.isEmpty()) emptyList() else mongo.findByIdIn(ids)

    fun getTodosLosLibrosDeUsuario(usuarioId: Int): List<Libro> =
        mongo.findByDuenioId(usuarioId)

    fun findLibroIdsByDuenioId(usuarioId: Int): List<Int> {
        val query = Query(Criteria.where("duenio.id").`is`(usuarioId))
        query.fields().include("id").exclude("_id")
        return mongoTemplate.find(query, org.bson.Document::class.java, "libros")
            .mapNotNull { it.getInteger("id") }
    }

    fun getLibrosDeUsuario(usuarioId: Int): List<Libro> =
        mongo.findByDuenioIdAndActivoTrue(usuarioId)

    fun findPaginadoByDuenioId(
        usuarioId: Int,
        filtro: String,
        ordenarPor: String,
        direccion: String,
        page: Int,
        size: Int,
        librosReservadosAhora: Set<Int>,
    ): PagedResponse<Libro> {
        val criteria = Criteria.where("duenio.id").`is`(usuarioId)
        when (filtro.lowercase()) {
            "disponibles" -> if (librosReservadosAhora.isNotEmpty()) criteria.and("id").nin(librosReservadosAhora)
            "prestados"   -> criteria.and("id").`in`(
                if (librosReservadosAhora.isEmpty()) listOf(Int.MIN_VALUE) else librosReservadosAhora
            )
        }

        val sortField = when (ordenarPor.lowercase()) {
            "estado" -> "activo"
            "fecha"  -> "fechaAgregado"
            else     -> "titulo"
        }
        val sortDir = if (direccion.lowercase() == "descendente") Sort.Direction.DESC else Sort.Direction.ASC

        val baseQuery = Query(criteria)
        val total = mongoTemplate.count(baseQuery, Libro::class.java)
        val pageRequest = PageRequest.of(page, size, Sort.by(sortDir, sortField))
        val items = mongoTemplate.find(baseQuery.with(pageRequest), Libro::class.java)
        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / size).toInt()
        return PagedResponse(items, page, totalPages, total)
    }

    fun save(libro: Libro): Libro {
        if (libro.id == 0) {
            libro.id = (mongo.findTopByOrderByIdDesc()?.id ?: 0) + 1
        }
        return mongo.save(libro)
    }

    fun delete(libro: Libro) {
        val persisted = mongo.findByBusinessId(libro.id)
            ?: throw NotFoundException("Libro ${libro.id} no encontrado")
        mongo.delete(persisted)
    }

    fun buscarPaginado(filtros: LibroFiltros, page: Int, size: Int): PagedResponse<Libro> {
        val criteria = Criteria.where("activo").`is`(true)

        val texto = filtros.query.trim()
        if (texto.isNotBlank()) {
            val escaped = Pattern.quote(texto)
            criteria.orOperator(
                Criteria.where("titulo").regex(escaped, "i"),
                Criteria.where("autor").regex(escaped, "i"),
                Criteria.where("descripcion").regex(escaped, "i"),
            )
        }

        if (filtros.generos.isNotEmpty()) {
            criteria.and("genero").`in`(filtros.generos.map { it.trim().uppercase() })
        }
        filtros.paginasMin?.let { criteria.and("cantidadPaginas").gte(it) }
        filtros.paginasMax?.let { criteria.and("cantidadPaginas").lte(it) }
        filtros.isbn?.takeIf { it.isNotBlank() }?.let { criteria.and("isbn").`is`(it) }
        filtros.prestadoPor?.takeIf { it.isNotBlank() }?.let {
            criteria.and("duenio.nombre").regex(Pattern.quote(it), "i")
        }
        filtros.excluirDuenioId?.let { criteria.and("duenio.id").ne(it) }
        filtros.libroIdsReservados?.takeIf { it.isNotEmpty() }?.let { ids ->
            criteria.and("id").nin(ids)
        }

        val sortField = when (filtros.sortBy.lowercase()) {
            "autor"        -> "autor"
            "calificacion" -> "calificacion"
            else           -> "titulo"
        }
        val sortDir = if (sortField == "calificacion") Sort.Direction.DESC else Sort.Direction.ASC

        val baseQuery = Query(criteria)
        val total = mongoTemplate.count(baseQuery, Libro::class.java)

        val pageRequest = PageRequest.of(page, size, Sort.by(sortDir, sortField))
        val items = mongoTemplate.find(baseQuery.with(pageRequest), Libro::class.java)

        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / size).toInt()
        return PagedResponse(items, page, totalPages)
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

    fun getLibrosConPromedioMayorA4(): List<Libro> {
        val resultados = calificacionesMongo.findLibrosConPromedioMayorA4()
        if (resultados.isEmpty()) return emptyList()
        val libroIds = resultados.map { it.id }
        return mongo.findByIdIn(libroIds).filter { it.activo }
    }
}