package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Sharded
import java.time.LocalDate
import java.time.LocalDateTime
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
    val sortBy: String = "titulo",
) {
    companion object {
        fun desdeRequest(
            query: String, generos: String, paginasMin: Int?, paginasMax: Int?,
            isbn: String?, prestadoPor: String?, usuarioId: Int?,
            libroIdsReservados: Set<Int>?, sortBy: String?,
        ): LibroFiltros {
            if (paginasMin != null && paginasMax != null && paginasMin >= paginasMax)
                throw BusinessException("El mínimo de páginas debe ser menor al máximo")
            return LibroFiltros(
                query = query,
                generos = if (generos.isBlank()) emptyList() else generos.split(","),
                paginasMin = paginasMin,
                paginasMax = paginasMax,
                isbn = isbn,
                prestadoPor = prestadoPor,
                excluirDuenioId = usuarioId,
                libroIdsReservados = libroIdsReservados,
                sortBy = sortBy ?: "titulo",
            )
        }
    }
}

data class LibroData(
    val duenio: DuenioRef,
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
    val imagenUrl: String? = null,
    val fechaAgregado: LocalDateTime = LocalDateTime.now(),
)

@Document(collection = "libros")
@Sharded(shardKey = ["libroId"])
abstract class Libro() : BaseEntity() {
    companion object {
        private val ISBN_REGEX = Regex("^\\d{13}$")

        fun validarTextoObligatorio(raw: String, campo: String): String {
            val value = raw.trim()
            if (value.isBlank()) throw BusinessException("El $campo no puede estar vacío")
            return value
        }

        fun validarPaginas(value: Int): Int {
            if (value <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
            return value
        }

        fun normalizarYValidarIsbn(raw: String): String {
            val isbn = raw.trim()
            if (!ISBN_REGEX.matches(isbn)) throw BusinessException("El ISBN debe contener exactamente 13 caracteres numéricos")
            return isbn
        }

        fun crear(tipo: String, data: LibroData): Libro {
            return when (tipo) {
                "Comun"          -> LibroComun(data)
                "ConDedicatoria" -> LibroConDedicatoria(data)
                "Coleccionable"  -> LibroColeccionable(data)
                else -> throw BusinessException("Tipo de libro inválido: $tipo")
            }
        }
    }

    lateinit var duenio: DuenioRef
    var titulo: String = ""
    var autor: String = ""
    var descripcion: String = ""
    var genero: Genero = Genero.DRAMA
    var cantidadPaginas: Int = 1
    var isbn: String = ""
    var idioma: Idioma = Idioma.ESPANOL
    var editorial: String = ""
    var fechaPublicacion: LocalDate = LocalDate.now()
    var fechaAgregado: LocalDateTime = LocalDateTime.now()
    var estado: EstadoLibro = EstadoLibro.BUENO
    var imagenUrl: String? = null
    // Promedio de calificaciones (calculado por el service, guardado para ordenamiento)
    var calificacion: Double = 0.0
    var activo: Boolean = true

    var libroId: Int = 0
    var clicks: MutableList<ClickLog> = mutableListOf()
    var calificaciones: MutableList<Calificacion> = mutableListOf()
    var reservas: MutableList<ReservaFecha> = mutableListOf()

    fun estaDisponible(ahora: LocalDateTime = LocalDateTime.now()): Boolean =
        reservas.none { it.fechaHasta >= ahora }

    constructor(data: LibroData) : this() {
        duenio          = data.duenio
        titulo          = validarTextoObligatorio(data.titulo, "título")
        autor           = validarTextoObligatorio(data.autor, "autor")
        descripcion     = validarTextoObligatorio(data.descripcion, "descripción")
        genero          = data.genero
        cantidadPaginas = validarPaginas(data.cantidadPaginas)
        isbn            = normalizarYValidarIsbn(data.isbn)
        idioma          = data.idioma
        editorial       = validarTextoObligatorio(data.editorial, "editorial")
        fechaPublicacion = data.fechaPublicacion
        fechaAgregado   = data.fechaAgregado
        estado          = data.estado
        imagenUrl       = data.imagenUrl
    }

    fun calcularBiblioKarmaTotal(
        usuario: Usuario,
        desde: LocalDateTime,
        hasta: LocalDateTime,
        cantidadReservasDelLibro: Int = 0,
    ): Int {
        if (!hasta.isAfter(desde)) throw BusinessException("La fecha de hasta debe ser posterior a la fecha de desde")
        val diasReserva = ChronoUnit.DAYS.between(desde.toLocalDate(), hasta.toLocalDate()).toInt() + 1
        return (5 * diasReserva) + plusBiblioKarma(usuario, cantidadReservasDelLibro)
    }

    fun actualizarPromedioCalificacion(promedioGeneral: Double) {
        calificacion = (promedioGeneral * 10.0).roundToInt() / 10.0
    }

    fun agregarCalificacion(nueva: Calificacion) {
        if (nueva.valor !in 1..5) throw BusinessException("La calificación debe estar entre 1 y 5")
        if (calificaciones.any { it.usuarioId == nueva.usuarioId }) {
            throw ConflictException("Usuario ${nueva.usuarioId} ya calificó este libro")
        }
        calificaciones.add(nueva)
        val promedio = calificaciones.map { it.valor }.average()
        actualizarPromedioCalificacion(promedio)
    }

    abstract fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int = 0): Int

    fun actualizarDatos(
        nuevoTitulo: String,
        nuevoAutor: String,
        nuevaDescripcion: String,
        nuevoGenero: Genero,
        nuevasPaginas: Int,
        nuevoIsbn: String,
        nuevoIdioma: Idioma,
        nuevaEditorial: String,
        nuevaFechaPublicacion: LocalDate,
        nuevoEstado: EstadoLibro,
        nuevaImagenUrl: String?,
    ) {
        titulo           = validarTextoObligatorio(nuevoTitulo, "título")
        autor            = validarTextoObligatorio(nuevoAutor, "autor")
        descripcion      = validarTextoObligatorio(nuevaDescripcion, "descripción")
        genero           = nuevoGenero
        cantidadPaginas  = validarPaginas(nuevasPaginas)
        isbn             = normalizarYValidarIsbn(nuevoIsbn)
        idioma           = nuevoIdioma
        editorial        = validarTextoObligatorio(nuevaEditorial, "editorial")
        fechaPublicacion = nuevaFechaPublicacion
        estado           = nuevoEstado
        imagenUrl        = nuevaImagenUrl
    }
}

class LibroComun : Libro {
    constructor() : super()
    constructor(data: LibroData) : super(data)

    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        if (cantidadPaginas <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
        val multiplicador = if (usuario.bibliokarmas < 1000) 5 else 2
        return cantidadPaginas * multiplicador
    }
}

class LibroConDedicatoria : Libro {
    constructor() : super()
    constructor(data: LibroData) : super(data)

    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        return 200 + (10 * cantidadReservasDelLibro)
    }
}

class LibroColeccionable : Libro {
    constructor() : super()
    constructor(data: LibroData) : super(data)

    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        if (usuario.bibliokarmas < 0) throw BusinessException("El usuario no puede tener bibliokarmas negativos")
        if (cantidadPaginas <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
        val karmaParte = ceil(usuario.bibliokarmas / 5.0).toInt()
        return karmaParte + cantidadPaginas
    }
}

data class ClickLog(
    val fechaHora: LocalDateTime = LocalDateTime.now(),
    val nombreUsuario: String,
)

data class ReservaFecha(
    val fechaDesde: LocalDateTime,
    val fechaHasta: LocalDateTime,
)