package ar.edu.unsam.phm.persistence.repositories

import ar.edu.unsam.phm.persistence.entities.ReservaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ReservaJpaRepository : JpaRepository<ReservaEntity, Int> {
    fun findByLibroId(libroId: Int): List<ReservaEntity>
    @Query("""
        SELECT r FROM ReservaEntity r
        WHERE r.libro.duenio.id = :usuarioId
           OR r.usuario.id      = :usuarioId
    """)
    fun findByUsuarioIdOrLibroDuenioId(@Param("usuarioId") usuarioId: Int): List<ReservaEntity>

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
}