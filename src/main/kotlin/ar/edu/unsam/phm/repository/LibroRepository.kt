package ar.edu.unsam.phm.repository
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroFiltros
import ar.edu.unsam.phm.dtos.PagedResponse
import org.springframework.stereotype.Component
import kotlin.math.ceil

@Component
class LibroRepository: CommonRepository<Libro>() {
    fun getLibros(): List<Libro> = lista
    fun getLibrosDeUsuario(idUsuario: Int): List<Libro> {
        return lista.filter { idUsuario == it.duenio.id }
    }

    fun buscar(filtros: LibroFiltros): List<Libro> =
        lista
            .filter { filtros.excluirDuenioId == null || it.duenio.id != filtros.excluirDuenioId }
            .filter { it.cumpleFiltros(filtros) }
            .sortedBy { libro ->
                when (filtros.sortBy) {
                    "autor" -> libro.autor.lowercase()
                    "duenio" -> libro.duenio.nombre.lowercase()
                    else -> libro.titulo.lowercase()
                }

            }
    // Importante agregar el import de ar.edu.unsam.phm.dtos.PagedResponse
    // Importante agregar el import de kotlin.math.ceil

    fun buscarPaginado(filtros: LibroFiltros, page: Int, size: Int): PagedResponse<Libro> {
        val filtrados = buscar(filtros)

        val totalPages = ceil(filtrados.size.toDouble() / size).toInt()
        val content = filtrados.drop(page * size).take(size)

        return PagedResponse(content, page, totalPages)
    }

    fun search(value: String): List<Libro> {
        if (value.isBlank()) return lista

        return lista.filter { libro ->
            libro.titulo.contains(value, ignoreCase = true) ||
                    libro.autor.contains(value, ignoreCase = true)
        }
    }

    fun getByTitulo(nombre: String): Libro {
        return lista.find { it.titulo.equals(nombre, ignoreCase = true) }
            ?: throw EntityNotFoundException("No se encontró el libro con título: $nombre")
    }
}