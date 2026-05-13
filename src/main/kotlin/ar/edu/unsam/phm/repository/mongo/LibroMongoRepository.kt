package ar.edu.unsam.phm.repository.mongo

import ar.edu.unsam.phm.domain.Libro
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface LibroMongoRepository : MongoRepository<Libro, ObjectId> {
    fun findByIdAndActivoTrue(id: Int): Libro?
    @Query("{ 'id' : ?0 }")
    fun findByBusinessId(id: Int): Libro?
    fun findByDuenioId(duenioId: Int): List<Libro>
    fun findByDuenioIdAndActivoTrue(duenioId: Int): List<Libro>
}
