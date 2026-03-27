package ar.edu.unsam.phm.mappers

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.RangoReservaDTO
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun Usuario.toResponse(): UsuarioResponse {
    val tipoStr = when (tipoUsuario) {
        is UsuarioLector -> "Lector"
        is UsuarioPublicador -> "Publicador"
        is UsuarioLectorPublicador -> "LectorPublicador"
        else -> "Lector"
    }
    return UsuarioResponse(
        id = id,
        nombre = nombre,
        email = getCredencial().email,
        desc = desc,
        fechaRegistro = fechaRegistro.toString(),
        celular = celular,
        ciudad = ciudad,
        tipoUsuario = tipoStr,
        bibliokarmas = bibliokarmas,
        imagenUrl = imagenUrl,
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
        imagenUrl = this.imagenUrl,
        rating = this.getCalificacion(),
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

fun Reserva.toDTO(usuarioConsultaId: Int): ReservaDTO {
    return ReservaDTO(
        id = this.libro.id,
        prestamoId = this.id,
        titulo = this.libro.titulo,
        autor = this.libro.autor,
        estado = this.libro.estado.name,
        propietarioId = this.libro.duenio.id,
        imagenUrl = this.libro.imagenUrl,
        alquiladoPorId = usuario.id,
        fechaDesde = fechaDesde.format(DateTimeFormatter.ISO_DATE),
        fechaHasta = fechaHasta.format(DateTimeFormatter.ISO_DATE),
        rating = this.libro.getCalificacion(),
        bibliokarma = libro.calcularBiblioKarmaTotal(usuario, fechaDesde, fechaHasta)
    )
}

fun Libro.toHomeDTO(usuario: Usuario?): LibroHomeDTO {
    return LibroHomeDTO(
        id = this.id,
        imagenUrl = this.imagenUrl,
        genero = this.genero,
        titulo = this.titulo,
        autor = this.autor,
        calificacion = this.getCalificacion(),
        isbn = this.isbn,
        idioma = this.idioma,
        tipo = this.javaClass.simpleName.replace("Libro", ""),
        bibliokarma = if(usuario != null) this.plusBiblioKarma(usuario) else 0,
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