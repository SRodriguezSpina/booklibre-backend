package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.PrestamoDTO
import ar.edu.unsam.phm.services.LibroService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
class LibroController(val libroService: LibroService) {

    @GetMapping("/libros/{id}")
    fun getLibro(
        @PathVariable id: Int,
        @RequestParam usuarioId: Int
    ): LibroDTO =  libroService.getLibro(id, usuarioId)

    @GetMapping("/usuarios/{usuarioId}/prestamos")
    fun getPrestamos(
        @PathVariable usuarioId: Int,
        @RequestParam tipo: String
    ): List<PrestamoDTO> = libroService.obtenerPrestamos(usuarioId, tipo)
}