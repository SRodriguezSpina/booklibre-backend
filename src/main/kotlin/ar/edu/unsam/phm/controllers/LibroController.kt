package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.dtos.CalificacionDTO
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.services.LibroService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/libros")
class LibroController(val libroService: LibroService) {

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
    ): PagedResponse<LibroHomeDTO> {
        val pagedLibros = libroService.search(
            query, generos, page, size, usuarioId, paginasMin, paginasMax,
            fechaDesde, fechaHasta, isbn, prestadoPor, sortBy)
        val content = pagedLibros.content.map { libro ->
            libro.toHomeDTO(libroService.calcularBiblioKarma(libro, usuarioId))
        }
        return PagedResponse(content, pagedLibros.page, pagedLibros.totalPages)
    }

    @GetMapping("/{libroId}")
    fun getLibro(@PathVariable libroId: Int): LibroDTO {
        val libro = libroService.obtenerLibro(libroId)
        val reservas = libroService.obtenerReservasDeLibro(libroId)
        val nombresUsuarios = libroService.obtenerNombresUsuariosCalificaciones(libro)
        return libro.toDTO(
            todosLosRangos = reservas,
            nombresUsuarios = nombresUsuarios,
        )
    }

    @GetMapping("/{libroId}/calificaciones")
    fun getCalificaciones(@PathVariable libroId: Int): List<CalificacionDTO> = libroService.getCalificaciones(libroId)

    @PostMapping("/{libroId}/calificar")
    fun agregarCalificacion(@PathVariable libroId: Int, @RequestBody dto: Calificacion): Map<String, Double> {
        libroService.agregarCalificacion(libroId, dto)
        val nuevoRating = libroService.getRating(libroId)
        return mapOf("rating" to nuevoRating)
    }

    @PutMapping("/{libroId}")
    fun updateLibro(@PathVariable libroId: Int, @RequestParam usuarioId: Int, @RequestBody dto: LibroDTO
    ): LibroDTO = libroService.actualizarLibro(libroId, dto, usuarioId).toDTO()

    @DeleteMapping("/{libroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminarLibro(@PathVariable libroId: Int, @RequestParam usuarioId: Int) {
        libroService.eliminarLibro(libroId, usuarioId)
    }

    @PostMapping("/nuevo")
    fun crearLibro(@RequestParam usuarioId: Int, @RequestBody dto: LibroDTO): LibroDTO =
        libroService.crearLibro(dto, usuarioId).toDTO()
}