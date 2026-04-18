package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.persistence.entities.CalificacionEntity
import jakarta.persistence.*
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
    val imagenUrl: String? = null,
    val fechaAgregado: LocalDate = LocalDate.now(),
    val disponibilidad: Disponibilidad,
)

@Entity
@Table(name = "libros")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", discriminatorType = DiscriminatorType.STRING)
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
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "duenio_id", nullable = false)
    lateinit var duenio: Usuario

    @Column(nullable = false)
    var titulo: String = ""

    @Column(nullable = false)
    var autor: String = ""

    @Column(nullable = false, length = 2000)
    var descripcion: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var genero: Genero = Genero.DRAMA

    @Column(name = "cantidad_paginas", nullable = false)
    var cantidadPaginas: Int = 1

    @Column(nullable = false)
    var isbn: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var idioma: Idioma = Idioma.ESPANOL

    @Column(nullable = false)
    var editorial: String = ""

    @Column(nullable = false)
    var fechaPublicacion: LocalDate = LocalDate.now()

    @Column(nullable = false)
    var fechaAgregado: LocalDate = LocalDate.now()

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoLibro = EstadoLibro.BUENO

    @Column(nullable = true)
    var imagenUrl: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var disponibilidad: Disponibilidad = Disponibilidad.DISPONIBLE

    @OneToMany(mappedBy = "libro", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    var calificacionesJpa: MutableList<CalificacionEntity> = mutableListOf()

    val calificaciones: List<Calificacion>
        get() = calificacionesJpa.map { Calificacion(it.usuarioId, it.valor, it.comentario) }

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
        disponibilidad  = data.disponibilidad
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
        if (calificacionesJpa.any { it.usuarioId == nueva.usuarioId })
            throw BusinessException("Usuario ${nueva.usuarioId} ya calificó este libro")
        calificacionesJpa.add(
            CalificacionEntity(libro = this, usuarioId = nueva.usuarioId, valor = nueva.valor, comentario = nueva.comentario),
        )
    }

    fun getCalificacion(): Double {
        if (calificacionesJpa.isEmpty()) return 0.0
        val promedio = calificacionesJpa.map { it.valor }.average()
        return (promedio * 10.0).roundToInt() / 10.0
    }

    fun estaDisponible() = disponibilidad == Disponibilidad.DISPONIBLE

    fun actualizoDisponibilidad(estaDisponible: Boolean) {
        disponibilidad = if (estaDisponible) Disponibilidad.DISPONIBLE else Disponibilidad.RESERVADO
    }

    abstract fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int = 0): Int

    fun cumpleFiltros(filtros: LibroFiltros): Boolean {
        val matchQuery = filtros.query.isBlank() ||
            titulo.contains(filtros.query, ignoreCase = true) ||
            autor.contains(filtros.query, ignoreCase = true)
        val matchGenero = filtros.generos.isEmpty() || filtros.generos.contains(genero.name)
        val matchPaginasMin = filtros.paginasMin == null || cantidadPaginas >= filtros.paginasMin
        val matchPaginasMax = filtros.paginasMax == null || cantidadPaginas <= filtros.paginasMax
        val matchIsbn = filtros.isbn.isNullOrBlank() || isbn.contains(filtros.isbn, ignoreCase = true)
        val matchPrestadoPor = filtros.prestadoPor.isNullOrBlank() ||
            duenio.nombre.contains(filtros.prestadoPor, ignoreCase = true)
        val matchDisponibilidad = filtros.libroIdsReservados == null || id !in filtros.libroIdsReservados
        return matchQuery && matchGenero && matchPaginasMin && matchPaginasMax &&
            matchIsbn && matchPrestadoPor && matchDisponibilidad
    }

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

@Entity
@DiscriminatorValue("Comun")
class LibroComun : Libro {
    constructor() : super()
    constructor(data: LibroData) : super(data)

    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        if (cantidadPaginas <= 0) throw BusinessException("La cantidad de páginas debe ser mayor a 0")
        val multiplicador = if (usuario.bibliokarmas < 1000) 5 else 2
        return cantidadPaginas * multiplicador
    }
}

@Entity
@DiscriminatorValue("ConDedicatoria")
class LibroConDedicatoria : Libro {
    constructor() : super()
    constructor(data: LibroData) : super(data)

    override fun plusBiblioKarma(usuario: Usuario, cantidadReservasDelLibro: Int): Int {
        return 200 + (10 * cantidadReservasDelLibro)
    }
}

@Entity
@DiscriminatorValue("Coleccionable")
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