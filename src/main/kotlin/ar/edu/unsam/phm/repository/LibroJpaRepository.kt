package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.persistence.entities.LibroEntity
import org.springframework.data.jpa.repository.JpaRepository

interface LibroJpaRepository : JpaRepository<LibroEntity, Int> {
    fun findByDuenioId(duenioId: Int): List<LibroEntity>
}