package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.repository.UsuarioRepository
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.security.UserDetailsServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtils: JwtTokenUtils,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun login(request: LoginRequest): AuthResponseUsuario {
        val email = Usuario.normalizarYValidarEmail(request.email)
        val usuario = usuarioRepository.findByEmail(email)
            ?: throw UnauthorizedException("Usuario o contraseña incorrectos")
        if (!passwordEncoder.matches(request.password, usuario.password)) {
            if (!usuario.password.startsWith("\$2")) {
                logger.warn("Usuario con email {} tiene contraseña sin hashear (legacy). Requiere reset de contraseña.", email)
            }
            throw UnauthorizedException("Usuario o contraseña incorrectos")
        }
        val roles = UserDetailsServiceImpl.rolesParaTipo(usuario.tipoUsuario).map { it.authority }
        val token = jwtTokenUtils.generateToken(email, roles)
        return AuthResponseUsuario(true, "Login exitoso", usuario.toResponse(), token)
    }

    fun register(request: RegisterRequest): AuthResponseUsuario {
        val email = Usuario.normalizarYValidarEmail(request.email)
        usuarioRepository.validarEmailDisponible(email, -1)
        val nuevo = Usuario(
            email = email,
            password = passwordEncoder.encode(request.password),
            nombre = request.nombre,
            desc = "",
        )
        val guardado = usuarioRepository.save(nuevo)
        val roles = UserDetailsServiceImpl.rolesParaTipo(guardado.tipoUsuario).map { it.authority }
        val token = jwtTokenUtils.generateToken(email, roles)
        return AuthResponseUsuario(true, "Registro exitoso", guardado.toResponse(), token)
    }
}