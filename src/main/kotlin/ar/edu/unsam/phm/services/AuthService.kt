package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.AuthResponseUsuario
import ar.edu.unsam.phm.dtos.AuthTokens
import ar.edu.unsam.phm.dtos.LoginRequest
import ar.edu.unsam.phm.dtos.RegisterRequest
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.security.UserDetailsServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val usuarioJpa: UsuarioJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtils: JwtTokenUtils,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun login(request: LoginRequest): Pair<AuthResponseUsuario, AuthTokens> {
        val email = Usuario.normalizarYValidarEmail(request.email)
        val usuario = usuarioJpa.findByEmail(email)
            ?: throw UnauthorizedException("Usuario o contraseña incorrectos")
        if (!passwordEncoder.matches(request.password, usuario.password)) {
            if (!usuario.password.startsWith("\$2")) {
                logger.warn("Usuario con email {} tiene contraseña sin hashear (legacy). Requiere reset de contraseña.", email)
            }
            throw UnauthorizedException("Usuario o contraseña incorrectos")
        }
        val roles = UserDetailsServiceImpl.rolesParaUsuario(usuario).map { it.authority }
        val accessToken = jwtTokenUtils.generateToken(email, roles)
        val refreshToken = jwtTokenUtils.generateRefreshToken(email)

        return Pair(
            AuthResponseUsuario(true, "Login exitoso", usuario.toResponse()),
            AuthTokens(accessToken, refreshToken)
        )
    }

    fun register(request: RegisterRequest): Pair<AuthResponseUsuario, AuthTokens> {
        val email = Usuario.normalizarYValidarEmail(request.email)
        validarEmailDisponible(email)
        val nuevo = Usuario(
            email = email,
            password = passwordEncoder.encode(request.password),
            nombre = request.nombre,
            desc = "",
        )
        val guardado = usuarioJpa.save(nuevo)
        val roles = UserDetailsServiceImpl.rolesParaUsuario(guardado).map { it.authority }
        val accessToken = jwtTokenUtils.generateToken(email, roles)
        val refreshToken = jwtTokenUtils.generateRefreshToken(email)
        
        return Pair(
            AuthResponseUsuario(true, "Registro exitoso", guardado.toResponse()),
            AuthTokens(accessToken, refreshToken)
        )
    }

    fun refreshToken(refreshToken: String): AuthTokens {
        if (!jwtTokenUtils.validateToken(refreshToken)) {
            throw UnauthorizedException("Refresh token inválido o expirado")
        }
        val email = jwtTokenUtils.extractEmail(refreshToken)
        val usuario = usuarioJpa.findByEmail(email)
            ?: throw UnauthorizedException("Usuario no encontrado")
            
        val roles = UserDetailsServiceImpl.rolesParaUsuario(usuario).map { it.authority }
        val newAccessToken = jwtTokenUtils.generateToken(email, roles)
        // Opcional: rotar el refresh token también. Por ahora mantenemos el mismo.
        
        return AuthTokens(newAccessToken, refreshToken)
    }

    private fun validarEmailDisponible(email: String) {
        if (usuarioJpa.findByEmail(email) != null)
            throw ConflictException("El email ya está en uso")
    }
}