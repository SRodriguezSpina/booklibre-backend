package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.persistence.entities.ReservaEntity
import ar.edu.unsam.phm.readmodels.ReservaAnualUsuarioView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ReservaJpaRepository : JpaRepository<ReservaEntity, Int> {
    fun findByLibroId(libroId: Int): List<ReservaEntity>
    fun findByUsuarioId(usuarioId: Int): List<ReservaEntity>

    @Query("""
        SELECT r FROM ReservaEntity r
        WHERE r.libro.duenio.id = :usuarioId
    """)
    fun findByLibroDuenioId(@Param("usuarioId") usuarioId: Int): List<ReservaEntity>

    @Query("""
        SELECT COUNT(r) > 0 FROM ReservaEntity r
        WHERE r.libro.id      = :libroId
          AND r.fechaDesde   <= :hasta
          AND r.fechaHasta   >= :desde
    """)
    fun existsSuperposicion(
        @Param("libroId") libroId: Int,
        @Param("desde")   desde: LocalDate,
        @Param("hasta")   hasta: LocalDate,
    ): Boolean

    @Query("""
        SELECT r.libro.id FROM ReservaEntity r
        WHERE r.fechaDesde <= :hasta
          AND r.fechaHasta >= :desde
    """)
    fun findLibroIdsBySuperposicion(
        @Param("desde") desde: LocalDate,
        @Param("hasta") hasta: LocalDate,
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
            FROM vw_reservas_anuales_usuario
            WHERE usuario_id = :usuarioId
            ORDER BY fecha_desde ASC
        """,
        nativeQuery = true,
    )
    fun findReservasAnualesByUsuarioId(@Param("usuarioId") usuarioId: Int): List<ReservaAnualUsuarioView>
}