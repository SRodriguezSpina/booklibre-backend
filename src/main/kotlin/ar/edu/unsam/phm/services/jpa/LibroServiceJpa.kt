package ar.edu.unsam.phm.services.jpa

import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.LibroColeccionable
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroConDedicatoria
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.dtos.LibroDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.exceptions.UnauthorizedException
import ar.edu.unsam.phm.mappers.toDTO
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.mappers.toEntity
import ar.edu.unsam.phm.persistence.repositories.LibroJpaRepository
import ar.edu.unsam.phm.persistence.repositories.UsuarioJpaRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@Profile("jpa")
class LibroServiceJpa(
    private val libroJpaRepository: LibroJpaRepository,
    private val usuarioJpaRepository: UsuarioJpaRepository,
) {
    fun getLibro(id: Int): LibroDTO {
        val entity = libroJpaRepository.findById(id)
            .orElseThrow { NotFoundException("Libro $id no encontrado") }

        val domain = entity.toDomainLight()
        return domain.toDTO()
    }

    fun crearLibro(dto: LibroDTO, usuarioId: Int): LibroDTO {
        val duenio = usuarioJpaRepository.findById(usuarioId)
            .orElseThrow { NotFoundException("Usuario no encontrado") }

        val data = LibroData(
            duenio = duenio.toDomainLight(),
            titulo = dto.titulo,
            autor = dto.autor,
            descripcion = dto.descripcion,
            genero = dto.genero,
            cantidadPaginas = dto.paginas,
            isbn = dto.isbn,
            idioma = dto.idioma,
            editorial = dto.editorial,
            fechaPublicacion = dto.fechaPublicacion,
            estado = EstadoLibro.valueOf(dto.estado),
            imagenUrl = dto.imagenUrl,
            fechaAgregado = LocalDate.now(),
            disponibilidad = Disponibilidad.DISPONIBLE,
        )

        val libro = when (dto.tipo) {
            "Comun" -> LibroComun(data)
            "ConDedicatoria" -> LibroConDedicatoria(data)
            "Coleccionable" -> LibroColeccionable(data)
            else -> throw BusinessException("Tipo de libro inválido: ${dto.tipo}")
        }

        val entity = libro.toEntity(duenio)
        val saved = libroJpaRepository.save(entity)
        return saved.toDomainLight().toDTO()
    }

    fun actualizarLibro(id: Int, dto: LibroDTO, usuarioId: Int): LibroDTO {
        val existente = libroJpaRepository.findById(id)
            .orElseThrow { NotFoundException("Libro $id no encontrado") }

        if ((existente.duenio.id ?: 0) != usuarioId) {
            throw UnauthorizedException("No autorizado para editar este libro")
        }

        existente.titulo = dto.titulo
        existente.autor = dto.autor
        existente.descripcion = dto.descripcion
        existente.genero = dto.genero.name
        existente.cantidadPaginas = dto.paginas
        existente.isbn = dto.isbn
        existente.idioma = dto.idioma.name
        existente.editorial = dto.editorial
        existente.estado = EstadoLibro.valueOf(dto.estado).name
        existente.fechaPublicacion = dto.fechaPublicacion
        existente.imagenUrl = dto.imagenUrl

        val saved = libroJpaRepository.save(existente)
        return saved.toDomainLight().toDTO()
    }
}

private fun ar.edu.unsam.phm.persistence.entities.UsuarioEntity.toDomainLight(): ar.edu.unsam.phm.domain.Usuario =
    this.toDomain()

private fun ar.edu.unsam.phm.persistence.entities.LibroEntity.toDomainLight(): ar.edu.unsam.phm.domain.Libro =
    this.toDomain()

