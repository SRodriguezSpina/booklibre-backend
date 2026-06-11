package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.dtos.SaludCatalogoDTO
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SaludCatalogoService(
    private val libroMongoRepository: LibroMongoRepository,
) {
    fun obtenerSaludCatalogo(ahora: LocalDateTime = LocalDateTime.now()): SaludCatalogoDTO =
        SaludCatalogoDTO(
            total = libroMongoRepository.contarLibrosActivos().toInt(),
            prestados = libroMongoRepository.contarPrestados(ahora).toInt(),
            disponiblesNuncaReservados = libroMongoRepository.contarNuncaReservados().toInt(),
            disponiblesReservadosAFuturo = libroMongoRepository.contarReservadosAFuturo(ahora).toInt(),
            disponiblesDevueltos = libroMongoRepository.contarDevueltos(ahora).toInt(),
        )
}
