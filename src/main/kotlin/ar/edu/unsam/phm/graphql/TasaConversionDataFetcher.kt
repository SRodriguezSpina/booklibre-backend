package ar.edu.unsam.phm.graphql

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import ar.edu.unsam.phm.services.TasaConversionService
import ar.edu.unsam.phm.dtos.TasaConversionLibroDTO

@DgsComponent
class TasaConversionDataFetcher(
    private val tasaConversionService: TasaConversionService
) {
    @DgsQuery
    fun tasaConversion(): List<TasaConversionLibroDTO> {
        return tasaConversionService.obtenerTasaConversion()
    }
}
