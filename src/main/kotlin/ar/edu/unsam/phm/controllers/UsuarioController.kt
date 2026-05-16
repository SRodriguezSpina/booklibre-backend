package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.ClickLogDTO
import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.ReservaAnualUsuarioDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.mappers.toLibroUsuarioItemDTO
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.exceptions.ForbiddenException
import ar.edu.unsam.phm.services.ClickLogService
import ar.edu.unsam.phm.services.ReservaService
import ar.edu.unsam.phm.services.UsuarioService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController(
    val usuarioService: UsuarioService,
    val reservaService: ReservaService,
    val clickLogService: ClickLogService,
) {

    @GetMapping("/{id}")
    fun getUsuario(@PathVariable id: Int): UsuarioResponse =
        usuarioService.obtenerUsuario(id).toResponse()

    @PutMapping("/{id}")
    fun actualizarUsuario(
        @PathVariable id: Int,
        @RequestBody request: UsuarioUpdateRequest,
        auth: Authentication,
    ): UsuarioUpdateResponse {
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        val isOwner = usuarioService.obtenerUsuario(id).email == auth.name
        if (!isAdmin && !isOwner) throw ForbiddenException("No tenés permiso para modificar este usuario")
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

    /**
     * Bonus — Historial de clicks en los libros del publicador.
     * GET /api/usuarios/{id}/clicks
     * Muestra quién y cuándo vio los libros de este usuario (publicador).
     */
    @GetMapping("/{id}/clicks")
    fun getClicksDeLibrosDePublicador(@PathVariable id: Int): List<ClickLogDTO> =
        clickLogService.getClicksDeLibrosDeUsuario(id)
            .map { ClickLogDTO(it.libroId, it.libroTitulo, it.nombreUsuario, it.fechaHora.toString()) }
}
