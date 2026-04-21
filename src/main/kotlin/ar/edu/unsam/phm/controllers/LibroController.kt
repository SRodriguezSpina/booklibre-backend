package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.mappers.toHomeDTO
import ar.edu.unsam.phm.persistence.entities.HistorialPuntaje
import ar.edu.unsam.phm.services.LibroService
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/libros")
class LibroController(val libroService: LibroService) {

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

    @Transactional(readOnly = true)
    @GetMapping("/{libroId}")
    fun getLibro(@PathVariable libroId: Int): LibroDTO {
        val libro = libroService.obtenerLibro(libroId)
        val reservas = libroService.obtenerReservasDeLibro(libroId)
        val nombresUsuarios = libroService.obtenerNombresUsuariosCalificaciones(libro)
        return libro.toDTO(todosLosRangos = reservas, nombresUsuarios = nombresUsuarios)
    }

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
        @RequestBody dto: LibroDTO,
    ): LibroDTO = libroService.actualizarLibro(libroId, dto, usuarioId).toDTO()

    @DeleteMapping("/{libroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminarLibro(@PathVariable libroId: Int, @RequestParam usuarioId: Int) {
        libroService.eliminarLibro(libroId, usuarioId)
    }

    @PostMapping("/nuevo")
    fun crearLibro(@RequestParam usuarioId: Int, @RequestBody dto: LibroDTO): LibroDTO =
        libroService.crearLibro(dto, usuarioId).toDTO()

    @GetMapping("/{id}/historial-puntajes")
    fun getHistorial(@PathVariable id: Int): List<HistorialPuntaje> =
        libroService.obtenerHistorialDePuntajes(id)

    @PutMapping("/{libroId}/baja")
    fun darDeBaja(
        @PathVariable libroId: Int,
        @RequestParam usuarioId: Int
    ) = libroService.darDeBaja(libroId, usuarioId)

    @PutMapping("/{libroId}/reactivar")
    fun reactivar(
        @PathVariable libroId: Int,
        @RequestParam usuarioId: Int
    ) = libroService.reactivar(libroId, usuarioId)
}