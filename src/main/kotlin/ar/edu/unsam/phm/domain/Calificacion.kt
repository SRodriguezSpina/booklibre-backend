package ar.edu.unsam.phm.domain

import java.time.LocalDateTime

data class Calificacion(
    var usuarioId: Int,
    var valor: Int,
    var comentario: String,
    var fecha: LocalDateTime = LocalDateTime.now(),
)