package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.LibroColeccionable
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroConDedicatoria
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.mappers.toDetalleDTOs
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.ceil
@Service
class LibroService(
    private val libroRepository: LibroRepository,
    private val usuarioRepository: UsuarioRepository,
    private val reservaRepository: ReservaRepository,
) {

    fun search(
        query: String,
        generos: String,
        page: Int,
        size: Int,
        usuarioId: Int?,
        paginasMin: Int?,
        paginasMax: Int?,
        fechaDesde: String?,
        fechaHasta: String?,
        isbn: String?,
        prestadoPor: String?,
        sortBy: String?,
    ): PagedResponse<LibroHomeDTO> {
        if (paginasMin != null && paginasMax != null && paginasMin >= paginasMax)
            throw BusinessException("El mínimo de páginas debe ser menor al máximo")
        if (fechaDesde != null && fechaHasta != null) {
            val desde = LocalDate.parse(fechaDesde)
            val hasta = LocalDate.parse(fechaHasta)
            if (!desde.isBefore(hasta))
                throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
        }

        val usuario = if (usuarioId != null) usuarioRepository.findById(usuarioId) else null
        val listaGeneros = if (generos.isBlank()) emptyList() else generos.split(",")

        val filtrados = libroRepository.getLibros()
            .filter { usuario == null || it.duenio.id != usuarioId }
            .filter { libro ->
                val matchQuery = query.isBlank() ||
                        libro.titulo.contains(query, ignoreCase = true) ||
                        libro.autor.contains(query, ignoreCase = true)
                val matchGenero = listaGeneros.isEmpty() || listaGeneros.contains(libro.genero.name)
                val matchPaginasMin = paginasMin == null || libro.cantidadPaginas >= paginasMin
                val matchPaginasMax = paginasMax == null || libro.cantidadPaginas <= paginasMax
                val matchIsbn = isbn.isNullOrBlank() || libro.isbn.contains(isbn, ignoreCase = true)
                val matchPrestadoPor = prestadoPor.isNullOrBlank() ||
                        libro.duenio.nombre.contains(prestadoPor, ignoreCase = true)
                val matchFechas = if (fechaDesde != null && fechaHasta != null) {
                    val desde = LocalDate.parse(fechaDesde)
                    val hasta = LocalDate.parse(fechaHasta)
                    reservaRepository.getReservasDeLibro(libro.id).none { r ->
                        r.fechaDesde <= hasta && r.fechaHasta >= desde
                    }
                } else true

                matchQuery && matchGenero && matchPaginasMin && matchPaginasMax &&
                        matchIsbn && matchPrestadoPor && matchFechas
            }
            .sortedBy { libro ->
                when (sortBy) {
                    "autor"  -> libro.autor.lowercase()
                    "duenio" -> libro.duenio.nombre.lowercase()
                    else     -> libro.titulo.lowercase()
                }
            }
            .map { libro ->
                val bibliokarma = if (usuario != null) {
                    val cantidadReservas = reservaRepository.getReservasDeLibro(libro.id).size
                    libro.plusBiblioKarma(usuario, cantidadReservas)
                } else 0
                libro.toHomeDTO(bibliokarma)
            }

        val totalPages = ceil(filtrados.size.toDouble() / size).toInt()
        return PagedResponse(filtrados.drop(page * size).take(size), page, totalPages)
    }

    fun getLibro(id: Int): LibroDTO {
        val libro = libroRepository.getById(id)
        val reservas = reservaRepository.getReservasDeLibro(id)
        val nombresUsuarios = usuarioRepository.resolverNombresCalificaciones(libro.calificaciones)
        return libro.toDTO(todosLosRangos = reservas, nombresUsuarios = nombresUsuarios)
    }

    fun getRating(libroId: Int): Double =
        libroRepository.getById(libroId).getCalificacion()

    fun getCalificaciones(libroId: Int): List<CalificacionDTO> {
        val libro = libroRepository.getById(libroId)
        val nombres = usuarioRepository.resolverNombresCalificaciones(libro.calificaciones)
        return libro.calificaciones.toDetalleDTOs(nombres)
    }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): LibroDTO {
        val duenio = usuarioRepository.getById(usuarioId)
        val data = LibroData(
            duenio          = duenio,
            titulo          = dto.titulo,
            autor           = dto.autor,
            descripcion     = dto.descripcion,
            genero          = dto.genero,
            cantidadPaginas = dto.paginas,
            isbn            = dto.isbn,
            idioma          = dto.idioma,
            editorial       = dto.editorial,
            fechaPublicacion = dto.fechaPublicacion,
            estado          = EstadoLibro.valueOf(dto.estado),
            imagenUrl       = dto.imagenUrl,
            fechaAgregado   = LocalDate.now(),
            disponibilidad  = Disponibilidad.DISPONIBLE,
        )
        val libro = when (dto.tipo) {
            "Comun"         -> LibroComun(data)
            "ConDedicatoria" -> LibroConDedicatoria(data)
            "Coleccionable" -> LibroColeccionable(data)
            else -> throw BusinessException("Tipo de libro inválido: ${dto.tipo}")
        }
        return libroRepository.save(libro).toDTO()
    }

    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): LibroDTO {
        val libro = libroRepository.getById(id)
        if (libro.duenio.id != usuarioId)
            throw UnauthorizedException("No autorizado para editar este libro")
        libro.actualizarDatos(
            nuevoTitulo          = dto.titulo,
            nuevoAutor           = dto.autor,
            nuevaDescripcion     = dto.descripcion,
            nuevoGenero          = dto.genero,
            nuevasPaginas        = dto.paginas,
            nuevoIsbn            = dto.isbn,
            nuevoIdioma          = dto.idioma,
            nuevaEditorial       = dto.editorial,
            nuevaFechaPublicacion = dto.fechaPublicacion,
            nuevoEstado          = EstadoLibro.valueOf(dto.estado),
            nuevaImagenUrl       = dto.imagenUrl,
        )
        return libroRepository.save(libro).toDTO()
    }

    fun agregarCalificacion(idLibro: Int, calificacion: Calificacion) {
        val libro = libroRepository.getById(idLibro)
        usuarioRepository.getById(calificacion.usuarioId)
        libro.agregarCalificacion(calificacion)
        libroRepository.save(libro)
    }

    fun eliminarLibro(libroId: Int, usuarioId: Int) {
        val libro = libroRepository.getById(libroId)
        if (libro.duenio.id != usuarioId)
            throw UnauthorizedException("No autorizado para eliminar este libro")
        if (reservaRepository.existeReservaActiva(libroId, LocalDate.now()))
            throw ConflictException("El libro tiene una reserva activa y no puede eliminarse")
        libroRepository.delete(libro)
    }
}