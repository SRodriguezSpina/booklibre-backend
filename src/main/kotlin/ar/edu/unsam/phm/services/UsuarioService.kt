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
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class UsuarioService(
    val usuarioRepository: UsuarioRepository,
    val libroRepository: LibroRepository
) {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    private val celularRegex = Regex("^\\+?[0-9]{7,15}$")

    fun obtenerUsuario(id: Int): UsuarioResponse {
        val usuario = usuarioRepository.findById(id) ?: throw NotFoundException("Usuario no encontrado")
        return usuario.toResponse()
    }

    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioResponse {
        val existente = usuarioRepository.findById(id) ?: throw NotFoundException("Usuario no encontrado")
        val emailNormalizado = request.email.trim().lowercase()
        val celularNormalizado = request.celular.trim()
        validarEmail(emailNormalizado)
        validarCelular(celularNormalizado)
        validarDuplicados(id, emailNormalizado, celularNormalizado)
        val tipoUsuario = parseTipoUsuario(request.tipoUsuario)
        val actualizado = Usuario(
            credencial = Credencial(emailNormalizado, existente.getCredencial().password),
            nombre = request.nombre,
            desc = request.desc,
            celular = celularNormalizado,
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
        return when (value.uppercase().replace(" ", "_")) {
            "LECTOR" -> UsuarioLector()
            "PUBLICADOR" -> UsuarioPublicador()
            "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> UsuarioLectorPublicador()
            else -> UsuarioLector()
        }
    }

    private fun validarEmail(email: String) {
        if (email.isBlank() || !emailRegex.matches(email)) {
            throw BusinessException("El email no tiene un formato valido")
        }
    }

    private fun validarCelular(celular: String) {
        if (celular.isBlank() || !celularRegex.matches(celular)) {
            throw BusinessException("El celular no tiene un formato valido")
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
                val disponible = libro.duenio.reservas.none {
                    it.libro.id == libro.id && !hoy.isBefore(it.fechaDesde) && !hoy.isAfter(it.fechaHasta)
                }
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
