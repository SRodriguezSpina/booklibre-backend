package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.NewCalificacionDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.mongo.CalificacionMongoRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class LibroService(
    private val libroRepository: LibroRepository,
    private val usuarioJpa: UsuarioJpaRepository,
    private val reservaJpa: ReservaJpaRepository,
    private val calificacionesMongo: CalificacionMongoRepository,
    private val reservaService: ReservaService,
) {
    fun search(
        query: String, generos: String, page: Int, size: Int,
        usuarioId: Int?, paginasMin: Int?, paginasMax: Int?,
        fechaDesde: String?, fechaHasta: String?,
        isbn: String?, prestadoPor: String?, sortBy: String?,
    ): PagedResponse<Libro> {
        val librosReservadosIds = reservaService.obtenerIdsLibrosReservados(fechaDesde, fechaHasta)
        val filtros = LibroFiltros.desdeRequest(
            query, generos, paginasMin, paginasMax, isbn,
            prestadoPor, usuarioId, librosReservadosIds, sortBy,
        )
        return libroRepository.buscarPaginado(filtros, page, size)
    }

    fun calcularBiblioKarma(libro: Libro, usuarioId: Int?): Int {
        val usuario = usuarioId?.let { usuarioJpa.findById(it).orElse(null) } ?: return 0
        val cantidadReservas = reservaJpa.contarReservasPorLibro(libro.id)
        return libro.plusBiblioKarma(usuario, cantidadReservas)
    }

    fun obtenerLibro(id: Int): Libro = libroRepository.getById(id)

    fun obtenerReservasDeLibro(libroId: Int) = reservaJpa.findByLibroId(libroId)

    fun obtenerPrestatarioActual(libroId: Int): Int? =
        reservaJpa.obtenerPrestatarioActual(libroId)

    fun contarReservasPorLibro(libroId: Int): Int =
        reservaJpa.contarReservasPorLibro(libroId)

    fun getCalificacionesDetalle(libroId: Int, limite: Int? = 2) =
        libroRepository.getCalificacionesDetalle(libroId, limite)

    fun getRating(libroId: Int): Double =
        libroRepository.getById(libroId).calificacion

    fun getLibrosConReservasCumplidas(): List<Libro> {
        val libroIds = reservaJpa.findLibrosConTodasReservasCumplidas(LocalDateTime.now())
        return libroRepository.findAllByIds(libroIds)
    }

    fun getLibrosConReservasActivasMayorA(minReservas: Long = 3L): List<Libro> {
        val libroIds = reservaJpa.findLibroIdsConAlMenosNReservasActivas(LocalDateTime.now(), minReservas)
        return libroRepository.findAllByIds(libroIds)
    }

    fun getCalificaciones(libroId: Int): List<CalificacionDTO> =
        libroRepository.getCalificacionesDetalle(libroId, null)
            .map { CalificacionDTO(it.usuarioId, it.nombreUsuario, it.valor, it.comentario) }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): Libro {
        val duenio = usuarioJpa.findById(usuarioId)
            .orElseThrow { NotFoundException("Usuario $usuarioId no encontrado") }
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

    fun agregarCalificacion(idLibro: Int, dto: NewCalificacionDTO) {
        val libro = libroRepository.getById(idLibro)
        usuarioJpa.findById(dto.usuarioId)
            .orElseThrow { NotFoundException("Usuario ${dto.usuarioId} no encontrado") }

        if (libroRepository.usuarioYaCalifico(idLibro, dto.usuarioId)) {
            throw ConflictException("Usuario ${dto.usuarioId} ya calificó este libro")
        }
        if (dto.valor !in 1..5) {
            throw BusinessException("La calificación debe estar entre 1 y 5")
        }

        val calificacion = Calificacion(
            usuarioId  = dto.usuarioId,
            valor      = dto.valor,
            comentario = dto.comentario,
            fecha      = LocalDateTime.now(),
        )
        libroRepository.agregarCalificacion(libro, calificacion)
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

    fun obtenerHistorialDePuntajes(libroId: Int): List<HistorialPuntaje> =
        libroRepository.getHistorialPuntajes(libroId)

    fun contarLibrosColecciones(): Long =
        libroRepository.contarLibrosCollecionables()

    // Chequea si el usuario puede eliminar/editar un libro siendo dueño
    private fun evaluarPermiso(libro: Libro, usuarioId: Int) {
        if (libro.duenio.id != usuarioId)
            throw UnauthorizedException("Acción no autorizada")
    }
}