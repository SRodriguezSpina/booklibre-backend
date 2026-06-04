package ar.edu.unsam.phm.dtos

import ar.edu.unsam.phm.readmodels.PromedioCalificacionPorTipoView

data class PromedioCalificacionPorTipoDTO(
    val tipoLibro: String,
    val promedioCalificacion: Double,
) {
    companion object {
        fun de(promedio: PromedioCalificacionPorTipoView): PromedioCalificacionPorTipoDTO =
            PromedioCalificacionPorTipoDTO(
                tipoLibro = promedio.tipoLibro,
                promedioCalificacion = promedio.promedioCalificacion,
            )
    }
}
