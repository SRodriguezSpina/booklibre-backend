package ar.edu.unsam.phm.domain

import jakarta.persistence.*
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference

@Document(collection = "calificaciones")
data class Calificacion(
    val id : String? = null,
    @DocumentReference(lazy = true)
    val libroId: Int,
    val usuarioId: Int,
    val valor: Int,
    val comentario: String
)