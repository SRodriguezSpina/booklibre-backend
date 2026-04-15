package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.repository.BaseEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

data class LibroData(
    val duenio: Usuario,
    val titulo: String,
    val autor: String,
    val descripcion: String,
    val genero: Genero,
    val cantidadPaginas: Int,
    val isbn: String,
    val idioma: Idioma,
    val editorial: String,
    val fechaPublicacion: LocalDate,
    val estado: EstadoLibro,
    val imagenUrl: String,
    val fechaAgregado: LocalDate = LocalDate.now(),
    val disponibilidad: Disponibilidad
)

abstract class Libro(data: LibroData): BaseEntity() {
    companion object {
        private val ISBN_REGEX = Regex("^\\d{13}$")

        private fun normalizarYValidarTextoObligatorio(raw: String, nombreCampo: String): String {
            val value = raw.trim()
            if (value.isBlank()) {
                throw BusinessException("El $nombreCampo no puede estar vacío")
            }
            return value
        }

        private fun validarPaginas(value: Int): Int {
            if (value <= 0) {
                throw BusinessException("La cantidad de páginas debe ser mayor a 0")
            }
            return value
        }

        fun normalizarYValidarIsbn(raw: String): String {
            val isbn = raw.trim()
            if (!ISBN_REGEX.matches(isbn)) {
                throw BusinessException("El ISBN debe contener exactamente 13 caracteres numéricos")
            }
            return isbn
        }
    }

    val duenio = data.duenio
    var titulo = normalizarYValidarTextoObligatorio(data.titulo, "título")
        set(value) {
            field = normalizarYValidarTextoObligatorio(value, "título")
        }
    var autor = normalizarYValidarTextoObligatorio(data.autor, "autor")
        set(value) {
            field = normalizarYValidarTextoObligatorio(value, "autor")
        }
    var descripcion = normalizarYValidarTextoObligatorio(data.descripcion, "descripción")
        set(value) {
            field = normalizarYValidarTextoObligatorio(value, "descripción")
        }
    var genero = data.genero
    var cantidadPaginas = validarPaginas(data.cantidadPaginas)
        set(value) {
            field = validarPaginas(value)
        }
    var isbn = normalizarYValidarIsbn(data.isbn)
        set(value) {
            field = normalizarYValidarIsbn(value)
        }
    var idioma = data.idioma
    var editorial = normalizarYValidarTextoObligatorio(data.editorial, "editorial")
        set(value) {
            field = normalizarYValidarTextoObligatorio(value, "editorial")
        }
    var fechaPublicacion = data.fechaPublicacion
    var fechaAgregado = data.fechaAgregado
    var estado = data.estado
    var imagenUrl = normalizarYValidarTextoObligatorio(data.imagenUrl, "URL de imagen")
        set(value) {
            field = normalizarYValidarTextoObligatorio(value, "URL de imagen")
        }
    var calificaciones: MutableList<Calificacion> = mutableListOf()
    var disponibilidad: Disponibilidad = data.disponibilidad

    fun actualizarDatos(
        nuevoTitulo: String,
        nuevoAutor: String,
        nuevaDescripcion: String,
        nuevoGenero: Genero,
        nuevasPaginas: Int,
        nuevoIsbn: String,
        nuevoIdioma: Idioma,
        nuevaEditorial: String,
        nuevoEstado: EstadoLibro,
        nuevaFechaPublicacion: LocalDate,
        nuevaImagenUrl: String,
    ) {
        titulo = nuevoTitulo
        autor = nuevoAutor
        descripcion = nuevaDescripcion
        genero = nuevoGenero
        cantidadPaginas = nuevasPaginas
        isbn = nuevoIsbn
        idioma = nuevoIdioma
        editorial = nuevaEditorial
        estado = nuevoEstado
        fechaPublicacion = nuevaFechaPublicacion
        imagenUrl = nuevaImagenUrl
    }

    fun calcularBiblioKarmaTotal(
        usuario: Usuario,
        desde: LocalDate,
        hasta: LocalDate,
        cantidadReservasDelLibro: Int = 0,
    ): Int {
        if (!hasta.isAfter(desde)) throw BusinessException("La fecha de hasta debe ser posterior a la fecha de desde")
        val diasReserva = ChronoUnit.DAYS.between(desde, hasta).toInt() + 1
        return (5 * diasReserva) + plusBiblioKarma(usuario, cantidadReservasDelLibro)
    }
    fun agregarCalificacion(nueva: Calificacion) {
        if (nueva.valor !in 1..5) throw BusinessException("La calificación debe estar entre 1 y 5")
        if (calificaciones.any { it.usuarioId == nueva.usuarioId }) {
            throw BusinessException("Usuario ${nueva.usuarioId} ya calificó este libro")
        }
        calificaciones.add(nueva)
    }
    fun getCalificacion(): Double {
        if (calificaciones.isEmpty()) return 0.0
        val promedio = calificaciones.map { it.valor }.average()
        return (promedio * 10.0).roundToInt() / 10.0
    }
    fun estaDisponible() = disponibilidad == Disponibilidad.DISPONIBLE
    fun actualizoDisponibilidad(estado: Boolean) = if (estado) disponibilidad = Disponibilidad.DISPONIBLE else disponibilidad = Disponibilidad.RESERVADO

    abstract fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int = 0): Int
}
class LibroComun(data: LibroData) : Libro(data) {
    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        if (cantidadPaginas <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
        val multiplicador = if (usuario.bibliokarmas < 1000) 5 else 2
        return cantidadPaginas * multiplicador
    }
}
class LibroConDedicatoria(data: LibroData) : Libro(data) {
    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        return 200 + (10 * cantidadReservasDelLibro)
    }
}
class LibroColeccionable(data: LibroData) : Libro(data) {
    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        if (usuario.bibliokarmas < 0) throw BusinessException("El usuario no puede tener bibliokarmas negativos")
        if (cantidadPaginas <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
        val karmaParte = ceil(usuario.bibliokarmas / 5.0).toInt()
        return karmaParte + cantidadPaginas
    }
}