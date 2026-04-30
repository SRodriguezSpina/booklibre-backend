package ar.edu.unsam.phm.readmodels

import java.time.LocalDateTime

/*
    vw_rangos_reserva_libro
    
    Cada fila representa un rango de reserva futuro o actual
    esActivaHoy es verdadero cuando el rango se superpone con hoy, lo que permite
    poblar LibroDTO.estoyReservado() y
    LibroDTO.alquiladoPorId() sin cargar entidades Reserva completas.
*/
interface RangoReservaLibroView {
    val libroId: Int
    val fechaDesde: LocalDateTime
    val fechaHasta: LocalDateTime
    val usuarioId: Int
    val esActivaHoy: Boolean
}
