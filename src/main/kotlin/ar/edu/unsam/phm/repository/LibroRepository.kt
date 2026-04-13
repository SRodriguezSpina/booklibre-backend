package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Libro
import org.springframework.stereotype.Component
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.mappers.toEntity


@Component
class LibroRepository(private val jpa: LibroJpaRepository) {

    fun getById(id: Int): Libro =
        jpa.findById(id).orElseThrow { NotFoundException("Libro $id no encontrado") }
            .toDomain()

    fun getLibros(): List<Libro> = jpa.findAll().map { it.toDomain() }

    fun getLibrosDeUsuario(usuarioId: Int): List<Libro> =
        jpa.findByDuenioId(usuarioId).map { it.toDomain() }

    fun save(libro: Libro): Libro = jpa.save(libro.toEntity()).toDomain()

    fun delete(libro: Libro) = jpa.deleteById(libro.id)
}