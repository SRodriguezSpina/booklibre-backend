package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.services.LibroService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
class LibroController(val libroService: LibroService) {

    @GetMapping("/libros/{libroId}")
    fun getLibro(@PathVariable libroId: Int, @RequestParam usuarioId: Int
    ): LibroDTO =  libroService.getLibro(libroId, usuarioId)
}