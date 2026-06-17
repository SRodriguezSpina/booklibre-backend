package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.TipoUsuario
import ar.edu.unsam.phm.domain.TopBibliokarmasCache
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.*
import ar.edu.unsam.phm.mappers.toLibroUsuarioItemDTO
import ar.edu.unsam.phm.mappers.toBibliokarmaDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.redis.TopBibliokarmasCacheRepository
import ar.edu.unsam.phm.security.JwtTokenUtils
import ar.edu.unsam.phm.security.UserDetailsServiceImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UsuarioActualizadoConTokens(
    val usuario: Usuario,
    val tokens: AuthTokens,
)

@Service
class UsuarioService(
    private val usuarioJpa: UsuarioJpaRepository,
    private val libroRepository: LibroMongoRepository,
    private val reservaJpa: ReservaJpaRepository,
    private val jwtTokenUtils: JwtTokenUtils,
    private val topBibliokarmasCacheRepository: TopBibliokarmasCacheRepository,
) {
    fun obtenerUsuario(id: Int): Usuario =
        usuarioJpa.findById(id).orElseThrow { NotFoundException("Usuario $id no encontrado") }

    @Transactional
    fun actualizarUsuario(id: Int, request: UsuarioUpdateRequest): UsuarioActualizadoConTokens {
        val usuario = usuarioJpa.findById(id).orElseThrow { NotFoundException("Usuario $id no encontrado") }
        val email   = Usuario.normalizarYValidarEmail(request.email)
        val celular = Usuario.normalizarYValidarCelular(request.celular ?: "")

        validarEmailDisponible(email, id)
        validarCelularDisponible(celular, id)

        val tipoAnterior  = usuario.tipoUsuarioDomain()
        val nombreAnterior = usuario.nombre

        if (request.tipoUsuario != usuario.tipoUsuario && tieneReservasActivas(id)) {
            throw BusinessException("No podés cambiar el tipo de usuario mientras tenés reservas activas")
        }

        // Solo un admin puede conservar el tipo ADMIN: bloquea la auto-escalación de privilegios
        if (request.tipoUsuario.trim().uppercase().replace(" ", "_") == "ADMIN" && !usuario.esAdmin) {
            throw BusinessException("Tipo de usuario inválido")
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
        if (actualizado.nombre != nombreAnterior) {
            libroRepository.actualizarNombreDuenio(actualizado.id, actualizado.nombre)
        }

        val roles = UserDetailsServiceImpl.rolesParaUsuario(actualizado).map { it.authority }
        val accessToken = jwtTokenUtils.generateToken(actualizado.email, roles)
        val refreshToken = jwtTokenUtils.generateRefreshToken(actualizado.email)

        return UsuarioActualizadoConTokens(
            usuario = actualizado,
            tokens = AuthTokens(accessToken, refreshToken),
        )
    }

    fun obtenerLibrosDeUsuario(usuarioId: Int): List<Libro> =
        libroRepository.getLibrosDeUsuario(usuarioId)

    fun obtenerLibrosDeUsuarioPaginado(
        usuarioId: Int,
        filtro: String,
        ordenarPor: String,
        direccion: String,
        page: Int,
        size: Int,
    ): PagedResponse<LibroUsuarioItemDTO> {
        val ahora = java.time.LocalDateTime.now()
        val paged = libroRepository.findPaginadoByDuenioId(
            usuarioId, filtro, ordenarPor, direccion, page, size
        )
        val items = paged.content.map { libro ->
            libro.toLibroUsuarioItemDTO().copy(disponible = libro.estaDisponible(ahora))
        }
        return PagedResponse(items, paged.page, paged.totalPages, paged.totalElements)
    }

    // me traigo de redis la info
    fun obtenerTop5Bibliokarmas(): List<UsuarioBibliokarmaDTO> {
        return topBibliokarmasCacheRepository
            .findById(TopBibliokarmasCache.CACHE_KEY)
            .orElseGet { actualizarCacheTopBibliokarmas() }
            .usuarios
    }
    // si no está la busco en postgre
    fun actualizarCacheTopBibliokarmas(): TopBibliokarmasCache {
        val topUsuarios = obtenerTop5Postgres()     // acá
        val cache = TopBibliokarmasCache(
            usuarios = topUsuarios
        )
        return topBibliokarmasCacheRepository.save(cache)
    }
    // acá se hace la query de jpaUsuario, mapeo y listo el pollo
    private fun obtenerTop5Postgres(): List<UsuarioBibliokarmaDTO> =
        usuarioJpa
            .findTop5ByOrderByBibliokarmasDesc()    // acá
            .map { it.toBibliokarmaDTO() }

    private fun tieneReservasActivas(usuarioId: Int): Boolean {
        val ahora = java.time.LocalDateTime.now()
        if (reservaJpa.existsByUsuarioIdAndFechaHastaAfter(usuarioId, ahora)) return true
        val librosDelUsuario = libroRepository.findLibroIdsByDuenioId(usuarioId)
        return librosDelUsuario.isNotEmpty() &&
            reservaJpa.existsByLibroIdInAndFechaHastaAfter(librosDelUsuario, ahora)
    }

    // Validaciones de unicidad — incumben al service porque tocan el repositorio
    private fun validarEmailDisponible(email: String, idActual: Int) {
        val existente = usuarioJpa.findByEmail(email)
        if (existente != null && existente.id != idActual)
            throw ConflictException("El email ya está en uso")
    }

    private fun validarCelularDisponible(celular: String?, idActual: Int) {
        if (celular == null) return  // celular opcional, NULL no viola unicidad
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
