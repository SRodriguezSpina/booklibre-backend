package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.services.LibroService
import ar.edu.unsam.phm.services.ClickLogService
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/libros")
class LibroController(
    val libroService: LibroService,
    val clickLogService: ClickLogService,
) {

    @Transactional(readOnly = true)
    @GetMapping("/home")
    fun search(
        @RequestParam(defaultValue = "") query: String,
        @RequestParam(defaultValue = "") generos: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int,
        @RequestParam(required = false) usuarioId: Int?,
        @RequestParam(required = false) paginasMin: Int?,
        @RequestParam(required = false) paginasMax: Int?,
        @RequestParam(required = false) fechaDesde: String?,
        @RequestParam(required = false) fechaHasta: String?,
        @RequestParam(required = false) isbn: String?,
        @RequestParam(required = false) prestadoPor: String?,
        @RequestParam(defaultValue = "titulo") sortBy: String?,
    ): PagedResponse<LibroHomeDTO> =
        libroService.search(
            query, generos, page, size, usuarioId, paginasMin, paginasMax,
            fechaDesde, fechaHasta, isbn, prestadoPor, sortBy,
        ).toHomeDTO { libro -> libroService.calcularBiblioKarma(libro, usuarioId) }

    // Redis, Libros populares
    @Transactional(readOnly = true)
    @GetMapping("/populares")
    fun getLibrosPopulares(
        @RequestParam(required = false) usuarioId: Int?,
    ): PagedResponse<LibroHomeDTO> =
        libroService.getLibrosPopulares(usuarioId)

    @Transactional(readOnly = true)
    @GetMapping("/{libroId}")
    fun getLibro(
        @PathVariable libroId: Int,
        auth: Authentication?,   // nullable: el endpoint es permitAll(), puede venir sin token
        @RequestParam(required = false) usuarioId: Int?,
    ): LibroDTO {
        val libro = libroService.obtenerLibro(libroId)

        // Registrar el click solo si hay un usuario autenticado y NO es el propietario del libro
        auth?.name?.let { nombreUsuario ->
            val esDuenio = clickLogService.esEmailDelDuenio(libro.duenio.id, nombreUsuario)
            if (!esDuenio) {
                clickLogService.registrarClick(libroId, libro.titulo, nombreUsuario)
            }
        }

        val alquiladoPorId = libroService.obtenerPrestatarioActual(libroId)
        val totalReservas = libroService.contarReservasPorLibro(libroId)
        val calificaciones = libroService.getCalificacionesDetalle(libroId)
        val reservasActivas = libroService.obtenerRangosReservasActivas(libro)
        val estoyReservado = usuarioId?.let { libroService.usuarioTieneReservaPendiente(libroId, it) } ?: false
        val miReservaHasta = if (estoyReservado && usuarioId != null) libroService.obtenerMiReservaHasta(libroId, usuarioId) else null
        return libro.toDTO(
            alquiladoPorId = alquiladoPorId,
            estaDisponible = !estoyReservado,
            calificaciones = calificaciones,
            totalReservas  = totalReservas,
            reservas       = reservasActivas,
            miReservaFechaHasta = miReservaHasta,
        )
    }

    // Dante 1 — Query: libro más clickeado
    @GetMapping("/mas-clickeado")
    fun getLibroMasClickeado() =
        clickLogService.getLibroMasClickeado()?.let {
            mapOf(
                "libroId" to it.libroId,
                "libroTitulo" to it.libroTitulo,
                "total" to it.total,
            )
        } ?: mapOf("mensaje" to "Todavía no hay clicks registrados")

    // Query: Saber cuántos libros son del tipo coleccionables s/aggregation
    @GetMapping("/coleccionables/count")
    fun contarColeccionables(): Map<String, Long> =
        mapOf("cantidad" to libroService.contarLibrosColecciones())

    // Lolo: Saber qué libros tienen todas las reservas cumplidas
    @GetMapping("/reservas-cumplidas")
    fun getLibrosConReservasCumplidas(
        @RequestParam(required = false) usuarioId: Int?
    ): List<LibroHomeDTO> =
        libroService.getLibrosConReservasCumplidas()
            .map { it.toHomeDTO(libroService.calcularBiblioKarma(it, usuarioId)) }

    // Dami: libros con al menos N reservas activas
    @GetMapping("/reservas-activas")
    fun getLibrosConReservasActivas(
        @RequestParam(required = false) usuarioId: Int?,
        @RequestParam(defaultValue = "3") minReservas: Long,
    ): List<LibroHomeDTO> =
        libroService.getLibrosConReservasActivasMayorA(minReservas)
            .map { it.toHomeDTO(libroService.calcularBiblioKarma(it, usuarioId)) }

    // Query: Saber qué libros tienen más de 4 puntos de calificación
    @GetMapping("/mejor-calificados")
    fun getLibrosMejorCalificados(): List<LibroHomeDTO> =
        libroService.getLibrosConPromedioMayorA4()
            .map { it.toHomeDTO(null) }

    @GetMapping("/{libroId}/calificaciones")
    fun getCalificaciones(@PathVariable libroId: Int): List<CalificacionDTO> =
        libroService.getCalificaciones(libroId)

    @PostMapping("/{libroId}/calificar")
    fun agregarCalificacion(@PathVariable libroId: Int, @RequestBody dto: NewCalificacionDTO): Map<String, Double> {
        libroService.agregarCalificacion(libroId, dto)
        return mapOf("rating" to libroService.getRating(libroId))
    }

    @PutMapping("/{libroId}")
    fun updateLibro(
        @PathVariable libroId: Int,
        @RequestParam usuarioId: Int,
        @Valid @RequestBody dto: LibroDTO,
    ): LibroDTO = libroService.actualizarLibro(libroId, dto, usuarioId).toDTO()

    @PostMapping("/nuevo")
    fun crearLibro(@RequestParam usuarioId: Int, @Valid @RequestBody dto: LibroDTO): LibroDTO =
        libroService.crearLibro(dto, usuarioId).toDTO()

    @GetMapping("/{id}/historial-puntajes")
    fun getHistorial(@PathVariable id: Int): List<HistorialPuntaje> =
        libroService.obtenerHistorialDePuntajes(id)

    @PutMapping("/{libroId}/baja")
    fun darDeBaja(
        @PathVariable libroId: Int,
        @RequestParam usuarioId: Int,
    ) = libroService.darDeBaja(libroId, usuarioId)

    @PutMapping("/{libroId}/reactivar")
    fun reactivar(
        @PathVariable libroId: Int,
        @RequestParam usuarioId: Int,
    ) = libroService.reactivar(libroId, usuarioId)

    @DeleteMapping("/{libroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminarLibro(
        @PathVariable libroId: Int,
        @RequestParam usuarioId: Int,
    ) {
        libroService.eliminarLibro(libroId, usuarioId)
    }
}
