package ar.edu.unsam.phm.repository.mongo

import ar.edu.unsam.phm.domain.mongo.ClickLogDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository

interface ClickLogMongoRepository : MongoRepository<ClickLogDocument, ObjectId> {

    // Todos los clicks de un libro específico
    fun findByLibroIdOrderByFechaHoraDesc(libroId: Int): List<ClickLogDocument>

    // Clicks de varios libros a la vez (para el perfil del publicador)
    fun findByLibroIdInOrderByFechaHoraDesc(libroIds: List<Int>): List<ClickLogDocument>

    /**
     * Consulta MongoDB — Integrante 1: libro más clickeado
     *
     * Equivale a:
     *   db.click_logs.aggregate([
     *     { $group: { _id: "$libroId", libroTitulo: { $first: "$libroTitulo" }, total: { $sum: 1 } } },
     *     { $sort: { total: -1 } },
     *     { $limit: 1 }
     *   ])
     */
    @Aggregation(pipeline = [
        "{ \$group: { _id: '\$libroId', libroTitulo: { \$first: '\$libroTitulo' }, total: { \$sum: 1 } } }",
        "{ \$sort: { total: -1 } }",
        "{ \$limit: 1 }"
    ])
    fun findLibroMasClickeado(): LibroMasClickeadoResult?
}

data class LibroMasClickeadoResult(
    val id: Int,          // libroId
    val libroTitulo: String,
    val total: Long,
)
