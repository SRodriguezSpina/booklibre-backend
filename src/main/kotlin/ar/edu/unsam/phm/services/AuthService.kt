package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.Credencial
import ar.edu.unsam.phm.Usuario
import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.exceptions.*
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service

@Service
class AuthService (private val usuarioRepository : UsuarioRepository) {
    fun login(request: LoginRequest): AuthResponseUsuario {
        val usuario = usuarioRepository.lista
            .find { it.getCredencial().email == request.email }
            ?: throw UnauthorizedException("Usuario o contraseña incorrectos")

        if (usuario.validarPassword(request.password))
            throw UnauthorizedException("Usuario o contraseña incorrectos")

        return AuthResponseUsuario(true, "Login exitoso", usuario.toResponse())
    }

    fun register(request: RegisterRequest): AuthResponseUsuario {
        if (usuarioRepository.lista.any { it.getCredencial().email == request.email })
            throw ConflictException("El nombre de usuario ya está en uso")

        if (usuarioRepository.lista.any { it.email == request.email })
            throw ConflictException("El email ya está registrado")

        val nuevo = Usuario(
            credencial = Credencial(request.email, request.password),
            nombre = request.nombre,
            desc = "",
            email = request.email
        )
        usuarioRepository.create(nuevo)
        return AuthResponseUsuario(true, "Registro exitoso", nuevo.toResponse())
    }
}