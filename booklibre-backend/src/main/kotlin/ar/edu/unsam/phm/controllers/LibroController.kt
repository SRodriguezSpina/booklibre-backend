package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.services.LibroService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"]) // Permite que tu frontend de React se conecte [cite: 48]
@RequestMapping("/usuarios")
class LibroController(val libroService: LibroService) {

    @GetMapping("/{usuarioId}/prestamos")
    fun getPrestamos(
        @PathVariable usuarioId: Int,
        @RequestParam tipo: String // "prestados-a-mi" o "prestados-por-mi"
    ): List<LibroDTO> {
        return libroService.obtenerPrestamos(usuarioId, tipo)
    }
}