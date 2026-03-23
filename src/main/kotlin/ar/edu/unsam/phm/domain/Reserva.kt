package ar.edu.unsam.phm.domain
import ar.edu.unsam.phm.repository.BaseEntity
import java.time.LocalDate

class Reserva (
    val libro: Libro,   //titulo, autor
    val usuario: Usuario,   //persona que lo recibe
    val fechaDesde: LocalDate,
    val fechaHasta: LocalDate,
) : BaseEntity() {
    fun sePisaCon(otraFechaDesde: LocalDate, otraFechaHasta: LocalDate): Boolean {
        return this.fechaDesde <= otraFechaHasta && this.fechaHasta >= otraFechaDesde
    }
    fun confirmar() {
        asignarKarma()
        agregarReservas()
    }
    private fun asignarKarma() {
        usuario.bibliokarmas += libro.calcularBiblioKarmaTotal(usuario, fechaDesde, fechaHasta)
    }
    private fun agregarReservas() {
        usuario.agregarReserva(this)
        if (libro.duenio.id != usuario.id) {
            libro.duenio.agregarReserva(this)
        }
    }
}