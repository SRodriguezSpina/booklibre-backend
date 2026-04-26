package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.TipoUsuario
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
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

        val tipoAnterior = usuario.tipoUsuarioDomain()

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

        return usuarioRepository.save(usuario)
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

        val libros = libroRepository.getTodosLosLibrosDeUsuario(usuario.id)

        libros.forEach {
            it.activo = ahoraEsPublicador
            libroRepository.save(it)
        }
    }
}