package ar.edu.unsam.phm.services
import ar.edu.unsam.phm.Calificacion
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.stereotype.Service
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.UsuarioRepository

@Service
class LibroService(val libroRepository: LibroRepository, private val usuarioRepository: UsuarioRepository) {
    fun search(query: String, generos: String, page: Int, size: Int, usuarioId: Int): PagedResponse<LibroHomeDTO> {
        val listaGeneros = if (generos.isBlank()) emptyList() else generos.split(",")

        val filtrados = libroRepository.getLibros()
            .filter { it.duenio.id != usuarioId } //para excluir los libros que son dee el duhh
            .filter { libro ->
                val matchQuery = query.isBlank() ||
                        libro.titulo.contains(query, ignoreCase = true) ||
                        libro.autor.contains(query, ignoreCase = true)

                val matchGenero = listaGeneros.isEmpty() ||
                        listaGeneros.contains(libro.genero.name)

                matchQuery && matchGenero
            }
            .map { it.toHomeDTO() }

        val totalPages = Math.ceil(filtrados.size.toDouble() / size).toInt()
        val content = filtrados.drop(page * size).take(size)

        return PagedResponse(content, page, totalPages)
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