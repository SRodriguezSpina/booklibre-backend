package ar.edu.unsam.phm.repository.mongo

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroFiltros
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.readmodels.CalificacionDetalleView
import ar.edu.unsam.phm.repository.HistorialPuntajeJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.bson.Document
import org.springframework.stereotype.Component
import java.util.regex.Pattern
import kotlin.math.ceil

@Component
class LibroMongoRepository(
    private val mongoTemplate: MongoTemplate,
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

    fun getById(id: Int): Libro? {
        val query = Query(Criteria.where("libroId").`is`(id).and("activo").`is`(true))
        query.fields().exclude("clicks").slice("calificaciones", -2)
        return mongoTemplate.findOne(query, Libro::class.java)
    }

    fun getByIdIncluyendoInactivos(id: Int): Libro? {
        val query = Query(Criteria.where("libroId").`is`(id))
        query.fields().exclude("clicks").slice("calificaciones", -2)
        return mongoTemplate.findOne(query, Libro::class.java)
    }

    fun getByIdCompleto(id: Int): Libro? =
        mongoTemplate.findOne(Query(Criteria.where("libroId").`is`(id).and("activo").`is`(true)), Libro::class.java)

    fun getByIdIncluyendoInactivosCompleto(id: Int): Libro? =
        mongoTemplate.findOne(Query(Criteria.where("libroId").`is`(id)), Libro::class.java)

    fun getLibros(): List<Libro> {
        val query = Query(Criteria.where("activo").`is`(true))
        query.fields().exclude("calificaciones").exclude("clicks").exclude("reservas")
        return mongoTemplate.find(query, Libro::class.java)
    }

    fun findAllByIds(ids: Collection<Int>): List<Libro> =
        if (ids.isEmpty()) emptyList() else findByIdIn(ids)

    fun getTodosLosLibrosDeUsuario(usuarioId: Int): List<Libro> =
        findByDuenioId(usuarioId)

    fun getTodosLosLibrosDeUsuarioConClicks(usuarioId: Int): List<Libro> {
        val query = Query(Criteria.where("duenio.id").`is`(usuarioId))
        query.fields().exclude("calificaciones").exclude("reservas")
        return mongoTemplate.find(query, Libro::class.java)
    }

    fun findLibroIdsByDuenioId(usuarioId: Int): List<Int> {
        val query = Query(Criteria.where("duenio.id").`is`(usuarioId))
        query.fields().include("id").exclude("_id")
        return mongoTemplate.find(query, org.bson.Document::class.java, "libros")
            .mapNotNull { it.getInteger("id") }
    }

    fun getLibrosDeUsuario(usuarioId: Int): List<Libro> =
        findByDuenioIdAndActivoTrue(usuarioId)

    fun findPaginadoByDuenioId(
        usuarioId: Int,
        filtro: String,
        ordenarPor: String,
        direccion: String,
        page: Int,
        size: Int,
    ): PagedResponse<Libro> {
        val ahora = java.time.LocalDateTime.now()
        val criteria = Criteria.where("duenio.id").`is`(usuarioId)
        when (filtro.lowercase()) {
            "disponibles" -> {
                criteria.and("reservas").not().elemMatch(
                    Criteria.where("fechaDesde").lte(ahora).and("fechaHasta").gte(ahora)
                )
            }
            "prestados"   -> {
                criteria.and("reservas").elemMatch(
                    Criteria.where("fechaDesde").lte(ahora).and("fechaHasta").gte(ahora)
                )
            }
        }

        val total = mongoTemplate.count(Query(criteria), Libro::class.java)
        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / size).toInt()
        val sortDir = if (direccion.lowercase() == "descendente") Sort.Direction.DESC else Sort.Direction.ASC

        val items = if (ordenarPor.lowercase() == "estado") {
            val pipeline = mutableListOf<AggregationOperation>()
            pipeline.add(Aggregation.match(criteria))
            
            val customProject = AggregationOperation { _ ->
                Document("\$addFields", Document("disponible", Document("\$eq", listOf(
                    Document("\$size", Document("\$filter", Document()
                        .append("input", Document("\$ifNull", listOf("\$reservas", emptyList<Any>())))
                        .append("as", "res")
                        .append("cond", Document("\$and", listOf(
                            Document("\$lte", listOf("\$\$res.fechaDesde", ahora)),
                            Document("\$gte", listOf("\$\$res.fechaHasta", ahora))
                        )))
                    )),
                    0
                ))))
            }
            pipeline.add(customProject)
            pipeline.add(Aggregation.sort(sortDir, "disponible").and(Sort.Direction.ASC, "titulo"))
            pipeline.add(Aggregation.skip((page * size).toLong()))
            pipeline.add(Aggregation.limit(size.toLong()))
            
            val agg = Aggregation.newAggregation(pipeline)
            mongoTemplate.aggregate(agg, Libro::class.java, Libro::class.java).mappedResults
        } else {
            val sortField = if (ordenarPor.lowercase() == "fecha") "fechaAgregado" else "titulo"
            val baseQuery = Query(criteria)
            baseQuery.fields().exclude("calificaciones").exclude("clicks")
            val pageRequest = PageRequest.of(page, size, Sort.by(sortDir, sortField))
            mongoTemplate.find(baseQuery.with(pageRequest), Libro::class.java)
        }

        return PagedResponse(items, page, totalPages, total)
    }

    @Synchronized
    private fun getNextSequence(seqName: String): Int {
        val query = Query(Criteria.where("_id").`is`(seqName))
        val update = org.springframework.data.mongodb.core.query.Update().inc("seq", 1)
        val options = org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true)
        
        val counter = mongoTemplate.findAndModify(query, update, options, Document::class.java, "database_sequences")
        if (counter == null) {
            val maxId = findTopByOrderByIdDesc()?.id ?: 0
            val doc = Document("_id", seqName).append("seq", maxId + 1)
            mongoTemplate.insert(doc, "database_sequences")
            return maxId + 1
        }
        return counter.getInteger("seq")
    }

    fun save(libro: Libro): Libro {
        if (libro.id == 0) {
            libro.id = getNextSequence("libros_sequence")
        }
        libro.libroId = libro.id
        return mongoTemplate.save(libro)
    }

    fun delete(libro: Libro) {
        mongoTemplate.remove(libro)
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
            criteria.and("libroId").nin(ids)
        }

        val sortField = when (filtros.sortBy.lowercase()) {
            "autor"        -> "autor"
            "calificacion" -> "calificacion"
            else           -> "titulo"
        }
        val sortDir = if (sortField == "calificacion") Sort.Direction.DESC else Sort.Direction.ASC

        val baseQuery = Query(criteria)
        baseQuery.fields().exclude("calificaciones").exclude("clicks").exclude("reservas")
        val total = mongoTemplate.count(baseQuery, Libro::class.java)

        val pageRequest = PageRequest.of(page, size, Sort.by(sortDir, sortField))
        val items = mongoTemplate.find(baseQuery.with(pageRequest), Libro::class.java)

        val totalPages = if (total == 0L) 0 else ceil(total.toDouble() / size).toInt()
        return PagedResponse(items, page, totalPages)
    }

    fun getHistorialPuntajes(libroId: Int): List<HistorialPuntaje> =
        historialJpa.getHistorialPuntajes(libroId)

    fun getCalificacionesDetalle(libroId: Int, limite: Int? = 2): List<CalificacionDetalleView> {
        val libro = getByIdIncluyendoInactivosCompleto(libroId) ?: return emptyList()
        val todas = libro.calificaciones.sortedByDescending { it.fecha }
        val total = todas.size.toLong()
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

    fun usuarioYaCalifico(libroId: Int, usuarioId: Int): Boolean {
        val libro = getByIdIncluyendoInactivosCompleto(libroId) ?: return false
        return libro.calificaciones.any { it.usuarioId == usuarioId }
    }

    fun agregarCalificacion(libro: Libro, calificacion: Calificacion): Libro {
        libro.calificaciones.add(calificacion)
        val promedio = if (libro.calificaciones.isEmpty()) 0.0 else libro.calificaciones.map { it.valor }.average()
        libro.actualizarPromedioCalificacion(promedio)
        return save(libro)
    }

    fun actualizarActivoPorDuenio(usuarioId: Int, activo: Boolean) {
        val query = Query(Criteria.where("duenio.id").`is`(usuarioId))
        val update = org.springframework.data.mongodb.core.query.Update().set("activo", activo)
        mongoTemplate.updateMulti(query, update, Libro::class.java)
    }

    fun actualizarNombreDuenio(usuarioId: Int, nuevoNombre: String) {
        val query = Query(Criteria.where("duenio.id").`is`(usuarioId))
        val update = org.springframework.data.mongodb.core.query.Update().set("duenio.nombre", nuevoNombre)
        mongoTemplate.updateMulti(query, update, Libro::class.java)
    }

    fun liberarReservasVencidas(now: java.time.LocalDateTime): Int = 0

    fun contarLibrosCollecionables(): Long =
        mongoTemplate.count(Query(Criteria.where("_class").`is`("ar.edu.unsam.phm.domain.LibroColeccionable")), Libro::class.java)

    fun getLibrosConPromedioMayorA4(): List<Libro> {
        val query = Query(Criteria.where("calificacion").gt(4.0).and("activo").`is`(true))
            .with(Sort.by(Sort.Direction.DESC, "calificacion"))
        query.fields().exclude("calificaciones").exclude("clicks").exclude("reservas")
        return mongoTemplate.find(query, Libro::class.java)
    }

    fun findLibrosConReservasCumplidas(ahora: java.time.LocalDateTime = java.time.LocalDateTime.now()): List<Libro> {
        val criteria = Criteria.where("activo").`is`(true)
            .and("reservas.0").exists(true)
            .and("reservas").not().elemMatch(
                Criteria.where("fechaHasta").gte(ahora)
            )
        val query = Query(criteria)
        query.fields().exclude("calificaciones").exclude("clicks").exclude("reservas")
        return mongoTemplate.find(query, Libro::class.java)
    }

    // Helper implementations targeting Spring Data Mongo equivalent behavior
    private fun findByIdAndActivoTrue(libroId: Int): Libro? =
        mongoTemplate.findOne(Query(Criteria.where("libroId").`is`(libroId).and("activo").`is`(true)), Libro::class.java)

    private fun findByBusinessId(libroId: Int): Libro? =
        mongoTemplate.findOne(Query(Criteria.where("libroId").`is`(libroId)), Libro::class.java)

    private fun findByDuenioId(duenioId: Int): List<Libro> {
        val query = Query(Criteria.where("duenio.id").`is`(duenioId))
        query.fields().exclude("calificaciones").exclude("clicks").exclude("reservas")
        return mongoTemplate.find(query, Libro::class.java)
    }

    private fun findByDuenioIdAndActivoTrue(duenioId: Int): List<Libro> {
        val query = Query(Criteria.where("duenio.id").`is`(duenioId).and("activo").`is`(true))
        query.fields().exclude("calificaciones").exclude("clicks").exclude("reservas")
        return mongoTemplate.find(query, Libro::class.java)
    }

    private fun findByIdIn(ids: Collection<Int>): List<Libro> {
        val query = Query(Criteria.where("libroId").`in`(ids))
        query.fields().exclude("calificaciones").exclude("clicks").exclude("reservas")
        return mongoTemplate.find(query, Libro::class.java)
    }

    private fun findTopByOrderByIdDesc(): Libro? =
        mongoTemplate.findOne(Query().with(Sort.by(Sort.Direction.DESC, "id")).limit(1), Libro::class.java)
}
