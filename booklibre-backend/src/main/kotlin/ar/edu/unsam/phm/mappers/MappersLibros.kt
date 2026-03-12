package ar.edu.unsam.phm.mappers

import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.PrestamoDTO
import java.time.format.DateTimeFormatter

fun Libro.toDTO(usuarioConsultaId: Int): LibroDTO {
    val ultimaReserva = this.reservas.lastOrNull()
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

fun Libro.toPrestamoDTO(usuarioConsultaId: Int): PrestamoDTO {
    val ultimaReserva = this.reservas.lastOrNull()
    return PrestamoDTO(
        id = this.id,
        titulo = this.titulo,
        autor = this.autor,
        estado = this.estado.name,
        propietarioId = this.duenio.id,
        imagenUrl = this.imagenUrl,
        alquiladoPorId = ultimaReserva?.usuario?.id,
        fechaDesde = ultimaReserva?.fechaDesde?.format(DateTimeFormatter.ISO_DATE),
        fechaHasta = ultimaReserva?.fechaHasta?.format(DateTimeFormatter.ISO_DATE),
        rating = 4.5,
        bibliokarma = ultimaReserva?.let {
            this.calcularBiblioKarmaTotal(it.usuario, it.fechaDesde, it.fechaHasta)
        }
    )
}