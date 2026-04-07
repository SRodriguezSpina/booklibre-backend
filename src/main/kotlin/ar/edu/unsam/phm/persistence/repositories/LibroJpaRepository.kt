package ar.edu.unsam.phm.persistence.repositories

import ar.edu.unsam.phm.persistence.entities.LibroEntity
import org.springframework.data.jpa.repository.JpaRepository

interface LibroJpaRepository : JpaRepository<LibroEntity, Int>

