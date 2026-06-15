package ar.edu.unsam.phm.dtos

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

data class LibroAgregadoDTO(
    val libroId: Int,
    val titulo: String,
    val autor: String,
    val duenioNombre: String,
    val fecha: String,
    val tipoEvento: String = "LIBRO_AGREGADO",
) {
    fun fechaParaOrdenar(): LocalDateTime = LocalDateTime.parse(fecha, FORMATTER)
}

data class ReservaConfirmadaDTO(
    val reservaId: Int,
    val libroId: Int,
    val libroTitulo: String,
    val usuarioId: Int,
    val usuarioNombre: String,
    val fecha: String,
    val tipoEvento: String = "RESERVA_CONFIRMADA",
) {
    fun fechaParaOrdenar(): LocalDateTime = LocalDateTime.parse(fecha, FORMATTER)
}
