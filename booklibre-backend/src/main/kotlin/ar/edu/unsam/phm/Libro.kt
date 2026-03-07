package ar.edu.unsam.phm
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

abstract class Libro(
    val titulo: String,
    val autor: String,
    val descripcion: String,
    val genero: Genero,
    val cantidadPaginas: Int,
    val isbn: String,
    val idioma: Idioma,
    val editorial: String,
    val fechaPublicacion: LocalDate,
    var estado: EstadoLibro,
    val reservas: MutableList<Reserva> = mutableListOf()
) {
}