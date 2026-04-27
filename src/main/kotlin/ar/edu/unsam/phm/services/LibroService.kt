package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.NewCalificacionDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LibroService(
    private val libroRepository: LibroRepository,
    private val usuarioRepository: UsuarioRepository,
    private val reservaRepository: ReservaRepository,
) {
    fun search(
        query: String, generos: String, page: Int, size: Int,
        usuarioId: Int?, paginasMin: Int?, paginasMax: Int?,
        fechaDesde: String?, fechaHasta: String?,
        isbn: String?, prestadoPor: String?, sortBy: String?,
    ): PagedResponse<Libro> {
        val librosReservadosIds = reservaRepository.obtenerIdsLibrosReservados(fechaDesde, fechaHasta)
        val filtros = LibroFiltros.desdeRequest(
            query, generos, paginasMin, paginasMax, isbn,
            prestadoPor, usuarioId, librosReservadosIds, sortBy,
        )
        return libroRepository.buscarPaginado(filtros, page, size)
    }

    fun calcularBiblioKarma(libro: Libro, usuarioId: Int?): Int {
        val usuario = usuarioId?.let { usuarioRepository.findById(it) } ?: return 0
        val cantidadReservas = reservaRepository.findByLibroId(libro.id).size
        return libro.plusBiblioKarma(usuario, cantidadReservas)
    }

    fun obtenerLibro(id: Int): Libro = libroRepository.getById(id)

    fun obtenerReservasDeLibro(libroId: Int): List<Reserva> =
        reservaRepository.getReservasDeLibro(libroId)

    fun obtenerPrestatarioActual(libroId: Int): Int? =
        reservaRepository.obtenerPrestatarioActual(libroId)

    fun contarReservasPorLibro(libroId: Int): Int =
        reservaRepository.contarReservasPorLibro(libroId)

    fun getCalificacionesDetalle(libroId: Int, limite: Int? = 2) =
        libroRepository.getCalificacionesDetalle(libroId, limite)

    fun obtenerNombresUsuariosCalificaciones(libro: Libro): Map<Int, String> =
        usuarioRepository.resolverNombresCalificaciones(libro.calificaciones)

    fun getRating(libroId: Int): Double =
        libroRepository.getById(libroId).calificacion

    fun getCalificaciones(libroId: Int): List<CalificacionDTO> =
        libroRepository.getCalificacionesDetalle(libroId, null)
            .map { CalificacionDTO(it.usuarioId, it.nombreUsuario, it.valor, it.comentario) }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): Libro {
        val duenio = usuarioRepository.getById(usuarioId)
        val data = LibroData(
            duenio           = duenio,
            titulo           = dto.titulo,
            autor            = dto.autor,
            descripcion      = dto.descripcion,
            genero           = dto.genero,
            cantidadPaginas  = dto.paginas,
            isbn             = dto.isbn,
            idioma           = dto.idioma,
            editorial        = dto.editorial,
            fechaPublicacion = dto.fechaPublicacion,
            estado           = EstadoLibro.fromString(dto.estado),
            imagenUrl        = dto.imagenUrl,
            fechaAgregado    = LocalDate.now(),
            disponibilidad   = Disponibilidad.DISPONIBLE,
        )
        val libro = when (dto.tipo) {
            "Comun"          -> LibroComun(data)
            "ConDedicatoria" -> LibroConDedicatoria(data)
            "Coleccionable"  -> LibroColeccionable(data)
            else -> throw BusinessException("Tipo de libro inválido: ${dto.tipo}")
        }
        return libroRepository.save(libro)
    }

    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): Libro {
        val libro = libroRepository.getById(id)
        evaluarPermiso(libro, usuarioId)
        libro.actualizarDatos(
            nuevoTitulo           = dto.titulo,
            nuevoAutor            = dto.autor,
            nuevaDescripcion      = dto.descripcion,
            nuevoGenero           = dto.genero,
            nuevasPaginas         = dto.paginas,
            nuevoIsbn             = dto.isbn,
            nuevoIdioma           = dto.idioma,
            nuevaEditorial        = dto.editorial,
            nuevaFechaPublicacion = dto.fechaPublicacion,
            nuevoEstado           = EstadoLibro.fromString(dto.estado),
            nuevaImagenUrl        = dto.imagenUrl,
        )
        return libroRepository.save(libro)
    }

    @Transactional
    fun agregarCalificacion(idLibro: Int, dto: NewCalificacionDTO) {
        val libro = libroRepository.getById(idLibro)
        usuarioRepository.getById(dto.usuarioId)

        val calificacion = Calificacion(
            libro = libro,
            usuarioId = dto.usuarioId,
            valor = dto.valor,
            comentario = dto.comentario
        )
        libro.agregarCalificacion(calificacion)
        libroRepository.save(libro)
    }

    fun darDeBaja(libroId: Int, usuarioId: Int) {
        val libro = libroRepository.getByIdIncluyendoInactivos(libroId)
        evaluarPermiso(libro, usuarioId)
        libro.activo = false
        libroRepository.save(libro)
    }
    fun reactivar(libroId: Int, usuarioId: Int) {
        val libro = libroRepository.getByIdIncluyendoInactivos(libroId)
        evaluarPermiso(libro, usuarioId)
        libro.activo = true
        libroRepository.save(libro)
    }

    fun obtenerHistorialDePuntajes(libroId: Int): List<HistorialPuntaje> {
        return libroRepository.getHistorialPuntajes(libroId)
    }

    private fun evaluarPermiso(libro: Libro, usuarioId: Int) {
        if (libro.duenio.id != usuarioId)
            throw UnauthorizedException("Acción no autorizada")
    } //chequea si el usuario puede eliminar/editar libro si es dueño
}