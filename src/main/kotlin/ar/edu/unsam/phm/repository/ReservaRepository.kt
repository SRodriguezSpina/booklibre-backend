package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.persistence.mappers.toDomain
import ar.edu.unsam.phm.persistence.mappers.toEntity
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ReservaRepository(private val jpa: ReservaJpaRepository) {

    fun getById(id: Int): Reserva =
        jpa.findById(id).orElseThrow { NotFoundException("Reserva $id no encontrada") }

    fun existeSuperposicion(libroId: Int, desde: LocalDate, hasta: LocalDate): Boolean =
        jpa.existsSuperposicion(libroId, desde, hasta)

    fun save(reserva: Reserva): Reserva = jpa.save(reserva)

    fun getReservasDeLibro(libroId: Int): List<Reserva> =
        jpa.findByLibroId(libroId)

    fun findByLibroId(libroId: Int): List<Reserva> = getReservasDeLibro(libroId)

    fun existeReservaActiva(libroId: Int, hoy: LocalDate): Boolean =
        jpa.existsSuperposicion(libroId, hoy, hoy)

    fun obtenerIdsLibrosReservados(fechaDesdeStr: String?, fechaHastaStr: String?): Set<Int>? {
        if (fechaDesdeStr == null || fechaHastaStr == null) return null
        val desde = LocalDate.parse(fechaDesdeStr)
        val hasta = LocalDate.parse(fechaHastaStr)
        if (!desde.isBefore(hasta))
            throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
        return jpa.findLibroIdsBySuperposicion(desde, hasta).toSet()
    }
}