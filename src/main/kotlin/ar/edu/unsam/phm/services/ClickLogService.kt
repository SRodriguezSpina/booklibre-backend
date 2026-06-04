package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.ClickLog
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.exceptions.NotFoundException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class ClickLogDocument(
    val libroId: Int,
    val libroTitulo: String,
    val nombreUsuario: String,
    val fechaHora: LocalDateTime,
)

data class LibroMasClickeadoResult(
    val libroId: Int,
    val libroTitulo: String,
    val total: Long,
)

@Service
class ClickLogService(
    private val libroRepository: LibroMongoRepository,
    private val mongoTemplate: MongoTemplate,
    private val usuarioJpaRepository: UsuarioJpaRepository,
    private val libroService: LibroService,
) {

    fun esEmailDelDuenio(duenioId: Int, email: String): Boolean =
        usuarioJpaRepository.findById(duenioId).orElse(null)?.email == email

    /**
     * Registra que un usuario entro al detalle de un libro.
     * Se llama desde LibroController.getLibro() cuando hay usuario autenticado.
     */
    fun registrarClick(libroId: Int, libroTitulo: String, nombreUsuario: String) {
        val ahora = LocalDateTime.now()
        val limiteDedupe = ahora.minusSeconds(5)
        val query = Query(
            Criteria.where("libroId").`is`(libroId)
                .and("clicks").not().elemMatch(
                    Criteria.where("nombreUsuario").`is`(nombreUsuario)
                        .and("fechaHora").gt(limiteDedupe)
                )
        )
        val update = Update().push("clicks", ClickLog(fechaHora = ahora, nombreUsuario = nombreUsuario))
        val result = mongoTemplate.updateFirst(query, update, "libros")

        if (result.matchedCount > 0) {
            libroService.actualizarCacheTopLibros()
            return
        }

        val existeLibro = mongoTemplate.exists(
            Query(Criteria.where("libroId").`is`(libroId)),
            "libros"
        )
        if (!existeLibro) throw NotFoundException("Libro $libroId no encontrado")
    }

    /**
     * Dante 1 - Query: libro mas clickeado.
     * Devuelve null si todavia no hay ningun click registrado.
     */
    fun getLibroMasClickeado(): LibroMasClickeadoResult? {
        val aggregation = Aggregation.newAggregation(
            Aggregation.project()
                .andExclude("_id")
                .and("id").`as`("libroId")
                .and("titulo").`as`("libroTitulo")
                .and("clicks").size().`as`("total"),
            Aggregation.sort(Sort.Direction.DESC, "total"),
            Aggregation.limit(1)
        )
        val results = mongoTemplate.aggregate(aggregation, "libros", LibroMasClickeadoResult::class.java)
        val result = results.uniqueMappedResult
        return if (result != null && result.total > 0) result else null
    }

    /**
     * Caso de uso bonus: todos los clicks sobre los libros de un publicador.
     * Se usa para mostrar la seccion de clicks en el perfil del publicador.
     */
    fun getClicksDeLibrosDeUsuario(usuarioId: Int): List<ClickLogDocument> {
        val libros = libroRepository.getTodosLosLibrosDeUsuarioConClicks(usuarioId)
        return libros.flatMap { libro ->
            libro.clicks.map { click ->
                ClickLogDocument(
                    libroId = libro.id,
                    libroTitulo = libro.titulo,
                    nombreUsuario = click.nombreUsuario,
                    fechaHora = click.fechaHora
                )
            }
        }.sortedByDescending { it.fechaHora }
    }
}
