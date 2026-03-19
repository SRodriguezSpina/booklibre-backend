package ar.edu.unsam.phm.controllers


import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.services.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@CrossOrigin(origins = ["*"])
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
}