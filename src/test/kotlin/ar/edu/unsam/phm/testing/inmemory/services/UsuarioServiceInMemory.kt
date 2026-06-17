package ar.edu.unsam.phm.testing.inmemory.services

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
import ar.edu.unsam.phm.testing.inmemory.repository.EntityNotFoundException
import ar.edu.unsam.phm.testing.inmemory.repository.LibroRepository
import ar.edu.unsam.phm.testing.inmemory.repository.ReservaRepository
import ar.edu.unsam.phm.testing.inmemory.repository.UsuarioRepository

class UsuarioServiceInMemory(
    private val usuarioRepository: UsuarioRepository,
    private val libroRepository: LibroRepository,
    private val reservaRepository: ReservaRepository,
) {

    private fun getUsuarioOrThrow(id: Int): Usuario {
        return try {
            usuarioRepository.getById(id)
        } catch (_: EntityNotFoundException) {
            throw NotFoundException("Usuario no encontrado")
        }
    }

    fun obtenerUsuario(id: Int): UsuarioResponse =
        getUsuarioOrThrow(id).toResponse()

    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioResponse {
        val existente = getUsuarioOrThrow(id)

        val email = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular ?: "")
        validarDuplicados(id, email, celular)

        // Solo un admin puede conservar el tipo ADMIN: bloquea la auto-escalación de privilegios
        if (request.tipoUsuario.trim().uppercase().replace(" ", "_") == "ADMIN" && !existente.esAdmin) {
            throw BusinessException("Tipo de usuario inválido")
        }

        val actualizado = Usuario(
            email, existente.password,
            nombre = request.nombre,
            desc = request.desc,
            celular = celular,
            ciudad = request.ciudad,
            tipoUsuario = request.tipoUsuario,
            esAdmin = existente.esAdmin,
            bibliokarmas = existente.bibliokarmas,
            imagenUrl = request.imagenUrl ?: existente.imagenUrl,
            fechaRegistro = existente.fechaRegistro,
        )
        actualizado.id = existente.id
        usuarioRepository.update(actualizado)
        return actualizado.toResponse()
    }

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro> =
        libroRepository.getLibrosDeUsuario(usuarioId)

    private fun parseTipoUsuario(value: String): TipoUsuario {
        val normalized = value.trim().uppercase().replace(" ", "_")
        return when (normalized) {
            "LECTOR" -> UsuarioLector()
            "PUBLICADOR" -> UsuarioPublicador()
            "LECTOR_PUBLICADOR", "LECTORPUBLICADOR" -> UsuarioLectorPublicador()
            else -> throw BusinessException("Tipo de usuario inválido: $value")
        }
    }

    private fun validarDuplicados(idUsuarioActual: Int, email: String, celular: String?) {
        val usuarioConEmail = usuarioRepository.findByEmail(email)
        if (usuarioConEmail != null && usuarioConEmail.id != idUsuarioActual) {
            throw ConflictException("El email ya esta en uso")
        }
        if (celular != null) {
            val usuarioConCelular = usuarioRepository.findByCelular(celular)
            if (usuarioConCelular != null && usuarioConCelular.id != idUsuarioActual) {
                throw ConflictException("El celular ya esta en uso")
            }
        }
    }
}