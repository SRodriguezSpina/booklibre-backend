package ar.edu.unsam.phm.repository.mongo

import ar.edu.unsam.phm.domain.Libro
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface LibroMongoRepository : MongoRepository<Libro, ObjectId> {
    @Query("{ 'id' : ?0, 'activo' : true }")
    fun findByIdAndActivoTrue(id: Int): Libro?
    @Query("{ 'id' : ?0 }")
    fun findByBusinessId(id: Int): Libro?
    @Query("{ 'duenio.id' : ?0 }")
    fun findByDuenioId(duenioId: Int): List<Libro>
    @Query("{ 'duenio.id' : ?0, 'activo' : true }")
    fun findByDuenioIdAndActivoTrue(duenioId: Int): List<Libro>
    @Query(value = "{ '_class': ?0 }", count = true)
    fun contarPorTipo(tipo: String): Long
}
