package ar.edu.unsam.phm.testing.inmemory.services

import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.testing.inmemory.repository.UsuarioRepository

class AuthServiceInMemory(private val usuarioRepository: UsuarioRepository) {
    fun login(request: LoginRequest): AuthResponseUsuario {
        val usuario = usuarioRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("Usuario o contraseña incorrectos")

        if (!usuario.validarPassword(request.password)) {
            throw UnauthorizedException("Usuario o contraseña incorrectos")
        }

        return AuthResponseUsuario(true, "Login exitoso", usuario.toResponse())
    }

    fun register(request: RegisterRequest): AuthResponseUsuario {
        if (usuarioRepository.lista.any { it.email == request.email }) {
            throw ConflictException("El email ya está registrado")
        }

        val nuevo = Usuario(
            request.email, request.password,
            nombre = request.nombre,
            desc = "",
        )
        usuarioRepository.create(nuevo)
        return AuthResponseUsuario(true, "Registro exitoso", nuevo.toResponse())
    }
}