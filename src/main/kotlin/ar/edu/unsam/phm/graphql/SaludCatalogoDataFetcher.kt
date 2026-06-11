package ar.edu.unsam.phm.graphql

import ar.edu.unsam.phm.dtos.SaludCatalogoDTO
import ar.edu.unsam.phm.services.SaludCatalogoService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

@DgsComponent
class SaludCatalogoDataFetcher(
    private val saludCatalogoService: SaludCatalogoService,
) {
    @DgsQuery
    fun saludCatalogo(): SaludCatalogoDTO =
        saludCatalogoService.obtenerSaludCatalogo()
}
