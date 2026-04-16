package ar.edu.unsam.phm.persistence.mappers

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.persistence.entities.CalificacionEntity

fun CalificacionEntity.toDomain(): Calificacion = Calificacion(
    usuarioId = usuarioId,
    valor = valor,
    comentario = comentario,
)

fun Calificacion.toEntity(libro: Libro): CalificacionEntity = CalificacionEntity(
    libro = libro,
    usuarioId = usuarioId,
    valor = valor,
    comentario = comentario,
)