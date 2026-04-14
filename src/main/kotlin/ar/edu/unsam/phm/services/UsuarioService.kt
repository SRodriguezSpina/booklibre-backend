package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.UsuarioRepository

import org.springframework.stereotype.Service
@Service
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val libroRepository: LibroRepository,
) {
    fun obtenerUsuario(id: Int): Usuario =
        usuarioRepository.getById(id)

    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): Usuario {
        val usuario = usuarioRepository.getById(id)
        val email   = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular)

        usuarioRepository.validarEmailDisponible(email, id)
        usuarioRepository.validarCelularDisponible(celular, id)

        usuario.actualizarDatosPersonales(
            nuevoNombre    = request.nombre,
            nuevaDesc      = request.desc,
            nuevoCelular   = celular,
            nuevaCiudad    = request.ciudad,
            nuevoTipo      = parseTipoUsuario(request.tipoUsuario),
            nuevaImagenUrl = request.imagenUrl,
            nuevoEmail     = email,
        )

        return usuarioRepository.save(usuario)
    }

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro> =
        libroRepository.getLibrosDeUsuario(usuarioId)

    private fun parseTipoUsuario(value: String): String {
        val normalized = value.trim().uppercase().replace(" ", "_")
        return when (normalized) {
            "LECTOR"                                -> "LECTOR"
            "PUBLICADOR"                            -> "PUBLICADOR"
            "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> "LECTOR_PUBLICADOR"
            else -> throw BusinessException("Tipo de usuario inválido: $value")
        }
    }
}