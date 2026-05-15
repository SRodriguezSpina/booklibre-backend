package ar.edu.unsam.phm.repository.mongo

import ar.edu.unsam.phm.domain.Calificacion
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface CalificacionMongoRepository : MongoRepository<Calificacion, ObjectId> {
    fun findByLibroIdOrderByFechaDesc(libroId: Int): List<Calificacion>
    fun findTop2ByLibroIdOrderByFechaDesc(libroId: Int): List<Calificacion>
    fun countByLibroId(libroId: Int): Long
    fun existsByLibroIdAndUsuarioId(libroId: Int, usuarioId: Int): Boolean
}
