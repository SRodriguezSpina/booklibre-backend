package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.Libro
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.dtos.LibroHomeDTO
import ar.edu.unsam.phm.dtos.PrestamoDTO
import ar.edu.unsam.phm.services.LibroService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
class LibroController(val libroService: LibroService) {

    @GetMapping("/libros")
    fun getAll(): List<LibroHomeDTO> = libroService.getAll()

    @GetMapping("/libros/{libroId}")
    fun getLibro(@PathVariable libroId: Int, @RequestParam usuarioId: Int
    ): LibroDTO =  libroService.getLibro(libroId, usuarioId)

    @GetMapping("/usuarios/{usuarioId}/prestamos")
    fun getPrestamos(@PathVariable usuarioId: Int, @RequestParam tipo: String
    ): List<PrestamoDTO> = libroService.obtenerPrestamos(usuarioId, tipo)
}