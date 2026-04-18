package ar.edu.unsam.phm.domain
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "reservas")

class Reserva (
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    var libro: Libro,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario,

    @Column(nullable = false)
    var fechaDesde: LocalDate,

    @Column(nullable = false)
    var fechaHasta: LocalDate,

    @Column(nullable = false)
    var bibliokarmas: Int = 0

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
        this.bibliokarmas = bibliokarmasASumar
    }
}