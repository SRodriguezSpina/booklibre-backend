package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.PromedioCalificacionPorTipoDTO
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import org.springframework.stereotype.Service

@Service
class AnalisisCalificacionesService(
    private val libroMongoRepository: LibroMongoRepository,
) {
    fun obtenerAnalisisCalificaciones(): List<PromedioCalificacionPorTipoDTO> =
        libroMongoRepository.obtenerPromedioCalificacionPorTipo()
            .map { PromedioCalificacionPorTipoDTO.de(it) }
}
