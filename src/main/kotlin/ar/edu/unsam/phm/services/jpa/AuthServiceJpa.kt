package ar.edu.unsam.phm.services.jpa

import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.persistence.entities.UsuarioEntity
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.repositories.UsuarioJpaRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("jpa")
class AuthServiceJpa(private val usuarioJpaRepository: UsuarioJpaRepository) {
    fun login(request: LoginRequest): AuthResponseUsuario {
        val usuario = usuarioJpaRepository.findByEmail(request.email)
            ?: throw UnauthorizedException("Usuario o contraseña incorrectos")

        if (usuario.password != request.password) {
            throw UnauthorizedException("Usuario o contraseña incorrectos")
        }

        return AuthResponseUsuario(true, "Login exitoso", usuario.toResponseEntity())
    }

    fun register(request: RegisterRequest): AuthResponseUsuario {
        if (usuarioJpaRepository.existsByEmail(request.email)) {
            throw ConflictException("El email ya está registrado")
        }

        val nuevo = UsuarioEntity(
            email = request.email,
            password = request.password,
            nombre = request.nombre,
            desc = "",
        )
        val guardado = usuarioJpaRepository.save(nuevo)
        return AuthResponseUsuario(true, "Registro exitoso", guardado.toResponseEntity())
    }

    private fun UsuarioEntity.toResponseEntity() = this.toDomainForResponse().toResponse()

    private fun UsuarioEntity.toDomainForResponse() = this.toDomain()
}

