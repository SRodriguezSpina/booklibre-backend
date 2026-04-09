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
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.mappers.toDetalleDTOs
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.mappers.toEntity
import ar.edu.unsam.phm.persistence.repositories.LibroJpaRepository
import ar.edu.unsam.phm.persistence.repositories.ReservaJpaRepository
import ar.edu.unsam.phm.persistence.repositories.UsuarioJpaRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import kotlin.math.ceil

@Service
class LibroService(
    private val libroJpaRepository: LibroJpaRepository,
    private val usuarioJpaRepository: UsuarioJpaRepository,
    private val reservaJpaRepository: ReservaJpaRepository,
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
        if (paginasMin != null && paginasMax != null && paginasMin >= paginasMax) {
            throw BusinessException("El mínimo de páginas debe ser menor al máximo")
        }
        if (fechaDesde != null && fechaHasta != null) {
            val desde = LocalDate.parse(fechaDesde)
            val hasta = LocalDate.parse(fechaHasta)
            if (!desde.isBefore(hasta)) {
                throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
            }
        }

        val usuario = if (usuarioId != null) {
            usuarioJpaRepository.findById(usuarioId).orElse(null)?.toDomainLight()
        } else {
            null
        }

        val listaGeneros = if (generos.isBlank()) emptyList() else generos.split(",")

        val filtrados = libroJpaRepository.findAll()
            .map { it.toDomainLight() }
            .filter { usuario == null || it.duenio.id != usuarioId }
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
                    val reservasDelLibro = reservaJpaRepository.findByLibroId(libro.id).map { it.toDomain() }
                    reservasDelLibro.none { reserva ->
                        reserva.fechaDesde <= hasta && reserva.fechaHasta >= desde
                    }
                } else {
                    true
                }

                matchQuery && matchGenero && matchPaginasMin && matchPaginasMax &&
                    matchIsbn && matchPrestadoPor && matchFechas
            }
            .sortedBy { libro ->
                when (sortBy) {
                    "autor" -> libro.autor.lowercase()
                    "duenio" -> libro.duenio.nombre.lowercase()
                    else -> libro.titulo.lowercase()
                }
            }
            .map {
                val bibliokarma = if (usuario != null) {
                    val cantidadReservasDelLibro = reservaJpaRepository.findByLibroId(it.id).size
                    it.plusBiblioKarma(usuario, cantidadReservasDelLibro)
                } else {
                    0
                }
                it.toHomeDTO(bibliokarma)
            }

        val totalPages = ceil(filtrados.size.toDouble() / size).toInt()
        val content = filtrados.drop(page * size).take(size)

        return PagedResponse(content, page, totalPages)
    }

    fun getLibro(id: Int): LibroDTO {
        val entity = libroJpaRepository.findById(id)
            .orElseThrow { NotFoundException("Libro $id no encontrado") }

        val libro = entity.toDomainLight()
        val reservasDelLibro = reservaJpaRepository.findByLibroId(id).map { it.toDomain() }
        val nombresUsuarios = nombreUsuarioResenia(libro.calificaciones)
        return libro.toDTO(
            todosLosRangos = reservasDelLibro,
            nombresUsuarios = nombresUsuarios,
        )
    }

    private fun nombreUsuarioResenia(calificaciones: List<Calificacion>): Map<Int, String> {
        return calificaciones
            .map { it.usuarioId }
            .toSet()
            .associateWith { uid ->
                usuarioJpaRepository.findById(uid).orElse(null)?.nombre ?: "Usuario $uid"
            }
    }

    fun getRating(libroId: Int): Double {
        val entity = libroJpaRepository.findById(libroId)
            .orElseThrow { NotFoundException("Libro $libroId no encontrado") }
        return entity.toDomainLight().getCalificacion()
    }

    fun getCalificaciones(libroId: Int): List<CalificacionDTO> {
        val entity = libroJpaRepository.findById(libroId)
            .orElseThrow { NotFoundException("Libro $libroId no encontrado") }
        val libro = entity.toDomainLight()
        return libro.calificaciones.toDetalleDTOs(nombreUsuarioResenia(libro.calificaciones))
    }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): LibroDTO {
        val duenio = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

        val data = LibroData(
            duenio = duenio.toDomainLight(),
            titulo = dto.titulo,
            autor = dto.autor,
            descripcion = dto.descripcion,
            genero = dto.genero,
            cantidadPaginas = dto.paginas,
            isbn = dto.isbn,
            idioma = dto.idioma,
            editorial = dto.editorial,
            fechaPublicacion = dto.fechaPublicacion,
            estado = EstadoLibro.valueOf(dto.estado),
            imagenUrl = dto.imagenUrl,
            fechaAgregado = LocalDate.now(),
            disponibilidad = Disponibilidad.DISPONIBLE,
        )

        val libro = when (dto.tipo) {
            "Comun" -> LibroComun(data)
            "ConDedicatoria" -> LibroConDedicatoria(data)
            "Coleccionable" -> LibroColeccionable(data)
            else -> throw BusinessException("Tipo de libro inválido: ${dto.tipo}")
        }

        val entity = libro.toEntity(duenio)
        val saved = libroJpaRepository.save(entity)
        return saved.toDomainLight().toDTO()
    }

    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): LibroDTO {
        val existente = libroJpaRepository.findById(id)
            .orElseThrow { NotFoundException("Libro $id no encontrado") }

        if ((existente.duenio.id ?: 0) != usuarioId) {
            throw UnauthorizedException("No autorizado para editar este libro")
        }

        existente.titulo = dto.titulo
        existente.autor = dto.autor
        existente.descripcion = dto.descripcion
        existente.genero = dto.genero.name
        existente.cantidadPaginas = dto.paginas
        existente.isbn = dto.isbn
        existente.idioma = dto.idioma.name
        existente.editorial = dto.editorial
        existente.estado = EstadoLibro.valueOf(dto.estado).name
        existente.fechaPublicacion = dto.fechaPublicacion
        existente.imagenUrl = dto.imagenUrl

        val saved = libroJpaRepository.save(existente)
        return saved.toDomainLight().toDTO()
    }

    fun agregarCalificacion(idLibro: Int, calificacion: Calificacion) {
        val libroEntity = libroJpaRepository.findById(idLibro)
            .orElseThrow { NotFoundException("Libro $idLibro no encontrado") }
        usuarioJpaRepository.findById(calificacion.usuarioId)
            .orElseThrow { NotFoundException("Usuario ${calificacion.usuarioId} no encontrado") }

        val libro = libroEntity.toDomainLight()
        libro.agregarCalificacion(calificacion)

        libroEntity.calificaciones = libro.calificaciones.map { it.toEntity(libroEntity) }.toMutableList()
        libroJpaRepository.save(libroEntity)
    }

    fun eliminarLibro(libroId: Int, usuarioId: Int) {
        val libro = libroJpaRepository.findById(libroId)
            .orElseThrow { NotFoundException("Libro no encontrado") }

        if ((libro.duenio.id ?: 0) != usuarioId) {
            throw UnauthorizedException("No autorizado para eliminar este libro")
        }

        val hoy = LocalDate.now()
        val hayReservaActiva = reservaJpaRepository.findByLibroId(libroId)
            .map { it.toDomain() }
            .any { !hoy.isBefore(it.fechaDesde) && !hoy.isAfter(it.fechaHasta) }

        if (hayReservaActiva) {
            throw ConflictException("El libro tiene una reserva activa y no puede eliminarse")
        }

        libroJpaRepository.delete(libro)
    }
}

private fun ar.edu.unsam.phm.persistence.entities.UsuarioEntity.toDomainLight(): ar.edu.unsam.phm.domain.Usuario =
    this.toDomain()

private fun ar.edu.unsam.phm.persistence.entities.LibroEntity.toDomainLight(): ar.edu.unsam.phm.domain.Libro =
    this.toDomain()