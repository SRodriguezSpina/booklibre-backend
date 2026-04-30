package ar.edu.unsam.phm.controllers


import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.services.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// Response compatible con el formato OAuth2 que espera Swagger UI
data class OAuth2TokenResponse(
    val access_token: String,
    val token_type: String = "bearer",
)

@RestController
@RequestMapping("/api")
class AuthController (private val authService: AuthService) {
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponseUsuario> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponseUsuario> {
        return ResponseEntity.ok(authService.register(request))
    }

    /**
     * Endpoint OAuth2 password-flow para Swagger UI.
     * Recibe username (= email) y password como form-urlencoded,
     * devuelve { access_token, token_type } que Swagger UI entiende nativamente.
     */
    @PostMapping("/auth/token", consumes = ["application/x-www-form-urlencoded"])
    fun swaggerToken(
        @RequestParam username: String,
        @RequestParam password: String,
    ): ResponseEntity<OAuth2TokenResponse> {
        val result = authService.login(LoginRequest(email = username, password = password))
        val token = result.token ?: throw IllegalStateException("Token no generado")
        return ResponseEntity.ok(OAuth2TokenResponse(access_token = token))
    }
}