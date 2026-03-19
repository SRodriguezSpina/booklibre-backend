package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UsuarioService(
    val usuarioRepository: UsuarioRepository,
    val libroRepository: LibroRepository
) {
    fun obtenerUsuario(id: Int): UsuarioResponse {
        val usuario = usuarioRepository.findById(id) ?: throw NotFoundException("Usuario no encontrado")
        return usuario.toResponse()
    }

    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioResponse {
        val existente = usuarioRepository.findById(id) ?: throw NotFoundException("Usuario no encontrado")
        val tipoUsuario = parseTipoUsuario(request.tipoUsuario)
        val actualizado = Usuario(
            existente.getCredencial(),
            request.nombre,
            request.desc,
            request.celular,
            request.ciudad,
            tipoUsuario,
            existente.bibliokarmas,
            existente.fechaRegistro
        )
        actualizado.id = existente.id
        usuarioRepository.update(actualizado)
        return actualizado.toResponse()
    }

    private fun parseTipoUsuario(value: String): TipoUsuario {
        return when (value.uppercase().replace(" ", "_")) {
            "LECTOR" -> UsuarioLector()
            "PUBLICADOR" -> UsuarioPublicador()
            "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> UsuarioLectorPublicador()
            else -> UsuarioLector()
        }
    }

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<LibroUsuarioItemDTO> {
        val hoy = LocalDate.now()
        return libroRepository.getLibros()
            .filter { it.duenio.id == usuarioId }
            .map { libro ->
                val disponible = libro.duenio.reservas.none {
                    it.libro.id == libro.id && !hoy.isBefore(it.fechaDesde) && !hoy.isAfter(it.fechaHasta)
                }
                LibroUsuarioItemDTO(
                    id = libro.id,
                    titulo = libro.titulo,
                    autor = libro.autor,
                    genero = libro.genero.name,
                    disponible = disponible,
                    fechaAgregado = null,
                    imagenUrl = libro.imagenUrl
                )
            }
            .sortedByDescending { it.id }
    }
}
