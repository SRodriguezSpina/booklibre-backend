package ar.edu.unsam.phm.readmodels

/*
 * fn_buscar_libros (con paginación)
 *
 * Cada fila contiene el id de un libro que cumple los filtros.
 * total es el conteo total de resultados (sin LIMIT) obtenido mediante
 * función de ventana COUNT(*) OVER (), mismo patrón que CalificacionDetalleView.
 * Permite calcular el totalPages sin hacer una segunda consulta a la BDD.
 */
interface LibroPaginadoView {
    val libroId: String
    val total: Long
}
