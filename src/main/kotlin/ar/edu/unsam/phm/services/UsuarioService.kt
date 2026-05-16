package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.TipoUsuario
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.security.UserDetailsServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UsuarioActualizadoConToken(
    val usuario: Usuario,
    val token: String,
)

@Service
class UsuarioService(
    private val usuarioJpa: UsuarioJpaRepository,
    private val libroRepository: LibroRepository,
    private val reservaJpa: ReservaJpaRepository,
    private val jwtTokenUtils: JwtTokenUtils,
) {
    fun obtenerUsuario(id: Int): Usuario =
        usuarioJpa.findById(id).orElseThrow { NotFoundException("Usuario $id no encontrado") }

    @Transactional
    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioActualizadoConToken {
        val usuario = usuarioJpa.findById(id).orElseThrow { NotFoundException("Usuario $id no encontrado") }
        val email   = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular)

        validarEmailDisponible(email, id)
        validarCelularDisponible(celular, id)

        val tipoAnterior = usuario.tipoUsuarioDomain()

        if (request.tipoUsuario != usuario.tipoUsuario && reservaJpa.tieneReservasActivas(id)) {
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

        val actualizado = usuarioJpa.save(usuario)
        val roles = UserDetailsServiceImpl.rolesParaTipo(actualizado.tipoUsuario).map { it.authority }
        val nuevoToken = jwtTokenUtils.generateToken(actualizado.email, roles)

        return UsuarioActualizadoConToken(
            usuario = actualizado,
            token = nuevoToken,
        )
    }

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro> =
        libroRepository.getLibrosDeUsuario(usuarioId)

    // Validaciones de unicidad — incumben al service porque tocan el repositorio
    private fun validarEmailDisponible(email: String, idActual: Int) {
        val existente = usuarioJpa.findByEmail(email)
        if (existente != null && existente.id != idActual)
            throw ConflictException("El email ya está en uso")
    }

    private fun validarCelularDisponible(celular: String, idActual: Int) {
        val existente = usuarioJpa.findByCelular(celular)
        if (existente != null && existente.id != idActual)
            throw ConflictException("El celular ya está en uso")
    }

    private fun manejarCambioDeTipo(
        usuario: Usuario,
        tipoAnterior: TipoUsuario,
        tipoActual: TipoUsuario,
    ) {
        val eraPublicador    = tipoAnterior.puedePublicar()
        val ahoraEsPublicador = tipoActual.puedePublicar()
        if (eraPublicador == ahoraEsPublicador) return
        libroRepository.actualizarActivoPorDuenio(usuario.id, ahoraEsPublicador)
    }
}
