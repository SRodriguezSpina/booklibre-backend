package ar.edu.unsam.phm.services
import ar.edu.unsam.phm.Calificacion
import ar.edu.unsam.phm.EstadoLibro
import ar.edu.unsam.phm.LibroColeccionable
import ar.edu.unsam.phm.LibroComun
import ar.edu.unsam.phm.LibroConDedicatoria
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.*
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.stereotype.Service
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.UsuarioRepository
import java.time.LocalDate
import kotlin.math.ceil

@Service
class LibroService(val libroRepository: LibroRepository, private val usuarioRepository: UsuarioRepository) {
    fun search(query: String, generos: String, page: Int, size: Int, usuarioId: Int, paginasMin: Int?, paginasMax: Int?,
               fechaDesde: String?, fechaHasta: String?, isbn: String?, prestadoPor: String?): PagedResponse<LibroHomeDTO> {
        val listaGeneros = if (generos.isBlank()) emptyList() else generos.split(",")

        val filtrados = libroRepository.getLibros()
            .filter { it.duenio.id != usuarioId } //para excluir los libros que son de el
            .filter { libro ->
                val matchQuery = query.isBlank() ||
                        libro.titulo.contains(query, ignoreCase = true) ||
                        libro.autor.contains(query, ignoreCase = true)

                val matchGenero = listaGeneros.isEmpty() ||
                        listaGeneros.contains(libro.genero.name)

                val matchPaginasMin = paginasMin == null || libro.cantidadPaginas >= paginasMin
                val matchPaginasMax = paginasMax == null || libro.cantidadPaginas <= paginasMax

                val matchIsbn = isbn.isNullOrBlank() ||
                        libro.isbn.contains(isbn, ignoreCase = true)

                val matchPrestadoPor = prestadoPor.isNullOrBlank() ||
                        libro.duenio.nombre.contains(prestadoPor, ignoreCase = true)

                val matchFechas = if (fechaDesde != null && fechaHasta != null) {
                    val desde = LocalDate.parse(fechaDesde)
                    val hasta = LocalDate.parse(fechaHasta)
                    val reservasDelLibro = usuarioRepository.lista
                        .flatMap { it.reservas }
                        .filter { it.libro.id == libro.id }
                    reservasDelLibro.none { reserva ->
                        reserva.fechaDesde <= hasta && reserva.fechaHasta >= desde
                    }
                } else true

                matchQuery && matchGenero && matchPaginasMin && matchPaginasMax &&
                        matchIsbn && matchPrestadoPor && matchFechas
            }
            .map { it.toHomeDTO() }

        val totalPages = ceil(filtrados.size.toDouble() / size).toInt()
        val content = filtrados.drop(page * size).take(size)

        return PagedResponse(content, page, totalPages)
    }
    fun getLibro(id: Int): LibroDTO {
        val libro = libroRepository.getById(id)
        return libro.toDTO()
    }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): LibroDTO {
        val duenio = usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario no encontrado")

        val libro = when (dto.tipo) {
            "Comun" -> LibroComun(
                duenio, 0, dto.titulo, dto.autor, dto.descripcion,
                dto.genero, dto.paginas, dto.isbn, dto.idioma,
                dto.editorial, dto.fechaPublicacion, EstadoLibro.valueOf(dto.estado), dto.imagenUrl
            )
            "ConDedicatoria" -> LibroConDedicatoria(
                duenio, 0, dto.titulo, dto.autor, dto.descripcion,
                dto.genero, dto.paginas, dto.isbn, dto.idioma,
                dto.editorial, dto.fechaPublicacion, EstadoLibro.valueOf(dto.estado), dto.imagenUrl
            )
            "Coleccionable" -> LibroColeccionable(
                duenio, 0, dto.titulo, dto.autor, dto.descripcion,
                dto.genero, dto.paginas, dto.isbn, dto.idioma,
                dto.editorial, dto.fechaPublicacion, EstadoLibro.valueOf(dto.estado), dto.imagenUrl
            )
            else -> throw BusinessException("Tipo de libro inválido: ${dto.tipo}")
        }

        libroRepository.create(libro)
        return libro.toDTO()
    }

    fun actualizarLibro(id : Int, dto: LibroDTO) : LibroDTO{
        val libroExistente = libroRepository.getById(id)

        libroExistente.titulo = dto.titulo
        libroExistente.autor = dto.autor
        libroExistente.descripcion = dto.descripcion
        libroExistente.genero = dto.genero
        libroExistente.cantidadPaginas = dto.paginas
        libroExistente.isbn = dto.isbn
        libroExistente.idioma = dto.idioma
        libroExistente.editorial = dto.editorial
        libroExistente.estado = EstadoLibro.valueOf(dto.estado)
        libroExistente.fechaPublicacion = dto.fechaPublicacion
        libroExistente.imagenUrl = dto.imagenUrl

        libroRepository.update(libroExistente)
        return libroExistente.toDTO()
    }

    fun agregarCalificacion(idLibro: Int, calificacion: Calificacion) {
        val libro = libroRepository.getById(idLibro)
        libro.agregarCalificacion(calificacion)
        libroRepository.update(libro)
    }
}