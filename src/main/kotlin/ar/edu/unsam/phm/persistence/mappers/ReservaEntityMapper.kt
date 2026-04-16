package ar.edu.unsam.phm.persistence.mappers

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.persistence.entities.ReservaEntity

fun ReservaEntity.toDomain(): Reserva {
    val reserva = Reserva(
        libro     = libro,
        usuario   = usuario,
        fechaDesde = fechaDesde,
        fechaHasta = fechaHasta,
        bibliokarma = bibliokarmas
    )
    reserva.id = id ?: 0
    return reserva
}

fun Reserva.toEntity(
    libro: Libro,
    usuario: Usuario,
): ReservaEntity {
    return ReservaEntity(
        id         = id,
        libro      = libro,
        usuario    = usuario,
        fechaDesde = fechaDesde,
        fechaHasta = fechaHasta,
        bibliokarmas = bibliokarma
    )
}