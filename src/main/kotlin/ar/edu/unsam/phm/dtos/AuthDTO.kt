package ar.edu.unsam.phm.dtos

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "El email no puede estar vacío")
    @field:Email(message = "Debe ser un email válido")
    val email: String,

    @field:NotBlank(message = "La contraseña no puede estar vacía")
    val password: String
)

data class RegisterRequest(
    @field:NotBlank(message = "El nombre no puede estar vacío")
    val nombre: String,

    @field:NotBlank(message = "El email no puede estar vacío")
    @field:Email(message = "Debe ser un email válido")
    val email: String,

    @field:NotBlank(message = "La contraseña no puede estar vacía")
    @field:Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    val password: String
)

data class AuthResponseUsuario(
    val success: Boolean,
    val message: String? = null,
    val usuario: UsuarioResponse? = null,
)

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)
