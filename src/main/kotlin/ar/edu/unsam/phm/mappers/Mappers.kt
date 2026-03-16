package ar.edu.unsam.phm.mappers

import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.Reserva
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.ReservaDTO
import java.time.format.DateTimeFormatter

fun Libro.toDTO(usuarioConsultaId: Int): LibroDTO {
    val ultimaReserva = this.duenio.reservas.lastOrNull()
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
        propietarioId = this.duenio.id,
        imagenUrl = this.imagenUrl,
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