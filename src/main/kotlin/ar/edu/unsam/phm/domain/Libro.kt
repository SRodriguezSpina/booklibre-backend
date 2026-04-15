package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.repository.BaseEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

data class LibroFiltros(
    val query: String = "",
    val generos: List<String> = emptyList(),
    val paginasMin: Int? = null,
    val paginasMax: Int? = null,
    val isbn: String? = null,
    val prestadoPor: String? = null,
    val excluirDuenioId: Int? = null,
    val libroIdsReservados: Set<Int>? = null,
    val sortBy: String = "titulo"
) {
    companion object {
        fun desdeRequest(
            query: String, generos: String, paginasMin: Int?, paginasMax: Int?,
            isbn: String?, prestadoPor: String?, usuarioId: Int?,
            libroIdsReservados: Set<Int>?, sortBy: String?
        ): LibroFiltros {
            if (paginasMin != null && paginasMax != null && paginasMin >= paginasMax) {
                throw BusinessException("El mínimo de páginas debe ser menor al máximo")
            }

            return LibroFiltros(
                query = query,
                generos = if (generos.isBlank()) emptyList() else generos.split(","),
                paginasMin = paginasMin,
                paginasMax = paginasMax,
                isbn = isbn,
                prestadoPor = prestadoPor,
                excluirDuenioId = usuarioId,
                libroIdsReservados = libroIdsReservados,
                sortBy = sortBy ?: "titulo" // Valor por defecto
            )
        }
    }
}

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
    fun actualizoDisponibilidad(estado: Boolean) =
        if (estado) disponibilidad = Disponibilidad.DISPONIBLE else disponibilidad = Disponibilidad.RESERVADO

    abstract fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int = 0): Int

    fun cumpleFiltros(filtros: LibroFiltros): Boolean {
        val matchQuery = filtros.query.isBlank() ||
                titulo.contains(filtros.query, ignoreCase = true) ||
                autor.contains(filtros.query, ignoreCase = true)

        val matchGenero = filtros.generos.isEmpty() ||
                filtros.generos.contains(genero.name)

        val matchPaginasMin = filtros.paginasMin == null || cantidadPaginas >= filtros.paginasMin
        val matchPaginasMax = filtros.paginasMax == null || cantidadPaginas <= filtros.paginasMax

        val matchIsbn = filtros.isbn.isNullOrBlank() ||
                isbn.contains(filtros.isbn, ignoreCase = true)

        val matchPrestadoPor = filtros.prestadoPor.isNullOrBlank() ||
                duenio.nombre.contains(filtros.prestadoPor, ignoreCase = true)

        val matchDisponibilidad = filtros.libroIdsReservados == null ||
                id !in filtros.libroIdsReservados

        return matchQuery && matchGenero && matchPaginasMin && matchPaginasMax &&
                matchIsbn && matchPrestadoPor && matchDisponibilidad
    }
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