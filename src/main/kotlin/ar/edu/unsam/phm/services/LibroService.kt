package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.domain.DuenioRef
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.NewCalificacionDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.dtos.RangoReservaDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.redis.TopLibrosCacheRepository
import ar.edu.unsam.phm.domain.TopLibrosCache
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.mappers.toPopularCacheDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.domain.LibrosClicks
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class LibroService(
    private val libroRepository: LibroMongoRepository,
    private val usuarioJpa: UsuarioJpaRepository,
    private val reservaJpa: ReservaJpaRepository,
    private val reservaService: ReservaService,
    private val topLibrosCacheRepository: TopLibrosCacheRepository,
    private val librosClicks: LibrosClicks,
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

    fun getLibrosPopulares(usuarioId: Int?): PagedResponse<LibroHomeDTO> {
        val topLibros = topLibrosCacheRepository.findById(TopLibrosCache.CACHE_KEY)
            .orElseGet { actualizarCacheTopLibros() }
        val homeDTOs = topLibros.libros.map { it.toHomeDTO() }
        val content = agregarBibliokarmaACadaLibro(homeDTOs, usuarioId)
        return topLibros.toPagedResponse(content)
    }

    fun actualizarCacheTopLibros(): TopLibrosCache {
        val topIdsFromRedis = librosClicks.getTopLibrosIds(10)

        val librosFromRedis = if (topIdsFromRedis.isNotEmpty()) {
            val librosMap = libroRepository.findAllByIdsCompleto(topIdsFromRedis).associateBy { it.id }
            topIdsFromRedis.mapNotNull { librosMap[it] }
        } else {
            emptyList()
        }

        val topLibros = if (librosFromRedis.size < 10) {
            val faltantes = 10 - librosFromRedis.size
            val fallbackLibros = libroRepository.findTopActivosExcluyendo(limit = faltantes, excludeIds = topIdsFromRedis)
            (librosFromRedis + fallbackLibros).distinctBy { it.id }.take(10)
        } else {
            librosFromRedis
        }

        val totalElements = libroRepository.contarLibrosActivos()
        val cache = TopLibrosCache(
            libros        = topLibros.map { it.toPopularCacheDTO(null) },
            totalElements = totalElements,
        )
        return topLibrosCacheRepository.save(cache)
    }

    private fun agregarBibliokarmaACadaLibro(
        dtos: List<LibroHomeDTO>,
        usuarioId: Int?,
    ): List<LibroHomeDTO> {
        if (usuarioId == null) return dtos
        val librosMap = libroRepository.findAllByIds(dtos.map { it.id }).associateBy { it.id }
        return dtos.map { dto ->
            librosMap[dto.id]
                ?.let { dto.copy(bibliokarma = calcularBiblioKarma(it, usuarioId)) }
                ?: dto
        }
    }

    fun calcularBiblioKarma(libro: Libro, usuarioId: Int?): Int {
        val usuario = usuarioId?.let { usuarioJpa.findById(it).orElse(null) } ?: return 0
        val cantidadReservas = reservaJpa.contarReservasPorLibro(libro.id)
        return libro.plusBiblioKarma(usuario, cantidadReservas)
    }

    fun obtenerLibro(id: Int): Libro = libroRepository.getById(id) ?: throw NotFoundException("Libro $id no encontrado")

    fun obtenerReservasDeLibro(libroId: Int) = reservaJpa.findByLibroId(libroId)

    fun obtenerPrestatarioActual(libroId: Int): Int? =
        reservaJpa.obtenerPrestatarioActual(libroId)

    fun contarReservasPorLibro(libroId: Int): Int =
        reservaJpa.contarReservasPorLibro(libroId)

    fun obtenerRangosReservasActivas(libro: Libro): List<RangoReservaDTO> {
        val ahora = LocalDateTime.now()
        return libro.reservas
            .filter { it.fechaHasta.isAfter(ahora) }
            .map { RangoReservaDTO(
                fechaDesde = it.fechaDesde.toLocalDate().toString(),
                fechaHasta = it.fechaHasta.toLocalDate().toString(),
            ) }
    }

    fun usuarioTieneReservaPendiente(libroId: Int, usuarioId: Int): Boolean =
        reservaJpa.existsByLibroIdAndUsuarioIdAndFechaHastaAfter(libroId, usuarioId, LocalDateTime.now())

    fun obtenerMiReservaHasta(libroId: Int, usuarioId: Int): String? =
        reservaJpa.findFirstByLibroIdAndUsuarioIdAndFechaHastaAfter(libroId, usuarioId, LocalDateTime.now())
            .map { it.fechaHasta.toLocalDate().toString() }
            .orElse(null)

    fun getCalificacionesDetalle(libroId: Int, limite: Int? = 2) =
        libroRepository.getCalificacionesDetalle(libroId, limite)

    fun getRating(libroId: Int): Double =
        (libroRepository.getById(libroId) ?: throw NotFoundException("Libro $libroId no encontrado")).calificacion

    fun getLibrosConReservasCumplidas(): List<Libro> =
        libroRepository.findLibrosConReservasCumplidas()

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
        val duenioRef = DuenioRef(id = duenio.id, nombre = duenio.nombre)
        val data = LibroData(
            duenio           = duenioRef,
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
            fechaAgregado    = LocalDateTime.now(),
        )
        val libro = Libro.crear(dto.tipo, data)
        val saved = libroRepository.save(libro)
        actualizarCacheTopLibros()
        return saved
    }

    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): Libro {
        val libro = libroRepository.getByIdCompleto(id) ?: throw NotFoundException("Libro $id no encontrado")
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
        val saved = libroRepository.save(libro)
        actualizarCacheTopLibros()
        return saved
    }

    fun agregarCalificacion(idLibro: Int, dto: NewCalificacionDTO) {
        val libro = libroRepository.getByIdCompleto(idLibro) ?: throw NotFoundException("Libro $idLibro no encontrado")
        usuarioJpa.findById(dto.usuarioId)
            .orElseThrow { NotFoundException("Usuario ${dto.usuarioId} no encontrado") }

        val calificacion = Calificacion(
            usuarioId  = dto.usuarioId,
            valor      = dto.valor,
            comentario = dto.comentario,
            fecha      = LocalDateTime.now(),
        )
        libroRepository.agregarCalificacion(libro, calificacion)
        actualizarCacheTopLibros()
    }

    fun darDeBaja(libroId: Int, usuarioId: Int) {
        val libro = libroRepository.getByIdIncluyendoInactivosCompleto(libroId) ?: throw NotFoundException("Libro $libroId no encontrado")
        evaluarPermiso(libro, usuarioId)
        libro.activo = false
        libroRepository.save(libro)
        actualizarCacheTopLibros()
    }

    fun eliminarLibro(libroId: Int, usuarioId: Int) {
        val libro = libroRepository.getByIdCompleto(libroId) ?: throw NotFoundException("Libro $libroId no encontrado")
        evaluarPermiso(libro, usuarioId)
        val reservasActivas = reservaJpa.findByLibroIdAndFechaHastaAfter(libroId, LocalDateTime.now())
        if (reservasActivas.isNotEmpty()) {
            throw ConflictException("No se puede eliminar el libro porque tiene reservas activas")
        }
        libroRepository.delete(libro)
        actualizarCacheTopLibros()
    }

    fun reactivar(libroId: Int, usuarioId: Int) {
        val libro = libroRepository.getByIdIncluyendoInactivosCompleto(libroId) ?: throw NotFoundException("Libro $libroId no encontrado")
        evaluarPermiso(libro, usuarioId)
        libro.activo = true
        libroRepository.save(libro)
        actualizarCacheTopLibros()
    }

    fun obtenerHistorialDePuntajes(libroId: Int): List<HistorialPuntaje> =
        libroRepository.getHistorialPuntajes(libroId)

    fun contarLibrosColecciones(): Long =
        libroRepository.contarLibrosCollecionables()

    // Query: libros con promedio de calificación > 4
    fun getLibrosConPromedioMayorA4(): List<Libro> =
        libroRepository.getLibrosConPromedioMayorA4()

    // Chequea si el usuario puede eliminar/editar un libro siendo dueño
    private fun evaluarPermiso(libro: Libro, usuarioId: Int) {
        if (libro.duenio.id != usuarioId)
            throw UnauthorizedException("Acción no autorizada")
    }
}