package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.readmodels.ReservaAnualUsuarioView
import ar.edu.unsam.phm.readmodels.UsuarioConMasReservasView
import ar.edu.unsam.phm.readmodels.UsuarioConReservasDevueltasView
import ar.edu.unsam.phm.readmodels.LibroReservasCount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional


interface ReservaJpaRepository : JpaRepository<Reserva, Int> {
    fun findByLibroId(libroId: Int): List<Reserva>
    fun findByLibroIdIn(libroIds: Collection<Int>): List<Reserva>
    fun findByLibroIdIn(libroIds: Collection<Int>, pageable: Pageable): Page<Reserva>
    fun findByUsuarioId(usuarioId: Int): List<Reserva>
    fun findByUsuarioId(usuarioId: Int, pageable: Pageable): Page<Reserva>
    fun countByLibroId(libroId: Int): Long
    fun existsByUsuarioIdAndFechaHastaAfter(usuarioId: Int, now: java.time.LocalDateTime): Boolean
    fun existsByLibroIdInAndFechaHastaAfter(libroIds: Collection<Int>, now: java.time.LocalDateTime): Boolean
    fun existsByLibroIdAndUsuarioIdAndFechaHastaAfter(libroId: Int, usuarioId: Int, now: java.time.LocalDateTime): Boolean
    fun findByLibroIdAndFechaHastaAfter(libroId: Int, now: java.time.LocalDateTime): List<Reserva>
    fun findFirstByLibroIdAndUsuarioIdAndFechaHastaAfter(libroId: Int, usuarioId: Int, now: java.time.LocalDateTime): Optional<Reserva>
    fun findTopByLibroIdOrderByIdDesc(libroId: Int): Optional<Reserva>

    @Query("""
        SELECT r.libroId as libroId, COUNT(r) as count
        FROM Reserva r
        WHERE r.libroId IN :libroIds
        GROUP BY r.libroId
    """)
    fun countReservasByLibroIds(@Param("libroIds") libroIds: Collection<Int>): List<LibroReservasCount>

    @Query("""
        SELECT r.libroId FROM Reserva r
        WHERE r.fechaDesde <= :hasta
          AND r.fechaHasta >= :desde
    """)
    fun findLibroIdsBySuperposicion(
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime,
    ): List<Int>

    @Query("""
        SELECT r.libroId 
        FROM Reserva r
        GROUP BY r.libroId
        HAVING MAX(r.fechaHasta) < :now
    """)
    fun findLibrosConTodasReservasCumplidas(@Param("now") now: LocalDateTime): List<Int>


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

    fun obtenerIdsLibrosReservados(desde: LocalDateTime, hasta: LocalDateTime): Set<Int> =
        findLibroIdsBySuperposicion(desde, hasta).toSet()

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reserva r
        WHERE r.libroId = :libroId
          AND r.fechaDesde <= :hasta
          AND r.fechaHasta >= :desde
    """)
    fun estaReservadoEnPeriodo(
        @Param("libroId") libroId: Int,
        @Param("desde") desde: LocalDateTime,
        @Param("hasta") hasta: LocalDateTime,
    ): Boolean

    @Query("""
        SELECT r.libroId
        FROM Reserva r
        WHERE r.fechaHasta >= :now
        GROUP BY r.libroId
        HAVING COUNT(r) >= :minReservas
    """)
    fun findLibroIdsConAlMenosNReservasActivas(
        @Param("now") now: LocalDateTime,
        @Param("minReservas") minReservas: Long,
    ): List<Int>

    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.usuario u
        ORDER BY r.fechaDesde DESC
    """)
    fun findTopNRecentesConUsuario(pageable: Pageable): List<Reserva>
}