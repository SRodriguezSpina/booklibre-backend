package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.readmodels.CalificacionDetalleView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibroJpaRepository : JpaRepository<Libro, Int> {
    fun findByDuenioId(duenioId: Int): List<Libro>

    @Query(
        value = """
            SELECT
                usuario_id           AS "usuarioId",
                nombre_usuario       AS "nombreUsuario",
                valor,
                comentario,
                total_calificaciones AS "totalCalificaciones"
            FROM fn_ultimas_calificaciones(:libroId, :limite)
        """,
        nativeQuery = true,
    )
    fun findCalificacionesDetalle(
        @Param("libroId") libroId: Int,
        @Param("limite")  limite: Int,
    ): List<CalificacionDetalleView>
}