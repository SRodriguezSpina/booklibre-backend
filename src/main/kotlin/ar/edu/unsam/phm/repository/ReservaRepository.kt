package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.ConflictException
import ar.edu.unsam.phm.exceptions.NotFoundException
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ReservaRepository(private val jpa: ReservaJpaRepository) {

    fun getById(id: Int): Reserva =
        jpa.findById(id).orElseThrow { NotFoundException("Reserva $id no encontrada") }

    fun existeSuperposicion(libroId: Int, desde: LocalDate, hasta: LocalDate) {
        if (jpa.existeSuperposicion(libroId, desde, hasta)) {
            throw ConflictException("El libro ya está reservado para esas fechas.")
        }
    }

    fun save(reserva: Reserva): Reserva = jpa.save(reserva)

    fun getReservasDeLibro(libroId: Int): List<Reserva> =
        jpa.findByLibroId(libroId)

    fun findByUsuarioId(usuarioId: Int) = jpa.findByUsuarioId(usuarioId)
    fun findByLibroDuenioId(usuarioId: Int) = jpa.findByLibroDuenioId(usuarioId)

    fun findByLibroId(libroId: Int): List<Reserva> = getReservasDeLibro(libroId)

    fun existeReservaActiva(libroId: Int, hoy: LocalDate) {
        if (jpa.existeSuperposicion(libroId, hoy, hoy)) {
            throw ConflictException("El libro tiene una reserva activa y no puede eliminarse")
        }
    }

    fun obtenerIdsLibrosReservados(fechaDesdeStr: String?, fechaHastaStr: String?): Set<Int>? {
        if (fechaDesdeStr == null || fechaHastaStr == null) return null
        val desde = LocalDate.parse(fechaDesdeStr)
        val hasta = LocalDate.parse(fechaHastaStr)
        if (!desde.isBefore(hasta))
            throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
        return jpa.findLibroIdsBySuperposicion(desde, hasta).toSet()
    }

    fun findReservasAnualesByUsuarioId(usuarioId: Int) = jpa.findReservasAnualesByUsuarioId(usuarioId)
}