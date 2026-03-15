package ar.edu.unsam.phm
import ar.edu.unsam.phm.repository.BaseEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

abstract class Libro(
    val duenio: Usuario,
    var titulo: String,
    var autor: String,
    var descripcion: String,
    var genero: Genero,
    var cantidadPaginas: Int,
    var isbn: String,
    var idioma: Idioma,
    var editorial: String,
    var fechaPublicacion: LocalDate,
    var estado: EstadoLibro,
    var imagenUrl: String,
    var reservas: MutableList<Reserva> = mutableListOf(),
): BaseEntity() {
    fun calcularBiblioKarmaTotal(usuario: Usuario, desde: LocalDate, hasta: LocalDate): Int {
        val diasReserva = ChronoUnit.DAYS.between(desde, hasta).toInt() + 1
        return (5 * diasReserva) + plusBiblioKarma(usuario)
    }

    abstract fun plusBiblioKarma(usuario: Usuario): Int

    fun agregarReserva(reserva: Reserva) {
        reservas.add(reserva)
    }
}
class LibroComun(
    duenio: Usuario, id: Int, titulo: String, autor: String, descripcion: String, genero: Genero,
    paginas: Int, isbn: String, idioma: Idioma, editorial: String, fecha: LocalDate, estado: EstadoLibro, imagenUrl: String
) : Libro(duenio, titulo, autor, descripcion, genero, paginas, isbn, idioma, editorial, fecha, estado, imagenUrl) {
    override fun plusBiblioKarma(usuario: Usuario): Int {
        val multiplicador = if (usuario.bibliokarmas < 1000) 5 else 2
        return cantidadPaginas * multiplicador
    }
}
class LibroConDedicatoria(
    duenio: Usuario, id: Int, titulo: String, autor: String, descripcion: String, genero: Genero,
    paginas: Int, isbn: String, idioma: Idioma, editorial: String, fecha: LocalDate, estado: EstadoLibro, imagenUrl: String
) : Libro(duenio, titulo, autor, descripcion, genero, paginas, isbn, idioma, editorial, fecha, estado, imagenUrl) {
    override fun plusBiblioKarma(usuario: Usuario): Int {
        return 200 + (10 * reservas.size)
    }
}
class LibroColeccionable(
    duenio: Usuario, id: Int, titulo: String, autor: String, descripcion: String, genero: Genero,
    paginas: Int, isbn: String, idioma: Idioma, editorial: String, fecha: LocalDate, estado: EstadoLibro, imagenUrl: String
) : Libro(duenio, titulo, autor, descripcion, genero, paginas, isbn, idioma, editorial, fecha, estado, imagenUrl) {
    override fun plusBiblioKarma(usuario: Usuario): Int {
        val karmaParte = ceil(usuario.bibliokarmas / 5.0).toInt()
        return karmaParte + cantidadPaginas
    }
}