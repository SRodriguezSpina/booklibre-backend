package ar.edu.unsam.phm.mappers

import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.Reserva
import ar.edu.unsam.phm.Usuario
import ar.edu.unsam.phm.UsuarioLector
import ar.edu.unsam.phm.UsuarioLectorPublicador
import ar.edu.unsam.phm.UsuarioPublicador
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
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
        bibliokarmas = bibliokarmas
    )
}

fun Libro.toDTO(): LibroDTO {
    val ultimaReserva = this.duenio.reservas.lastOrNull { it.libro.id == this.id }
    val hoy = LocalDate.now()
    return LibroDTO(
        id = this.id,
        titulo = this.titulo,
        tipo = this.javaClass.simpleName.replace("Libro", ""),
        descripcion = this.descripcion,
        genero = this.genero,
        autor = this.autor,
        autorImg = this.autorImg,
        paginas = this.cantidadPaginas,
        isbn = this.isbn,
        idioma = this.idioma,
        editorial = this.editorial,
        estado = this.estado.name,
        fechaPublicacion = this.fechaPublicacion,
        propietarioId = this.duenio.id,
        imagenUrl = this.imagenUrl,
        rating = this.getCalificacion(),
        estoyReservado = ultimaReserva?.sePisaCon(hoy, hoy) ?: false,
        alquiladoPorId = ultimaReserva?.usuario?.id,
        fechaDevolucion = ultimaReserva?.fechaHasta?.format(DateTimeFormatter.ISO_DATE),
        bibliokarma = ultimaReserva?.let {
            this.calcularBiblioKarmaTotal(it.usuario, it.fechaDesde, it.fechaHasta)
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
        bibliokarma = let {
            this.libro.calcularBiblioKarmaTotal(it.usuario, it.fechaDesde, it.fechaHasta)
        }
    )
}

fun Libro.toHomeDTO(): LibroHomeDTO {
    return LibroHomeDTO(
        id = this.id,
        imagenUrl = this.imagenUrl,
        genero = this.genero,
        titulo = this.titulo,
        autor = this.autor,
        calificacion = this.getCalificacion(), // terminar de agregar
        isbn = this.isbn,
        idioma = this.idioma,
        tipo = this.javaClass.simpleName.replace("Libro", ""),
        bibliokarma = 0,
        estado = this.estado.name,
        duenio = this.duenio.nombre,
    )
}