package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Libro
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface LibroJpaRepository : JpaRepository<Libro, Int> {
    fun findByDuenioId(duenioId: Int): List<Libro>
    fun findAllByActivoTrue(): List<Libro>
    fun findByIdAndActivoTrue(id: Int): Optional<Libro>
    fun findByDuenioIdAndActivoTrue(usuarioId: Int): List<Libro>
}