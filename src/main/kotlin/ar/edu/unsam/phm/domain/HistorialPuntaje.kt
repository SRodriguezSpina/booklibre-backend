package ar.edu.unsam.phm.persistence.entities

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "historial_calificaciones_libro")
class HistorialPuntaje(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(name = "libro_id")
    val libroId: Int,

    @Column(name = "valor_anterior")
    val valorAnterior: Int?,

    @Column(name = "valor_nuevo")
    val valorNuevo: Int,

    @Column(name = "fecha_actualizacion")
    val fechaActualizacion: LocalDateTime = LocalDateTime.now(),

    @Column(name = "usuario_id")
    val usuarioId: Int
)