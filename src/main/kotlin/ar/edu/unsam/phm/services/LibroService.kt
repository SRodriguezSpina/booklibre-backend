package ar.edu.unsam.phm.services
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.stereotype.Service
import ar.edu.unsam.phm.mappers.toDTO

@Service
class LibroService(val libroRepository: LibroRepository) {
    fun getLibro(id: Int, usuarioId: Int): LibroDTO {
        val libro = libroRepository.getById(id)
        return libro.toDTO(usuarioId)
    }
}