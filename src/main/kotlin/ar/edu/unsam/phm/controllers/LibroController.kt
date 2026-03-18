package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.Calificacion
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PagedResponse
import ar.edu.unsam.phm.services.LibroService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/libros")
class LibroController(val libroService: LibroService) {

    @GetMapping("/home")
    fun search(
        @RequestParam(defaultValue = "") query: String,
        @RequestParam(defaultValue = "") generos: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int,    //podría sacar el default..
        @RequestParam usuarioId: Int,
        @RequestParam(required = false) paginasMin: Int?,
        @RequestParam(required = false) paginasMax: Int?,
        @RequestParam(required = false) fechaDesde: String?,
        @RequestParam(required = false) fechaHasta: String?,
        @RequestParam(required = false) isbn: String?,
        @RequestParam(required = false) prestadoPor: String?
    ): PagedResponse<LibroHomeDTO> = libroService.search(query, generos, page, size, usuarioId, paginasMin, paginasMax, fechaDesde, fechaHasta, isbn, prestadoPor)

    @GetMapping("/{libroId}")
    fun getLibro(@PathVariable libroId: Int, @RequestParam usuarioId: Int
    ): LibroDTO =  libroService.getLibro(libroId, usuarioId)

    @PostMapping("/{libroId}/calificar")
    fun agregarCalificacion(
        @PathVariable libroId: Int,
        @RequestBody dto: Calificacion
    ) {
        libroService.agregarCalificacion(libroId, dto)
        println("Libro $libroId calificado con ${dto.valor} y mensaje: ${dto.comentario}")
    }
}