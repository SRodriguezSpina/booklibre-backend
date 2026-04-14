package ar.edu.unsam.phm.persistence.mappers

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.persistence.entities.LibroEntity
import ar.edu.unsam.phm.persistence.entities.ReservaEntity

fun ReservaEntity.toDomain(): Reserva {
    val libroDomain  = libro.toDomain()
    val usuarioDomain = usuario

    val reserva = Reserva(
        libro     = libroDomain,
        usuario   = usuarioDomain,
        fechaDesde = fechaDesde,
        fechaHasta = fechaHasta,
    )
    reserva.id = id ?: 0
    return reserva
}

fun Reserva.toEntity(
    libroEntity: LibroEntity,
    usuario: Usuario,
): ReservaEntity {
    return ReservaEntity(
        id         = id,
        libro      = libroEntity,
        usuario    = usuario,
        fechaDesde = fechaDesde,
        fechaHasta = fechaHasta,
    )
}