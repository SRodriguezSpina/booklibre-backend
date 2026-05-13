package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HistorialPuntajeJpaRepository : JpaRepository<HistorialPuntaje, Int> {
    // Para ver el historial de un libro específico ordenado por fecha
    fun getHistorialPuntajes(libroId: Int): List<HistorialPuntaje>
}