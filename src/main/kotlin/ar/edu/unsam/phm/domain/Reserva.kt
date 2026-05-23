package ar.edu.unsam.phm.domain
import jakarta.persistence.*
import java.time.LocalDateTime
import ar.edu.unsam.phm.exceptions.BusinessException

@Entity
@Table(name = "reservas")

class Reserva (
    @Column(name = "libro_id", nullable = false)
    var libroId: Int,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario,

    @Column(nullable = false)
    var fechaDesde: LocalDateTime,

    @Column(nullable = false)
    var fechaHasta: LocalDateTime,

    @Column(nullable = false)
    var bibliokarmas: Int = 0

) : BaseEntity() {
    fun sePisaCon(otraFechaDesde: LocalDateTime, otraFechaHasta: LocalDateTime): Boolean {
        return this.fechaDesde <= otraFechaHasta && this.fechaHasta >= otraFechaDesde
    }
    fun confirmar(libro: Libro) {
        if (!fechaHasta.isAfter(fechaDesde)) {
            throw BusinessException("La fecha de fin debe ser posterior a la fecha de inicio")
        }
        if (libro.duenio.id == usuario.id) {
            throw BusinessException("No podés reservar tu propio libro")
        }
        asignarKarma(libro)
    }
    private fun asignarKarma(libro: Libro) {
        val bibliokarmasASumar = libro.calcularBiblioKarmaTotal(usuario, fechaDesde, fechaHasta)
        usuario.bibliokarmas += bibliokarmasASumar
        this.bibliokarmas = bibliokarmasASumar
    }
}