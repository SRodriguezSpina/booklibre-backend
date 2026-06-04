package ar.edu.unsam.phm.graphql

import ar.edu.unsam.phm.dtos.PromedioCalificacionPorTipoDTO
import ar.edu.unsam.phm.services.AnalisisCalificacionesService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

@DgsComponent
class AnalisisCalificacionesDataFetcher(
    private val analisisCalificacionesService: AnalisisCalificacionesService,
) {
    @DgsQuery
    fun analisisCalificaciones(): List<PromedioCalificacionPorTipoDTO> =
        analisisCalificacionesService.obtenerAnalisisCalificaciones()
}
