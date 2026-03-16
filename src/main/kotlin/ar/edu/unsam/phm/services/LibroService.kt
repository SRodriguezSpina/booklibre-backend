package ar.edu.unsam.phm.services
import ar.edu.unsam.phm.Calificacion
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.stereotype.Service
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.UsuarioRepository

@Service
class LibroService(val libroRepository: LibroRepository, private val usuarioRepository: UsuarioRepository) {
    fun getAll(): List<LibroHomeDTO> {
        return libroRepository.getLibros().map { it.toHomeDTO() }
    }
    fun getLibro(id: Int, usuarioId: Int): LibroDTO {
        val libro = libroRepository.getById(id)
        return libro.toDTO(usuarioId)
    }
    fun agregarCalificacion(idLibro: Int, calificacion: Calificacion) {
        val libro = libroRepository.getById(idLibro)
        libro.agregarCalificacion(calificacion)
        libroRepository.update(libro) // O save, dependiendo de tu repo
    }
}