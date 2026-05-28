package ar.edu.unsam.phm.repository.mongo

import ar.edu.unsam.phm.domain.CalificacionDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository

interface CalificacionMongoRepository : MongoRepository<CalificacionDocument, ObjectId> {
    fun findByLibroIdOrderByFechaDesc(libroId: Int): List<CalificacionDocument>
    fun findTop2ByLibroIdOrderByFechaDesc(libroId: Int): List<CalificacionDocument>
    fun countByLibroId(libroId: Int): Long
    fun existsByLibroIdAndUsuarioId(libroId: Int, usuarioId: Int): Boolean
    fun findByLibroIdInAndUsuarioId(libroIds: Collection<Int>, usuarioId: Int): List<CalificacionDocument>

    /**
     * Query: libros con promedio de calificación > 4 */
    @Aggregation(pipeline = [
        "{ \$group: { _id: '\$libroId', promedio: { \$avg: '\$valor' } } }",
        "{ \$match: { promedio: { \$gt: 4 } } }",
        "{ \$sort:  { promedio: -1 } }"
    ])
    fun findLibrosConPromedioMayorA4(): List<LibroPromedioResult>
}

data class LibroPromedioResult(
    val id: Int,         // libroId (mapeado desde _id del $group)
    val promedio: Double,
)
