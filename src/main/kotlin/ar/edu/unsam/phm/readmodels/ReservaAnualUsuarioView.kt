package ar.edu.unsam.phm.readmodels

import java.time.LocalDate

interface ReservaAnualUsuarioView {
    val reservaId: Int
    val usuarioId: Int
    val usuarioNombre: String
    val libroId: Int
    val libroTitulo: String
    val libroAutor: String
    val fechaDesde: LocalDate
    val fechaHasta: LocalDate
    val anioReserva: Int
}