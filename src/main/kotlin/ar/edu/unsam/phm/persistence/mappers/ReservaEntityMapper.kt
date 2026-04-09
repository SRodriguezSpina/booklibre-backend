package ar.edu.unsam.phm.persistence.mappers

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.persistence.entities.LibroEntity
import ar.edu.unsam.phm.persistence.entities.ReservaEntity
import ar.edu.unsam.phm.persistence.entities.UsuarioEntity

fun ReservaEntity.toDomain(): Reserva {
    val libroDomain  = libro.toDomain()
    val usuarioDomain = usuario.toDomain()

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
    usuarioEntity: UsuarioEntity,
): ReservaEntity {
    return ReservaEntity(
        id         = id,
        libro      = libroEntity,
        usuario    = usuarioEntity,
        fechaDesde = fechaDesde,
        fechaHasta = fechaHasta,
    )
}