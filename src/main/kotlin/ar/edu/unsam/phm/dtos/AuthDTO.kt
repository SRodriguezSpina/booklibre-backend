package ar.edu.unsam.phm.dtos

data class LoginRequest(
    val email: String,
    val password: String
)

class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String
)

class AuthResponseUsuario (
     val success: Boolean,
     val message: String? = null,
     val usuario: UsuarioResponse? = null
)
