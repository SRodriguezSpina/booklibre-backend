package ar.edu.unsam.phm.domain
import ar.edu.unsam.phm.repository.BaseEntity
import java.time.LocalDate

class Reserva (
    val libro: Libro,   //titulo, autor
    val usuario: Usuario,   //persona que lo recibe
    val fechaDesde: LocalDate,
    val fechaHasta: LocalDate,
    var bibliokarma: Int = 0    // AGREGADO
) : BaseEntity() {
    fun sePisaCon(otraFechaDesde: LocalDate, otraFechaHasta: LocalDate): Boolean {
        return this.fechaDesde <= otraFechaHasta && this.fechaHasta >= otraFechaDesde
    }
    fun confirmar() {
        asignarKarma()
    }
    private fun asignarKarma() {
        val bibliokarmasASumar = libro.calcularBiblioKarmaTotal(usuario,fechaDesde, fechaHasta)
        usuario.bibliokarmas += bibliokarmasASumar
        this.bibliokarma = bibliokarmasASumar
        // + actualizar bibliokarmas de una Reserva
    }
}