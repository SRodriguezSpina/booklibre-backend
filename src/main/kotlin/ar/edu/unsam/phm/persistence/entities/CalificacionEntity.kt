package ar.edu.unsam.phm.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "calificaciones")
class CalificacionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    var libro: LibroEntity,

    @Column(name = "usuario_id", nullable = false)
    var usuarioId: Int,

    @Column(nullable = false)
    var valor: Int,

    @Column(nullable = false, length = 2000)
    var comentario: String,
)