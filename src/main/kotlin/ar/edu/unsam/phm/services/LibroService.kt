package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PrestamoDTO
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.stereotype.Service
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.mappers.toPrestamoDTO
import java.time.format.DateTimeFormatter

@Service
class LibroService(val libroRepository: LibroRepository) {

    fun getAll(): List<LibroHomeDTO> {
        return libroRepository.getLibros().map { it.toHomeDTO() }
    }

    fun getLibro(id: Int, usuarioId: Int): LibroDTO {
        val libro = libroRepository.getById(id)
        return libro.toDTO(usuarioId)
    }

    fun obtenerPrestamos(usuarioId: Int, tipo: String): List<PrestamoDTO> {
        val libros = libroRepository.getLibros()

        val filtrados = when (tipo) {
            "prestados-a-mi" -> libros.filter { libro ->
                libro.reservas.any { it.usuario.id == usuarioId }
            }
            "prestados-por-mi" -> libros.filter { it.duenio.id == usuarioId && it.reservas.isNotEmpty() }
            else -> emptyList()
        }

        return filtrados.map { it.toPrestamoDTO(usuarioId) }
    }
}