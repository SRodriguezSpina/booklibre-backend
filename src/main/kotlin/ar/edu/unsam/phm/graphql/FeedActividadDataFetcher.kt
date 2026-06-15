package ar.edu.unsam.phm.graphql

import ar.edu.unsam.phm.dtos.LibroAgregadoDTO
import ar.edu.unsam.phm.dtos.ReservaConfirmadaDTO
import ar.edu.unsam.phm.services.FeedActividadService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsTypeResolver

@DgsComponent
class FeedActividadDataFetcher(
    private val feedActividadService: FeedActividadService,
) {
    @DgsQuery
    fun feedActividad(): List<Any> = feedActividadService.obtenerFeed()

    @DgsTypeResolver(name = "EventoActividad")
    fun resolverTipoEvento(obj: Any): String = when (obj) {
        is LibroAgregadoDTO     -> "LibroAgregado"
        is ReservaConfirmadaDTO -> "ReservaConfirmada"
        else -> throw RuntimeException("Tipo desconocido en EventoActividad: ${obj::class.simpleName}")
    }
}
