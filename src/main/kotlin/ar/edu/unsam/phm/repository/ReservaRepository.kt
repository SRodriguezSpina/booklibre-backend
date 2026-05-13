package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ReservaRepository(private val jpa: ReservaJpaRepository) {

    fun getById(id: Int): Reserva =
        jpa.getByIdOrThrow(id)

    fun save(reserva: Reserva): Reserva = jpa.save(reserva)

    fun getReservasDeLibro(libroId: Int): List<Reserva> =
        jpa.findByLibroId(libroId)

    fun obtenerPrestatarioActual(libroId: Int): Int? =
        jpa.obtenerPrestatarioActual(libroId)

    fun contarReservasPorLibro(libroId: Int): Int =
        jpa.contarReservasPorLibro(libroId)

    fun findByUsuarioId(usuarioId: Int) = jpa.findByUsuarioId(usuarioId)
    fun findByLibroDuenioId(usuarioId: Int) = jpa.findByLibroDuenioId(usuarioId)

    fun tieneReservasActivas(usuarioId: Int): Boolean {
        return jpa.tieneReservasActivas(usuarioId)
    }

    fun findByLibroId(libroId: Int): List<Reserva> = getReservasDeLibro(libroId)

    fun obtenerIdsLibrosReservados(desde: LocalDateTime, hasta: LocalDateTime): Set<Int> =
        jpa.obtenerIdsLibrosReservados(desde, hasta)

    fun findReservasAnualesByUsuarioId(usuarioId: Int) = jpa.findReservasAnualesByUsuarioId(usuarioId)
    fun findUsuariosConReservasDevueltas() = jpa.findUsuariosConReservasDevueltas()
    fun findUsuariosConMasDeNReservas(minReservas: Int) = jpa.findUsuariosConMasDeNReservas(minReservas)
}