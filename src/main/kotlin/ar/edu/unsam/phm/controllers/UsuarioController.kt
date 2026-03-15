package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.services.UsuarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/usuarios")
class UsuarioController(val usuarioService: UsuarioService) {

    @GetMapping("/{id}")
    fun getUsuario(@PathVariable id: Int): ResponseEntity<UsuarioResponse> {
        val usuario = usuarioService.obtenerUsuario(id)
        return if (usuario != null) ResponseEntity.ok(usuario)
        else ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}")
    fun actualizarUsuario(@PathVariable id: Int, @RequestBody request: UsuarioUpdateRequest): ResponseEntity<UsuarioResponse> {
        val actualizado = usuarioService.actualizarUsuario(id, request)
        return if (actualizado != null) ResponseEntity.ok(actualizado)
        else ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }

    @GetMapping("/{id}/libros")
    fun getLibrosDeUsuario(@PathVariable id: Int): List<LibroUsuarioItemDTO> =
        usuarioService.obtenerLibrosDeUsuario(id)
}
