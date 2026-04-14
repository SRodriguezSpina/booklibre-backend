package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service

@Service
class AuthService(private val usuarioRepository: UsuarioRepository) {

    fun login(request: LoginRequest): AuthResponseUsuario {
        val usuario = usuarioRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("Usuario o contraseña incorrectos")
        if (!usuario.validarPassword(request.password))
            throw UnauthorizedException("Usuario o contraseña incorrectos")
        return AuthResponseUsuario(true, "Login exitoso", usuario.toResponse())
    }

    fun register(request: RegisterRequest): AuthResponseUsuario {
        usuarioRepository.validarEmailDisponible(request.email, -1)
        val nuevo = Usuario(
            email = request.email,
            password = request.password,
            nombre = request.nombre,
            desc = "",
        )
        val guardado = usuarioRepository.save(nuevo)
        return AuthResponseUsuario(true, "Registro exitoso", guardado.toResponse())
    }
}