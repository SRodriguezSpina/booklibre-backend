package ar.edu.unsam.phm.domain

import jakarta.persistence.*

@Entity
@Table(name = "calificaciones")
class Calificacion(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    var libro: Libro,

    @Column(name = "usuario_id", nullable = false)
    var usuarioId: Int,

    @Column(nullable = false)
    var valor: Int,

    @Column(nullable = false, length = 2000)
    var comentario: String,
) : BaseEntity()