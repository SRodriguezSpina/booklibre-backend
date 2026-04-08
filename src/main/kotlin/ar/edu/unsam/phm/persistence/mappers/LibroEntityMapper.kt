package ar.edu.unsam.phm.persistence.mappers

import ar.edu.unsam.phm.domain.Disponibilidad
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.LibroColeccionable
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroConDedicatoria
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.persistence.entities.LibroEntity

fun LibroEntity.toDomain(): Libro {
    val duenioDomain = duenio.toDomain()

    val data = LibroData(
        duenio = duenioDomain,
        titulo = titulo,
        autor = autor,
        descripcion = descripcion,
        genero = Genero.valueOf(genero),
        cantidadPaginas = cantidadPaginas,
        isbn = isbn,
        idioma = Idioma.valueOf(idioma),
        editorial = editorial,
        fechaPublicacion = fechaPublicacion,
        estado = EstadoLibro.valueOf(estado),
        imagenUrl = imagenUrl ?: "",
        fechaAgregado = fechaAgregado,
        disponibilidad = Disponibilidad.valueOf(disponibilidad),
    )

    val normalizedTipo = tipo.trim().uppercase().replace(" ", "_")
    val libroDomain = when (normalizedTipo) {
        "COMUN" -> LibroComun(data)
        "CON_DEDICATORIA", "CONDEDICATORIA" -> LibroConDedicatoria(data)
        "COLECCIONABLE" -> LibroColeccionable(data)
        else -> throw BusinessException("Tipo de libro inválido: $tipo")
    }

    libroDomain.id = id ?: 0
    return libroDomain
}

fun Libro.toEntity(duenioEntity: ar.edu.unsam.phm.persistence.entities.UsuarioEntity): LibroEntity {
    val tipoPersistido = when (this) {
        is LibroComun -> "Comun"
        is LibroConDedicatoria -> "ConDedicatoria"
        is LibroColeccionable -> "Coleccionable"
        else -> "Comun"
    }

    return LibroEntity(
        id = id,
        tipo = tipoPersistido,
        descripcion = descripcion,
        titulo = titulo,
        autor = autor,
        genero = genero.name,
        estado = estado.name,
        disponibilidad = disponibilidad.name,
        cantidadPaginas = cantidadPaginas,
        isbn = isbn,
        idioma = idioma.name,
        editorial = editorial,
        fechaPublicacion = fechaPublicacion,
        fechaAgregado = fechaAgregado,
        imagenUrl = imagenUrl,
        duenio = duenioEntity,
    )
}

