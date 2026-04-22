package ar.edu.unsam.phm.readmodels

/*
 * fn_ultimas_calificaciones
 *
 * Cada fila es una reseña, ya unida con el nombre del autor desde
 * Usuario. totalCalificaciones es un conteo
 * por función de ventana de *todas* las reseñas del mismo libro, por lo que
 * una sola consulta provee tanto la lista a mostrar como el total general,
 * eliminando el patrón N+1 que antes emitía un SELECT por cada reseñador.
 */
interface CalificacionDetalleView {
    val usuarioId: Int
    val nombreUsuario: String
    val valor: Int
    val comentario: String
    val totalCalificaciones: Long
}
