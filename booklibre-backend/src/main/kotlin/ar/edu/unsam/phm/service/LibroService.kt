package ar.edu.unsam.phm.service
import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.stereotype.Service

@Service
class LibroService(private val libroRepository: LibroRepository) {
    fun getLibro(id: Int): Libro = libroRepository.getById(id)
}