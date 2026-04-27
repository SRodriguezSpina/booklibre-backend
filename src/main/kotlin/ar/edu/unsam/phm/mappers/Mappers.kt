package ar.edu.unsam.phm.mappers

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.dtos.RangoReservaDTO
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.readmodels.CalificacionDetalleView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.sortedByDescending

fun Usuario.toResponse(): UsuarioResponse {
    val tipoStr = when (tipoUsuario.trim().uppercase().replace(" ", "_")) {
        "PUBLICADOR"                              -> "Publicador"
        "LECTOR_PUBLICADOR", "LECTORPUBLICADOR"  -> "LectorPublicador"
        else                                      -> "Lector"
    }
    return UsuarioResponse(
        id          = id,
        nombre      = nombre,
        email       = email,
        desc        = desc,
        fechaRegistro = fechaRegistro.toString(),
        celular     = celular,
        ciudad      = ciudad,
        tipoUsuario = tipoStr,
        bibliokarmas = bibliokarmas,
        imagenUrl   = imagenUrl,
        leidos = leidos,
        reservados = reservados
    )
}

fun Libro.toDTO(
    todosLosRangos: List<Reserva> = emptyList(),
    nombresUsuarios: Map<Int, String> = emptyMap(),
): LibroDTO {
    val hoy = LocalDate.now()
    val reservasActivas = todosLosRangos.filter { it.sePisaCon(hoy, hoy) }
    val reservaPrincipal = reservasActivas.firstOrNull()
    val rangosDTO = todosLosRangos
        .filter { !it.fechaHasta.isBefore(hoy) }
        .map {
            RangoReservaDTO(
                fechaDesde = it.fechaDesde.format(DateTimeFormatter.ISO_DATE),
                fechaHasta = it.fechaHasta.format(DateTimeFormatter.ISO_DATE)
            )
        }
    return LibroDTO(
        id = this.id,
        titulo = this.titulo,
        tipo = this.javaClass.simpleName.replace("Libro", ""),
        descripcion = this.descripcion,
        genero = this.genero,
        autor = this.autor,
        paginas = this.cantidadPaginas,
        isbn = this.isbn,
        idioma = this.idioma,
        editorial = this.editorial,
        estado = this.estado.name,
        fechaPublicacion = this.fechaPublicacion,
        fechaAgregado = this.fechaAgregado,
        propietarioId = this.duenio.id,
        imagenUrl = this.imagenUrl ?: "",
        rating = this.calificacion,
        cantidadReservas = todosLosRangos.size,
        estoyReservado = reservaPrincipal != null,
        alquiladoPorId = reservaPrincipal?.usuario?.id,
        bibliokarma = null,
        reservas = rangosDTO,
        cantidadCalificaciones = this.calificaciones.size,
        ultimasCalificaciones = this.calificaciones
            .takeLast(2)
            .map { calificacion ->
                CalificacionDTO(
                    usuarioId     = calificacion.usuarioId,
                    nombreUsuario = nombresUsuarios[calificacion.usuarioId] ?: "Usuario ${calificacion.usuarioId}",
                    valor         = calificacion.valor,
                    comentario    = calificacion.comentario
                )
            }
    )
}

fun Libro.toDTO(
    alquiladoPorId: Int?,
    calificaciones: List<CalificacionDetalleView>,
    totalReservas: Int,
): LibroDTO {
    return LibroDTO(
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
        estoyReservado   = !this.estaDisponible(),
        alquiladoPorId   = alquiladoPorId,
        bibliokarma      = null,
        reservas         = emptyList(),
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
}

fun Reserva.toDTO(): ReservaDTO {
    return ReservaDTO(
        id = this.libro.id,
        prestamoId = this.id,
        titulo = this.libro.titulo,
        autor = this.libro.autor,
        estado = this.libro.estado.name,
        propietarioId = this.libro.duenio.id,
        propietarioNombre = this.libro.duenio.nombre,
        imagenUrl = this.libro.imagenUrl ?: "",
        alquiladoPorId = usuario.id,
        alquiladoPorNombre = usuario.nombre,
        fechaDesde = fechaDesde.format(DateTimeFormatter.ISO_DATE),
        fechaHasta = fechaHasta.format(DateTimeFormatter.ISO_DATE),
        rating = this.libro.calificacion,
        bibliokarma = bibliokarmas,
        disponibilidad = this.libro.estaDisponible(),
        libroActivo = this.libro.activo
    )
}

fun PagedResponse<Libro>.toHomeDTO(bibliokarma: (Libro) -> Int?): PagedResponse<LibroHomeDTO> =
    PagedResponse(content.map { it.toHomeDTO(bibliokarma(it)) }, page, totalPages)

fun Libro.toHomeDTO(bibliokarma: Int?): LibroHomeDTO {
    return LibroHomeDTO(
        id = this.id,
        imagenUrl = this.imagenUrl ?: "",
        genero = this.genero,
        titulo = this.titulo,
        autor = this.autor,
        calificacion = this.calificacion,
        isbn = this.isbn,
        idioma = this.idioma,
        tipo = this.javaClass.simpleName.replace("Libro", ""),
        bibliokarma = bibliokarma,
        estado = this.estado.name,
        duenio = this.duenio.nombre,
    )
    }
fun List<Calificacion>.toDetalleDTOs(nombrePorUsuarioId: Map<Int, String>): List<CalificacionDTO> {
    return map { calificacion ->
        CalificacionDTO(
            usuarioId     = calificacion.usuarioId,
            nombreUsuario = nombrePorUsuarioId[calificacion.usuarioId] ?: "Usuario ${calificacion.usuarioId}",
            valor         = calificacion.valor,
            comentario    = calificacion.comentario
        )
    }
}

fun Libro.toLibroUsuarioItemDTO(): LibroUsuarioItemDTO {
    return LibroUsuarioItemDTO(
        id = this.id,
        titulo = this.titulo,
        autor = this.autor,
        genero = this.genero.name,
        disponible = this.estaDisponible(),
        fechaAgregado = this.fechaAgregado.format(DateTimeFormatter.ISO_DATE),
        imagenUrl = this.imagenUrl
    )
}