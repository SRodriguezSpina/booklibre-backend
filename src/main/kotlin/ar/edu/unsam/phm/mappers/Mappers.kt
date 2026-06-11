package ar.edu.unsam.phm.mappers

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.readmodels.CalificacionDetalleView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Usuario.toResponse(): UsuarioResponse {
    val tipoStr = when (tipoUsuario.trim().uppercase().replace(" ", "_")) {
        "PUBLICADOR"                              -> "Publicador"
        "LECTOR_PUBLICADOR", "LECTORPUBLICADOR"   -> "LectorPublicador"
        "ADMIN"                                   -> "ADMIN"
        else                                      -> "Lector"
    }
    return UsuarioResponse(
        id           = id,
        nombre       = nombre,
        email        = email,
        desc         = desc,
        fechaRegistro = fechaRegistro.toString(),
        celular      = celular,
        ciudad       = ciudad,
        tipoUsuario  = tipoStr,
        bibliokarmas = bibliokarmas,
        imagenUrl    = imagenUrl,
        leidos       = leidos,
        reservados   = reservados,
        esAdmin      = esAdmin,
    )
}

fun Usuario.toBibliokarmaDTO(): UsuarioBibliokarmaDTO =
    UsuarioBibliokarmaDTO(
        id = id,
        nombre = nombre,
        bibliokarma = bibliokarmas
    )


/** Mapping simple: sin calificaciones, sin rangos — usado en create/update. */
fun Libro.toDTO(): LibroDTO = LibroDTO(
    id               = this.id,
    titulo           = this.titulo,
    tipo             = this.javaClass.simpleName.replace("Libro", ""),
    descripcion      = this.descripcion,
    genero           = this.genero,
    autor            = this.autor,
    paginas          = this.cantidadPaginas,
    isbn             = this.isbn,
    idioma           = this.idioma,
    editorial        = this.editorial,
    estado           = this.estado.name,
    fechaPublicacion = this.fechaPublicacion,
    fechaAgregado    = this.fechaAgregado,
    propietarioId    = this.duenio.id,
    imagenUrl        = this.imagenUrl ?: "",
    rating           = this.calificacion,
)

/** Mapping completo: recibe datos calculados por el service. */
fun Libro.toDTO(
    alquiladoPorId: Int?,
    estaDisponible: Boolean,
    calificaciones: List<CalificacionDetalleView>,
    totalReservas: Int,
    reservas: List<RangoReservaDTO> = emptyList(),
    miReservaFechaHasta: String? = null,
): LibroDTO = LibroDTO(
    id               = this.id,
    titulo           = this.titulo,
    tipo             = this.javaClass.simpleName.replace("Libro", ""),
    descripcion      = this.descripcion,
    genero           = this.genero,
    autor            = this.autor,
    paginas          = this.cantidadPaginas,
    isbn             = this.isbn,
    idioma           = this.idioma,
    editorial        = this.editorial,
    estado           = this.estado.name,
    fechaPublicacion = this.fechaPublicacion,
    fechaAgregado    = this.fechaAgregado,
    propietarioId    = this.duenio.id,
    imagenUrl        = this.imagenUrl ?: "",
    rating           = this.calificacion,
    cantidadReservas = totalReservas,
    estoyReservado   = !estaDisponible,
    alquiladoPorId   = alquiladoPorId,
    bibliokarma      = null,
    reservas         = reservas,
    miReservaFechaHasta = miReservaFechaHasta,
    cantidadCalificaciones = calificaciones.firstOrNull()?.totalCalificaciones?.toInt() ?: 0,
    ultimasCalificaciones  = calificaciones.map { c ->
        CalificacionDTO(
            usuarioId     = c.usuarioId,
            nombreUsuario = c.nombreUsuario,
            valor         = c.valor,
            comentario    = c.comentario,
        )
    },
)

fun Reserva.toDTO(
    libro: Libro,
    estaDisponible: Boolean = false,
    yaCalificadoPorUsuario: Boolean = false,
): ReservaDTO = ReservaDTO(
    id                     = libro.id,
    prestamoId             = this.id,
    titulo                 = libro.titulo,
    autor                  = libro.autor,
    estado                 = libro.estado.name,
    propietarioId          = libro.duenio.id,
    propietarioNombre      = libro.duenio.nombre,
    imagenUrl              = libro.imagenUrl ?: "",
    alquiladoPorId         = usuario.id,
    alquiladoPorNombre     = usuario.nombre,
    fechaDesde             = fechaDesde.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
    fechaHasta             = fechaHasta.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
    rating                 = libro.calificacion,
    bibliokarma            = bibliokarmas,
    disponibilidad         = estaDisponible,
    libroActivo            = libro.activo,
    yaCalificadoPorUsuario = yaCalificadoPorUsuario,
)

fun PagedResponse<Libro>.toHomeDTO(bibliokarma: (Libro) -> Int?): PagedResponse<LibroHomeDTO> =
    PagedResponse(content.map { it.toHomeDTO(bibliokarma(it)) }, page, totalPages)

fun Libro.toHomeDTO(bibliokarma: Int?): LibroHomeDTO = LibroHomeDTO(
    id           = this.id,
    imagenUrl    = this.imagenUrl ?: "",
    genero       = this.genero,
    titulo       = this.titulo,
    autor        = this.autor,
    calificacion = this.calificacion,
    isbn         = this.isbn,
    idioma       = this.idioma,
    tipo         = this.javaClass.simpleName.replace("Libro", ""),
    bibliokarma  = bibliokarma,
    estado       = this.estado.name,
    duenio       = this.duenio.nombre,
)

fun Libro.toPopularCacheDTO(bibliokarma: Int?): LibroPopularCacheDTO = LibroPopularCacheDTO(
    id           = this.id,
    imagenUrl    = this.imagenUrl ?: "",
    genero       = this.genero,
    titulo       = this.titulo,
    autor        = this.autor,
    calificacion = this.calificacion,
    isbn         = this.isbn,
    idioma       = this.idioma,
    tipo         = this.javaClass.simpleName.replace("Libro", ""),
    bibliokarma  = bibliokarma,
    estado       = this.estado.name,
    duenio       = this.duenio.nombre,
    clicks       = this.clicks.size,
)

fun Libro.toLibroUsuarioItemDTO(): LibroUsuarioItemDTO = LibroUsuarioItemDTO(
    id            = this.id,
    titulo        = this.titulo,
    autor         = this.autor,
    genero        = this.genero.name,
    disponible    = true, // La disponibilidad real se consulta desde el service cuando se necesita
    fechaAgregado = this.fechaAgregado.format(DateTimeFormatter.ISO_DATE),
    imagenUrl     = this.imagenUrl,
)