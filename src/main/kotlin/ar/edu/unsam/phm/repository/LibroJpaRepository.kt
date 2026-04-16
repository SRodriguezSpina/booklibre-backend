package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Libro
import org.springframework.data.jpa.repository.JpaRepository

interface LibroJpaRepository : JpaRepository<Libro, Int> {
    fun findByDuenioId(duenioId: Int): List<Libro>
}