package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class UsuarioService(
    val usuarioRepository: UsuarioRepository,
    val libroRepository: LibroRepository,
) {
    fun obtenerUsuario(id: Int): Usuario =
        usuarioRepository.getById(id)

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro> =
        libroRepository.getLibrosDeUsuario(usuarioId)

    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): Usuario {
        val usuario = usuarioRepository.getById(id)
        val email = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular)

        usuarioRepository.validarEmailDisponible(email, id)
        usuarioRepository.validarCelularDisponible(celular, id)

        usuario.actualizarDatosPersonales(
            nuevoNombre   = request.nombre,
            nuevaDesc     = request.desc,
            nuevoCelular  = celular,
            nuevaCiudad   = request.ciudad,
            nuevoTipo     = request.tipoUsuario,
            nuevaImagenUrl = request.imagenUrl,
            nuevoEmail    = email,
        )

        usuarioRepository.update(usuario)
        return usuario
    }
}