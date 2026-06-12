package ar.edu.unsam.phm.controllers

import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.exceptions.ForbiddenException
import ar.edu.unsam.phm.services.ClickLogService
import ar.edu.unsam.phm.services.ReservaService
import ar.edu.unsam.phm.services.UsuarioService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity

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
    ): ResponseEntity<UsuarioUpdateResponse> {
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
        val isOwner = usuarioService.obtenerUsuario(id).email == auth.name
        if (!isAdmin && !isOwner) throw ForbiddenException("No tenés permiso para modificar este usuario")
        val resultado = usuarioService.actualizarUsuario(id, request)
        
        val responseBody = UsuarioUpdateResponse(
            usuario = resultado.usuario.toResponse()
        )
        
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createCookie("accessToken", resultado.tokens.accessToken, 900).toString())
            .header(HttpHeaders.SET_COOKIE, createCookie("refreshToken", resultado.tokens.refreshToken, 604800).toString())
            .body(responseBody)
    }

    private fun createCookie(name: String, value: String, maxAge: Long): ResponseCookie {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            // .secure(true) // Descomentar en producción (requiere HTTPS)
            .path("/")
            .maxAge(maxAge)
            .sameSite("Strict")
            .build()
    }

    @GetMapping("/{id}/libros")
    fun getLibrosDeUsuario(
        @PathVariable id: Int,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "todos") filtro: String,
        @RequestParam(defaultValue = "titulo") ordenarPor: String,
        @RequestParam(defaultValue = "ascendente") direccion: String,
    ): PagedResponse<LibroUsuarioItemDTO> =
        usuarioService.obtenerLibrosDeUsuarioPaginado(id, filtro, ordenarPor, direccion, page, size)

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
