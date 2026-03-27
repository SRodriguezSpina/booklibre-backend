package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.repository.EntityNotFoundException
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
    private val reservaRepository: ReservaRepository,
) {
    private fun getUsuarioOrThrow(id: Int): Usuario {
        return try {
            usuarioRepository.getById(id)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Usuario no encontrado")
        }
    }

    fun obtenerUsuario(id: Int): UsuarioResponse {
        val usuario = getUsuarioOrThrow(id)
        return usuario.toResponse()
    }

    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioResponse {
        val existente = getUsuarioOrThrow(id)

        val email = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular)
        validarDuplicados(id, email, celular)
        val tipoUsuario = parseTipoUsuario(request.tipoUsuario)
        
        val actualizado = Usuario(
            credencial = Credencial(email, existente.getCredencial().password),
            nombre = request.nombre,
            desc = request.desc,
            celular = celular,
            ciudad = request.ciudad,
            tipoUsuario = tipoUsuario,
            bibliokarmas = existente.bibliokarmas,
            imagenUrl = request.imagenUrl ?: existente.imagenUrl,
            fechaRegistro = existente.fechaRegistro,
            reservas = existente.reservas.toMutableList(),
        )
        actualizado.id = existente.id
        usuarioRepository.update(actualizado)
        return actualizado.toResponse()
    }

    private fun parseTipoUsuario(value: String): TipoUsuario {
        val normalized = value.trim().uppercase().replace(" ", "_")
        return when (normalized) {
            "LECTOR" -> UsuarioLector()
            "PUBLICADOR" -> UsuarioPublicador()
            "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> UsuarioLectorPublicador()
            else -> throw BusinessException("Tipo de usuario inválido: $value")
        }
    }

    private fun validarDuplicados(idUsuarioActual: Int, email: String, celular: String) {
        val usuarioConEmail = usuarioRepository.findByEmail(email)
        if (usuarioConEmail != null && usuarioConEmail.id != idUsuarioActual) {
            throw ConflictException("El email ya esta en uso")
        }

        val usuarioConCelular = usuarioRepository.findByCelular(celular)
        if (usuarioConCelular != null && usuarioConCelular.id != idUsuarioActual) {
            throw ConflictException("El celular ya esta en uso")
        }
    }

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<LibroUsuarioItemDTO> {
        val hoy = LocalDate.now()
        return libroRepository.getLibros()
            .filter { it.duenio.id == usuarioId }
            .map { libro ->
                val disponible = !reservaRepository.existeSuperposicion(libro.id, hoy, hoy)
                LibroUsuarioItemDTO(
                    id = libro.id,
                    titulo = libro.titulo,
                    autor = libro.autor,
                    genero = libro.genero.name,
                    disponible = disponible,
                    fechaAgregado = libro.fechaAgregado.format(DateTimeFormatter.ISO_DATE),
                    imagenUrl = libro.imagenUrl
                )
            }
            .sortedByDescending { it.id }
    }
}
