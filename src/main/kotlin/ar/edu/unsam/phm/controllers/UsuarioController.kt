package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.services.UsuarioService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/usuarios")
class UsuarioController(val usuarioService: UsuarioService) {

    @GetMapping("/{id}")
    fun getUsuario(@PathVariable id: Int): UsuarioResponse =
        usuarioService.obtenerUsuario(id)

    @PutMapping("/{id}")
    fun actualizarUsuario(@PathVariable id: Int, @RequestBody request: UsuarioUpdateRequest): UsuarioResponse =
        usuarioService.actualizarUsuario(id, request)

    @GetMapping("/{id}/libros")
    fun getLibrosDeUsuario(@PathVariable id: Int): List<LibroUsuarioItemDTO> =
        usuarioService.obtenerLibrosDeUsuario(id)


}
