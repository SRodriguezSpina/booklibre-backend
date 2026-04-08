package ar.edu.unsam.phm.services.jpa

import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.TipoUsuario
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.domain.UsuarioLector
import ar.edu.unsam.phm.domain.UsuarioLectorPublicador
import ar.edu.unsam.phm.domain.UsuarioPublicador
import ar.edu.unsam.phm.dtos.UsuarioResponse
import ar.edu.unsam.phm.dtos.UsuarioUpdateRequest
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.mappers.toResponse
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.repositories.LibroJpaRepository
import ar.edu.unsam.phm.persistence.repositories.UsuarioJpaRepository
import ar.edu.unsam.phm.services.UsuarioService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("jpa")
class UsuarioServiceJpa(
    private val usuarioJpaRepository: UsuarioJpaRepository,
    private val libroJpaRepository: LibroJpaRepository,
) : UsuarioService {

    private fun getEntityOrThrow(id: Int) =
        usuarioJpaRepository.findById(id)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

    override fun obtenerUsuario(id: Int): UsuarioResponse =
        getEntityOrThrow(id).toDomain().toResponse()

    override fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioResponse {
        val existente = getEntityOrThrow(id)

        val email = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular)
        validarDuplicados(id, email, celular)

        existente.email = email
        existente.nombre = request.nombre
        existente.desc = request.desc
        existente.celular = celular
        existente.ciudad = request.ciudad
        existente.tipoUsuario = parseTipoUsuarioStr(request.tipoUsuario)
        if (request.imagenUrl != null) existente.imagenUrl = request.imagenUrl

        val guardado = usuarioJpaRepository.save(existente)
        return guardado.toDomain().toResponse()
    }

    override fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro> =
        libroJpaRepository.findByDuenioId(usuarioId).map { it.toDomain() }

    private fun parseTipoUsuarioStr(value: String): String {
        val normalized = value.trim().uppercase().replace(" ", "_")
        return when (normalized) {
            "LECTOR" -> "LECTOR"
            "PUBLICADOR" -> "PUBLICADOR"
            "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> "LECTOR_PUBLICADOR"
            else -> throw BusinessException("Tipo de usuario inválido: $value")
        }
    }

    private fun validarDuplicados(idUsuarioActual: Int, email: String, celular: String) {
        val conEmail = usuarioJpaRepository.findByEmail(email)
        if (conEmail != null && conEmail.id != idUsuarioActual) {
            throw ConflictException("El email ya esta en uso")
        }
        val conCelular = usuarioJpaRepository.findByCelular(celular)
        if (conCelular != null && conCelular.id != idUsuarioActual) {
            throw ConflictException("El celular ya esta en uso")
        }
    }
}
