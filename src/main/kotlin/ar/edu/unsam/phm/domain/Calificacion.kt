package ar.edu.unsam.phm.domain

import java.time.LocalDateTime

data class Calificacion(
    val usuarioId: Int,
    val valor: Int,
    val comentario: String,
    val fecha: LocalDateTime,
)
