package ar.edu.unsam.phm.services
import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.LibroColeccionable
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroConDedicatoria
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.*
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.EntityNotFoundException
import org.springframework.stereotype.Service
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.repository.UsuarioRepository
import java.time.LocalDate
import kotlin.math.ceil

@Service
class LibroService(val libroRepository: LibroRepository, private val usuarioRepository: UsuarioRepository) {
    fun search(query: String, generos: String, page: Int, size: Int, usuarioId: Int, paginasMin: Int?, paginasMax: Int?,
               fechaDesde: String?, fechaHasta: String?, isbn: String?, prestadoPor: String?): PagedResponse<LibroHomeDTO> {
        usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario $usuarioId no encontrado")

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
        val libro  = try {
            libroRepository.getById(id)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro $id no encontrado")
        }
        return libro.toDTO()
    }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): LibroDTO {
        val duenio = usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario no encontrado")

        val libro = when (dto.tipo) {
            "Comun" -> LibroComun(
                duenio, 0, dto.titulo, dto.autor, dto.descripcion,
                dto.autorImg, dto.genero, dto.paginas, dto.isbn, dto.idioma,
                dto.editorial, dto.fechaPublicacion, EstadoLibro.valueOf(dto.estado), dto.imagenUrl
            )
            "ConDedicatoria" -> LibroConDedicatoria(
                duenio, 0, dto.titulo, dto.autor, dto.descripcion,
                dto.autorImg, dto.genero, dto.paginas, dto.isbn, dto.idioma,
                dto.editorial, dto.fechaPublicacion, EstadoLibro.valueOf(dto.estado), dto.imagenUrl
            )
            "Coleccionable" -> LibroColeccionable(
                duenio, 0, dto.titulo, dto.autor, dto.descripcion,
                dto.autorImg, dto.genero, dto.paginas, dto.isbn, dto.idioma,
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
        val libro = try {
            libroRepository.getById(idLibro)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro $idLibro no encontrado")
        }
        usuarioRepository.findById(calificacion.usuarioId)
            ?: throw NotFoundException("Usuario ${calificacion.usuarioId} no encontrado")
        libro.agregarCalificacion(calificacion) // el BusinessException de duplicado ya está en el dominio
        libroRepository.update(libro)
    }

    fun eliminarLibro(libroId: Int, usuarioId: Int) {
        val libro = try {
            libroRepository.getById(libroId)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro no encontrado")
        }

        if (libro.duenio.id != usuarioId) {
            throw UnauthorizedException("No autorizado para eliminar este libro")
        }

        val hoy = LocalDate.now()
        val hayReservaActiva = usuarioRepository.lista
            .flatMap { it.reservas }
            .any { it.libro.id == libroId && !hoy.isBefore(it.fechaDesde) && !hoy.isAfter(it.fechaHasta) }

        if (hayReservaActiva) {
            throw ConflictException("El libro tiene una reserva activa y no puede eliminarse")
        }

        libroRepository.delete(libro)
    }
}