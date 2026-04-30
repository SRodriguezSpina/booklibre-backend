package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.TipoUsuario
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.security.UserDetailsServiceImpl

import ar.edu.unsam.phm.exceptions.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UsuarioActualizadoConToken(
    val usuario: Usuario,
    val token: String,
)

@Service
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val libroRepository: LibroRepository,
    private val reservaRepository: ReservaRepository,
    private val jwtTokenUtils: JwtTokenUtils,
) {
    fun obtenerUsuario(id: Int): Usuario =
        usuarioRepository.getById(id)

    @Transactional
    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioActualizadoConToken {
        val usuario = usuarioRepository.getById(id)
        val email   = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular)

        usuarioRepository.validarEmailDisponible(email, id)
        usuarioRepository.validarCelularDisponible(celular, id)

        val tipoAnterior = usuario.tipoUsuarioDomain()

        if (request.tipoUsuario != usuario.tipoUsuario && reservaRepository.tieneReservasActivas(id)) {
            throw BusinessException("No podés cambiar el tipo de usuario mientras tenés reservas activas")
        }

        usuario.actualizarDatosPersonales(
            nuevoNombre    = request.nombre,
            nuevaDesc      = request.desc,
            nuevoCelular   = celular,
            nuevaCiudad    = request.ciudad,
            nuevoTipo      = request.tipoUsuario,
            nuevaImagenUrl = request.imagenUrl,
            nuevoEmail     = email,
        )
        val tipoActual = usuario.tipoUsuarioDomain()

        manejarCambioDeTipo(usuario, tipoAnterior, tipoActual)

        val actualizado = usuarioRepository.save(usuario)
        val roles = UserDetailsServiceImpl.rolesParaTipo(actualizado.tipoUsuario).map { it.authority }
        val nuevoToken = jwtTokenUtils.generateToken(actualizado.email, roles)

        return UsuarioActualizadoConToken(
            usuario = actualizado,
            token = nuevoToken,
        )
    }

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro> =
        libroRepository.getLibrosDeUsuario(usuarioId)

    private fun manejarCambioDeTipo(
        usuario: Usuario,
        tipoAnterior: TipoUsuario,
        tipoActual: TipoUsuario
    ) {
        val eraPublicador = tipoAnterior.puedePublicar()
        val ahoraEsPublicador = tipoActual.puedePublicar()

        if (eraPublicador == ahoraEsPublicador) return

        libroRepository.actualizarActivoPorDuenio(usuario.id, ahoraEsPublicador)
    }
}
