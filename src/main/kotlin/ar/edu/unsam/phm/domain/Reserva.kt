package ar.edu.unsam.phm

import java.time.LocalDate

class Reserva (
    val id: Int,    // confirmar
    val libro: Libro,   //titulo, autor
    val usuario: Usuario,   //persona que lo recibe
    val fechaDesde: LocalDate,
    val fechaHasta: LocalDate,
) { }