package ar.edu.unsam.phm
import ar.edu.unsam.phm.repository.BaseEntity
import java.time.LocalDate

class Reserva (
    val libro: Libro,   //titulo, autor
    val usuario: Usuario,   //persona que lo recibe
    val fechaDesde: LocalDate,
    val fechaHasta: LocalDate,
) : BaseEntity() {
}