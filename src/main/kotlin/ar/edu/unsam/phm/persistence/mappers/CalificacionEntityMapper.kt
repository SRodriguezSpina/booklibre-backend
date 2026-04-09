package ar.edu.unsam.phm.persistence.mappers

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.persistence.entities.CalificacionEntity
import ar.edu.unsam.phm.persistence.entities.LibroEntity

fun CalificacionEntity.toDomain(): Calificacion = Calificacion(
    usuarioId = usuarioId,
    valor = valor,
    comentario = comentario,
)

fun Calificacion.toEntity(libroEntity: LibroEntity): CalificacionEntity = CalificacionEntity(
    libro = libroEntity,
    usuarioId = usuarioId,
    valor = valor,
    comentario = comentario,
)