package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.TipoUsuario
import ar.edu.unsam.phm.Usuario
import ar.edu.unsam.phm.UsuarioLector
import ar.edu.unsam.phm.UsuarioLectorPublicador
import ar.edu.unsam.phm.UsuarioPublicador
import ar.edu.unsam.phm.dtos.LibroUsuarioItemDTO
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UsuarioService(
    val usuarioRepository: UsuarioRepository,
    val libroRepository: LibroRepository
) {

    fun obtenerUsuario(id: Int): UsuarioResponse? {
        val usuario = usuarioRepository.findById(id) ?: return null
        return usuario.toResponse()
    }

    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioResponse? {
        val existente = usuarioRepository.findById(id) ?: return null
        val tipoUsuario = parseTipoUsuario(request.tipoUsuario)
        val actualizado = Usuario(
            existente.id,
            existente.getCredencial(),
            request.nombre,
            request.desc,
            request.email,
            request.celular,
            request.ciudad,
            tipoUsuario,
            existente.bibliokarmas
        )
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
        return libroRepository.all()
            .filter { it.duenio.id == usuarioId }
            .map { libro ->
                val disponible = libro.reservas.none { !hoy.isBefore(it.fechaDesde) && !hoy.isAfter(it.fechaHasta) }
                LibroUsuarioItemDTO(
                    id = libro.id,
                    titulo = libro.titulo,
                    autor = libro.autor,
                    genero = libro.genero.name,
                    disponible = disponible,
                    fechaAgregado = null
                )
            }
            .sortedByDescending { it.id }
    }

    private fun Usuario.toResponse(): UsuarioResponse {
        val tipoStr = when (tipoUsuario) {
            is UsuarioLector -> "Lector"
            is UsuarioPublicador -> "Publicador"
            is UsuarioLectorPublicador -> "LectorPublicador"
            else -> "Lector"
        }
        return UsuarioResponse(
            id = id,
            nombre = nombre,
            desc = desc,
            email = email,
            celular = celular,
            ciudad = ciudad,
            tipoUsuario = tipoStr,
            bibliokarmas = bibliokarmas
        )
    }
}
