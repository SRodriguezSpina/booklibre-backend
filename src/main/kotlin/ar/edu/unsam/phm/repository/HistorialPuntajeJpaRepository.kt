package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface HistorialPuntajeJpaRepository : JpaRepository<HistorialPuntaje, Int> {
    @Query("""
        SELECT h FROM HistorialPuntaje h
        WHERE h.libroId = :libroId
        ORDER BY h.fechaActualizacion DESC
    """)
    fun getHistorialPuntajes(@Param("libroId") libroId: Int): List<HistorialPuntaje>
}