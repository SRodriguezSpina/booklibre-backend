package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.readmodels.ReservaAnualUsuarioView
import ar.edu.unsam.phm.readmodels.UsuarioConMasReservasView
import ar.edu.unsam.phm.readmodels.UsuarioConReservasDevueltasView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface ReservaJpaRepository : JpaRepository<Reserva, Int> {
    fun findByLibroId(libroId: Int): List<Reserva>
    fun findByUsuarioId(usuarioId: Int): List<Reserva>
    fun countByLibroId(libroId: Int): Long
    fun existsByUsuarioIdAndFechaHastaAfter(usuarioId: Int, now: java.time.LocalDateTime): Boolean
    fun existsByLibroDuenioIdAndFechaHastaAfter(duenioId: Int, now: java.time.LocalDateTime): Boolean

    fun findTopByLibroIdOrderByIdDesc(libroId: Int): Optional<Reserva>
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.usuario = :usuarioId
    """)
    fun findByLibroDuenioId(@Param("usuarioId") usuarioId: Int): List<Reserva>
    @Query("""
        SELECT r.libro FROM Reserva r
        WHERE r.fechaDesde <= :hasta
          AND r.fechaHasta >= :desde
    """)
    fun findLibroIdsBySuperposicion(
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime,
    ): List<Int>

    @Query(
        value = """
            SELECT
                reserva_id AS reservaId,
                usuario_id AS usuarioId,
                usuario_nombre AS usuarioNombre,
                libro_id AS libroId,
                libro_titulo AS libroTitulo,
                libro_autor AS libroAutor,
                fecha_desde AS fechaDesde,
                fecha_hasta AS fechaHasta,
                anio_reserva AS anioReserva
            FROM fn_reservas_anuales_usuario(:usuarioId)
            ORDER BY fecha_desde ASC
        """,
        nativeQuery = true,
    )
    fun findReservasAnualesByUsuarioId(@Param("usuarioId") usuarioId: Int): List<ReservaAnualUsuarioView>

    @Query(
        value = """
        SELECT
            usuario_id AS usuarioId,
            usuario_nombre AS usuarioNombre,
            cantidad_reservas_devueltas AS cantidadReservasDevueltas
        FROM vw_usuarios_con_reservas_devueltas
    """,
        nativeQuery = true
    )
    fun findUsuariosConReservasDevueltas(): List<UsuarioConReservasDevueltasView>

    @Query(
        value = """
            SELECT
                usuario_id AS usuarioId,
                usuario_nombre AS usuarioNombre,
                cantidad_reservas AS cantidadReservas
            FROM fn_usuarios_con_mas_de_n_reservas(:minReservas)
        """,
        nativeQuery = true
    )
    fun findUsuariosConMasDeNReservas(@Param("minReservas") minReservas: Int): List<UsuarioConMasReservasView>

    fun getByIdOrThrow(id: Int): Reserva =
        findById(id).orElseThrow { NotFoundException("Reserva $id no encontrada") }

    fun obtenerPrestatarioActual(libroId: Int): Int? =
        findTopByLibroIdOrderByIdDesc(libroId).map { it.usuario.id }.orElse(null)

    fun contarReservasPorLibro(libroId: Int): Int =
        countByLibroId(libroId).toInt()

    fun tieneReservasActivas(usuarioId: Int, now: LocalDateTime = LocalDateTime.now()): Boolean =
        existsByUsuarioIdAndFechaHastaAfter(usuarioId, now) ||
            existsByLibroDuenioIdAndFechaHastaAfter(usuarioId, now)

    fun obtenerIdsLibrosReservados(desde: LocalDateTime, hasta: LocalDateTime): Set<Int> =
        findLibroIdsBySuperposicion(desde, hasta).toSet()

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reserva r
        WHERE r.libro.id = :libroId
          AND r.fechaDesde <= :hasta
          AND r.fechaHasta >= :desde
    """)
    fun estaReservadoEnPeriodo(
        @Param("libroId") libroId: Int,
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime,
    ): Boolean
}