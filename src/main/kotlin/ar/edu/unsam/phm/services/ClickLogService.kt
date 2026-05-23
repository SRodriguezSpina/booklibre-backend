package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.ClickLog
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import ar.edu.unsam.phm.exceptions.NotFoundException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class ClickLogDocument(
    val libroId: Int,
    val libroTitulo: String,
    val nombreUsuario: String,
    val fechaHora: LocalDateTime,
)

data class LibroMasClickeadoResult(
    val id: Int,          // libroId (from projected id)
    val libroTitulo: String,
    val total: Long,
)

@Service
class ClickLogService(
    private val libroRepository: LibroMongoRepository,
    private val mongoTemplate: MongoTemplate,
) {

    /**
     * Registra que un usuario entro al detalle de un libro.
     * Se llama desde LibroController.getLibro() cuando hay usuario autenticado.
     */
    fun registrarClick(libroId: Int, libroTitulo: String, nombreUsuario: String) {
        val ahora = LocalDateTime.now()
        val limiteDedupe = ahora.minusSeconds(5)
        val libro = libroRepository.getByIdIncluyendoInactivosCompleto(libroId)
            ?: throw NotFoundException("Libro $libroId no encontrado")
        val yaExiste = libro.clicks.any {
            it.nombreUsuario == nombreUsuario && it.fechaHora.isAfter(limiteDedupe)
        }
        if (yaExiste) return
        libro.clicks.add(ClickLog(fechaHora = ahora, nombreUsuario = nombreUsuario))
        libroRepository.save(libro)
    }

    /**
     * Integrante 1 - Query: libro mas clickeado.
     * Devuelve null si todavia no hay ningun click registrado.
     */
    fun getLibroMasClickeado(): LibroMasClickeadoResult? {
        val aggregation = Aggregation.newAggregation(
            Aggregation.project("id")
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
     * Bonus: todos los clicks sobre los libros de un publicador.
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
