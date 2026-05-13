package ar.edu.unsam.phm.repository.mongo

import ar.edu.unsam.phm.domain.mongo.CalificacionDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface CalificacionMongoRepository : MongoRepository<CalificacionDocument, ObjectId> {
    fun findByLibroIdOrderByFechaDesc(libroId: Int): List<CalificacionDocument>
    fun findTop2ByLibroIdOrderByFechaDesc(libroId: Int): List<CalificacionDocument>
    fun countByLibroId(libroId: Int): Long
    fun existsByLibroIdAndUsuarioId(libroId: Int, usuarioId: Int): Boolean
}
