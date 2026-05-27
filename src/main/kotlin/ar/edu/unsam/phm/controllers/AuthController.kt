package ar.edu.unsam.phm.controllers


import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.services.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

// Response compatible con el formato OAuth2 que espera Swagger UI
data class OAuth2TokenResponse(
    val access_token: String,
    val token_type: String = "bearer",
)

@RestController
@RequestMapping("/api")
class AuthController (private val authService: AuthService) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponseUsuario> {
        val (responseBody, tokens) = authService.login(request)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createCookie("accessToken", tokens.accessToken, 900).toString()) // 15 min
            .header(HttpHeaders.SET_COOKIE, createCookie("refreshToken", tokens.refreshToken, 604800).toString()) // 7 dias
            .body(responseBody)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponseUsuario> {
        val (responseBody, tokens) = authService.register(request)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createCookie("accessToken", tokens.accessToken, 900).toString())
            .header(HttpHeaders.SET_COOKIE, createCookie("refreshToken", tokens.refreshToken, 604800).toString())
            .body(responseBody)
    }

    @PostMapping("/refresh")
    fun refresh(@CookieValue(name = "refreshToken", required = true) refreshToken: String): ResponseEntity<Void> {
        val newTokens = authService.refreshToken(refreshToken)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createCookie("accessToken", newTokens.accessToken, 900).toString())
            .header(HttpHeaders.SET_COOKIE, createCookie("refreshToken", newTokens.refreshToken, 604800).toString())
            .build()
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Void> {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, createCookie("accessToken", "", 0).toString())
            .header(HttpHeaders.SET_COOKIE, createCookie("refreshToken", "", 0).toString())
            .build()
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
        val (_, tokens) = authService.login(LoginRequest(email = username, password = password))
        return ResponseEntity.ok(OAuth2TokenResponse(access_token = tokens.accessToken))
    }
}