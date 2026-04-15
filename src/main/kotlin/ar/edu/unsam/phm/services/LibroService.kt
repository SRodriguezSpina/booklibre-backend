package ar.edu.unsam.phm.services
import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.*
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import org.springframework.stereotype.Service
import ar.edu.unsam.phm.mappers.toDetalleDTOs
import java.time.LocalDate

@Service
class LibroService(val libroRepository: LibroRepository, private val usuarioRepository: UsuarioRepository, private val reservaRepository: ReservaRepository) {
    fun search(
        query: String, generos: String, page: Int, size: Int,
        usuarioId: Int?, paginasMin: Int?, paginasMax: Int?,
        fechaDesde: String?, fechaHasta: String?,
        isbn: String?, prestadoPor: String?, sortBy: String?
    ): PagedResponse<Libro> {
        val librosReservadosIds = reservaRepository.obtenerIdsLibrosReservados(fechaDesde, fechaHasta)

        val filtros = LibroFiltros.desdeRequest(
            query, generos, paginasMin, paginasMax, isbn,
            prestadoPor, usuarioId, librosReservadosIds, sortBy
        )

        return libroRepository.buscarPaginado(filtros, page, size)
    }

    fun calcularBiblioKarma(libro: Libro, usuarioId: Int?): Int {
        val usuario = usuarioId?.let { usuarioRepository.getById(it) } ?: return 0
        val cantidadReservas = reservaRepository.findByLibroId(libro.id).size
        return libro.plusBiblioKarma(usuario, cantidadReservas)
    }

    fun obtenerLibro(id: Int): Libro = libroRepository.getById(id)

    fun obtenerReservasDeLibro(libroId: Int): List<Reserva> =
        reservaRepository.findByLibroId(libroId)

    fun obtenerNombresUsuariosCalificaciones(libro: Libro): Map<Int, String> =
        usuarioRepository.resolverNombresCalificaciones(libro.calificaciones)

    private fun nombreUsuarioResenia(calificaciones: List<Calificacion>): Map<Int, String> {
        return calificaciones
            .map { it.usuarioId }
            .toSet()
            .associateWith { idCalificador ->
                usuarioRepository.getById(idCalificador).nombre
            }
    }

    fun getRating(libroId: Int): Double {
        val libro = libroRepository.getById(libroId)
        return libro.getCalificacion()
    }


    fun getCalificaciones(libroId: Int): List<CalificacionDTO> {
        val libro = libroRepository.getById(libroId)
        return libro.calificaciones.toDetalleDTOs(nombreUsuarioResenia(libro.calificaciones))
    }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): Libro {
        val duenio = usuarioRepository.getById(usuarioId)

        val data = LibroData(
            duenio = duenio,
            titulo = dto.titulo,
            autor = dto.autor,
            descripcion = dto.descripcion,
            genero = dto.genero,
            cantidadPaginas = dto.paginas,
            isbn = dto.isbn,
            idioma = dto.idioma,
            editorial = dto.editorial,
            fechaPublicacion = dto.fechaPublicacion,
            estado = EstadoLibro.fromString(dto.estado),
            imagenUrl = dto.imagenUrl,
            fechaAgregado = LocalDate.now(),
            disponibilidad = Disponibilidad.DISPONIBLE
        )

        val libro = when (dto.tipo) {
            "Comun" -> LibroComun(data)
            "ConDedicatoria" -> LibroConDedicatoria(data)
            "Coleccionable" -> LibroColeccionable(data)
            else -> throw BusinessException("Tipo de libro inválido: ${dto.tipo}")
        }
        libroRepository.create(libro)
        return libro
    }

    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): Libro {
        val libroExistente = libroRepository.getById(id)

        if (libroExistente.duenio.id != usuarioId) {
            throw UnauthorizedException("No autorizado para editar este libro")
        }

        libroExistente.actualizarDatos(
            nuevoTitulo = dto.titulo,
            nuevoAutor = dto.autor,
            nuevaDescripcion = dto.descripcion,
            nuevoGenero = dto.genero,
            nuevasPaginas = dto.paginas,
            nuevoIsbn = dto.isbn,
            nuevoIdioma = dto.idioma,
            nuevaEditorial = dto.editorial,
            nuevoEstado = EstadoLibro.fromString(dto.estado),
            nuevaFechaPublicacion = dto.fechaPublicacion,
            nuevaImagenUrl = dto.imagenUrl,
        )

        libroRepository.update(libroExistente)
        return libroExistente
    }

    fun agregarCalificacion(idLibro: Int, calificacion: Calificacion) {
        val libro = libroRepository.getById(idLibro)
        usuarioRepository.getById(calificacion.usuarioId)
        libro.agregarCalificacion(calificacion) // el BusinessException de duplicado ya está en el dominio
        libroRepository.update(libro)
    }

    fun eliminarLibro(libroId: Int, usuarioId: Int) {
        val libro = libroRepository.getById(libroId)

        if (libro.duenio.id != usuarioId) {
            throw UnauthorizedException("No autorizado para eliminar este libro")
        }

        val hoy = LocalDate.now()
        val hayReservaActiva = reservaRepository.findByLibroId(libroId)
            .any { !hoy.isBefore(it.fechaDesde) && !hoy.isAfter(it.fechaHasta) }

        if (hayReservaActiva) {
            throw ConflictException("El libro tiene una reserva activa y no puede eliminarse")
        }

        libroRepository.delete(libro)
    }
}