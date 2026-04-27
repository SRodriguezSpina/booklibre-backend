package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.mappers.toLibroUsuarioItemDTO
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.services.ReservaService
import ar.edu.unsam.phm.services.UsuarioService
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/usuarios")
class UsuarioController(
    val usuarioService: UsuarioService,
    val reservaService: ReservaService,
) {

    @GetMapping("/{id}")
    fun getUsuario(@PathVariable id: Int): UsuarioResponse =
        usuarioService.obtenerUsuario(id).toResponse()

    @PutMapping("/{id}")
    fun actualizarUsuario(@PathVariable id: Int, @RequestBody request: UsuarioUpdateRequest): UsuarioUpdateResponse {
        val resultado = usuarioService.actualizarUsuario(id, request)
        return UsuarioUpdateResponse(
            usuario = resultado.usuario.toResponse(),
            token = resultado.token,
        )
    }

    @GetMapping("/{id}/libros")
    fun getLibrosDeUsuario(@PathVariable id: Int): List<LibroUsuarioItemDTO> =
        usuarioService.obtenerLibrosDeUsuario(id).map { libro -> libro.toLibroUsuarioItemDTO() }

    @GetMapping("/{id}/reservas/anuales")
    fun getReservasAnualesDeUsuario(@PathVariable id: Int): List<ReservaAnualUsuarioDTO> =
        reservaService.obtenerReservasAnuales(id)
}
