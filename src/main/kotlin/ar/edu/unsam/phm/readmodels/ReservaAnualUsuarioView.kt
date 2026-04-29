package ar.edu.unsam.phm.readmodels

import java.time.LocalDateTime

interface ReservaAnualUsuarioView {
    val reservaId: Int
    val usuarioId: Int
    val usuarioNombre: String
    val libroId: Int
    val libroTitulo: String
    val libroAutor: String
    val fechaDesde: LocalDateTime
    val fechaHasta: LocalDateTime
    val anioReserva: Int
}