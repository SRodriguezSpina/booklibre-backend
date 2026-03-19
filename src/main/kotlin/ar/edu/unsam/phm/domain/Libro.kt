package ar.edu.unsam.phm
import ar.edu.unsam.phm.exceptions.BusinessException
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
    var calificaciones: MutableList<Calificacion> = mutableListOf(),
): BaseEntity() {
    fun calcularBiblioKarmaTotal(usuario: Usuario, desde: LocalDate, hasta: LocalDate): Int {
        if (!hasta.isAfter(desde)) throw BusinessException("La fecha de hasta debe ser posterior a la fecha de desde")
        val diasReserva = ChronoUnit.DAYS.between(desde, hasta).toInt() + 1
        return (5 * diasReserva) + plusBiblioKarma(usuario)
    }
    // En tu clase Libro.kt
    fun agregarCalificacion(nueva: Calificacion) {
        if (nueva.valor !in 1..5) throw BusinessException("La calificación debe estar entre 1 y 5")
        if (calificaciones.any { it.usuarioId == nueva.usuarioId }) {
            throw BusinessException("Usuario ${nueva.usuarioId} ya calificó este libro")
        }
        calificaciones.add(nueva)
    }
    fun getCalificacion(): Double {
        if (calificaciones.isEmpty()) return 0.0
        return calificaciones.map { it.valor }.average()
    }
    abstract fun plusBiblioKarma(usuario: Usuario): Int
}
class LibroComun(
    duenio: Usuario, id: Int, titulo: String, autor: String, descripcion: String, genero: Genero,
    paginas: Int, isbn: String, idioma: Idioma, editorial: String, fecha: LocalDate, estado: EstadoLibro, imagenUrl: String
) : Libro(duenio, titulo, autor, descripcion, genero, paginas, isbn, idioma, editorial, fecha, estado, imagenUrl) {
    override fun plusBiblioKarma(usuario: Usuario): Int {
        if (cantidadPaginas <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
        val multiplicador = if (usuario.bibliokarmas < 1000) 5 else 2
        return cantidadPaginas * multiplicador
    }
}
class LibroConDedicatoria(
    duenio: Usuario, id: Int, titulo: String, autor: String, descripcion: String, genero: Genero,
    paginas: Int, isbn: String, idioma: Idioma, editorial: String, fecha: LocalDate, estado: EstadoLibro, imagenUrl: String
) : Libro(duenio, titulo, autor, descripcion, genero, paginas, isbn, idioma, editorial, fecha, estado, imagenUrl) {
    override fun plusBiblioKarma(usuario: Usuario): Int {
        return 200 + (10 * duenio.reservas.size)
    }
}
class LibroColeccionable(
    duenio: Usuario, id: Int, titulo: String, autor: String, descripcion: String, genero: Genero,
    paginas: Int, isbn: String, idioma: Idioma, editorial: String, fecha: LocalDate, estado: EstadoLibro, imagenUrl: String
) : Libro(duenio, titulo, autor, descripcion, genero, paginas, isbn, idioma, editorial, fecha, estado, imagenUrl) {
    override fun plusBiblioKarma(usuario: Usuario): Int {
        if (usuario.bibliokarmas < 0) throw BusinessException("El usuario no puede tener bibliokarmas negativos")
        if (cantidadPaginas <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
        val karmaParte = ceil(usuario.bibliokarmas / 5.0).toInt()
        return karmaParte + cantidadPaginas
    }
}