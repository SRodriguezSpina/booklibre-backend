package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class ReservaRepository(private val jpa: ReservaJpaRepository) {

    fun getById(id: Int): Reserva =
        jpa.findById(id).orElseThrow { NotFoundException("Reserva $id no encontrada") }

    fun save(reserva: Reserva): Reserva = jpa.save(reserva)

    fun getReservasDeLibro(libroId: Int): List<Reserva> =
        jpa.findByLibroId(libroId)

    fun obtenerPrestatarioActual(libroId: Int): Int? =
        jpa.findTopByLibroIdOrderByIdDesc(libroId).map { it.usuario.id }.orElse(null)

    fun contarReservasPorLibro(libroId: Int): Int =
        jpa.countByLibroId(libroId).toInt()

    fun findByUsuarioId(usuarioId: Int) = jpa.findByUsuarioId(usuarioId)
    fun findByLibroDuenioId(usuarioId: Int) = jpa.findByLibroDuenioId(usuarioId)

    fun findByLibroId(libroId: Int): List<Reserva> = getReservasDeLibro(libroId)

    fun obtenerIdsLibrosReservados(fechaDesdeStr: String?, fechaHastaStr: String?): Set<Int>? {
        if (fechaDesdeStr == null || fechaHastaStr == null) return null
        val desde: LocalDateTime = LocalDate.parse(fechaDesdeStr).atStartOfDay()
        val hasta: LocalDateTime = LocalDate.parse(fechaHastaStr).atTime(23, 59, 59)
        if (!desde.isBefore(hasta))
            throw BusinessException("La fecha de inicio debe ser anterior a la fecha final")
        return jpa.findLibroIdsBySuperposicion(desde, hasta).toSet()
    }

    fun findReservasAnualesByUsuarioId(usuarioId: Int) = jpa.findReservasAnualesByUsuarioId(usuarioId)
    fun findUsuariosConReservasDevueltas() = jpa.findUsuariosConReservasDevueltas()
    fun findUsuariosConMasDeNReservas(minReservas: Int) = jpa.findUsuariosConMasDeNReservas(minReservas)
}