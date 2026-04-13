package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import org.springframework.stereotype.Component
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.mappers.toEntity
import java.time.LocalDate

@Component
class ReservaRepository(private val jpa: ReservaJpaRepository) {

    fun getById(id: Int): Reserva =
        jpa.findById(id).orElseThrow { NotFoundException("Reserva $id no encontrada") }
            .toDomain()

    fun getReservasDeUsuario(usuarioId: Int): List<Reserva> =
        jpa.findByUsuarioIdOrLibroDuenioId(usuarioId).map { it.toDomain() }

    fun existeSuperposicion(libroId: Int, desde: LocalDate, hasta: LocalDate): Boolean =
        jpa.existsSuperposicion(libroId, desde, hasta)

    fun save(reserva: Reserva): Reserva = jpa.save(reserva.toEntity()).toDomain()
}