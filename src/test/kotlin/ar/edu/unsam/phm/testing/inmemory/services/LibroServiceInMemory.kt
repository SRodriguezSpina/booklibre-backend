package ar.edu.unsam.phm.testing.inmemory.services

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
import ar.edu.unsam.phm.testing.inmemory.repository.EntityNotFoundException
import ar.edu.unsam.phm.testing.inmemory.repository.LibroRepository
import ar.edu.unsam.phm.testing.inmemory.repository.ReservaRepository
import ar.edu.unsam.phm.testing.inmemory.repository.UsuarioRepository
import java.time.LocalDate
import kotlin.math.ceil

class LibroServiceInMemory(
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
                    val reservasDelLibro = reservaRepository.findByLibroId(libro.id)
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
                    val cantidadReservasDelLibro = reservaRepository.findByLibroId(it.id).size
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
        val libro = try {
            libroRepository.getById(id)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro.kt $id no encontrado")
        }

        val reservasDelLibro = reservaRepository.findByLibroId(id)
        val nombresUsuarios = usuarioRepository.resolverNombresCalificaciones(libro.calificaciones)

        return libro.toDTO(
            todosLosRangos = reservasDelLibro,
            nombresUsuarios = nombresUsuarios,
        )
    }

    private fun nombreUsuarioResenia(calificaciones: List<Calificacion>): Map<Int, String> {
        return calificaciones
            .map { it.usuarioId }
            .toSet()
            .associateWith { idCalificador ->
                usuarioRepository.findById(idCalificador)?.nombre ?: "Usuario $idCalificador"
            }
    }

    fun getRating(libroId: Int): Double {
        val libro = try {
            libroRepository.getById(libroId)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro.kt $libroId no encontrado")
        }
        return libro.getCalificacion()
    }

    fun getCalificaciones(libroId: Int): List<CalificacionDTO> {
        val libro = try {
            libroRepository.getById(libroId)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro.kt $libroId no encontrado")
        }
        return libro.calificaciones.toDetalleDTOs(nombreUsuarioResenia(libro.calificaciones))
    }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): LibroDTO {
        val duenio = usuarioRepository.findById(usuarioId)
            ?: throw NotFoundException("Usuario no encontrado")

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
        libroRepository.create(libro)
        return libro.toDTO()
    }

    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): LibroDTO {
        val libroExistente = try {
            libroRepository.getById(id)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro.kt $id no encontrado")
        }

        if (libroExistente.duenio.id != usuarioId) {
            throw UnauthorizedException("No autorizado para editar este libro")
        }

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
            throw NotFoundException("Libro.kt $idLibro no encontrado")
        }
        usuarioRepository.findById(calificacion.usuarioId)
            ?: throw NotFoundException("Usuario ${calificacion.usuarioId} no encontrado")
        libro.agregarCalificacion(calificacion)
        libroRepository.update(libro)
    }

    fun eliminarLibro(libroId: Int, usuarioId: Int) {
        val libro = try {
            libroRepository.getById(libroId)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Libro.kt no encontrado")
        }

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