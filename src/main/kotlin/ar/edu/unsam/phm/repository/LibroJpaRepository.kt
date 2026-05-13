package ar.edu.unsam.phm.repository

import ar.edu.unsam.phm.domain.Libro
import ar.edu.unsam.phm.readmodels.CalificacionDetalleView
import ar.edu.unsam.phm.readmodels.LibroPaginadoView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import java.util.Optional

@NoRepositoryBeaninterface LibroJpaRepository : JpaRepository<Libro, Int> {
    fun findByDuenioId(duenioId: Int): List<Libro>
    fun findAllByActivoTrue(): List<Libro>
    fun findByIdAndActivoTrue(id: Int): Optional<Libro>
    fun findByDuenioIdAndActivoTrue(usuarioId: Int): List<Libro>

    @Query(
        value = """
            SELECT
                libro_id AS "libroId",
                total
            FROM fn_buscar_libros(
                :query,
                :generos,
                :paginasMin,
                :paginasMax,
                :isbn,
                :prestadoPor,
                :excluirId,
                :excluirIds,
                :sortBy,
                :limit,
                :offset
            )
        """,
        nativeQuery = true,
    )
    fun buscarIdsPaginados(
        @Param("query")       query: String,
        @Param("generos")     generos: Array<String>?,
        @Param("paginasMin")  paginasMin: Int?,
        @Param("paginasMax")  paginasMax: Int?,
        @Param("isbn")        isbn: String?,
        @Param("prestadoPor") prestadoPor: String?,
        @Param("excluirId")   excluirId: Int?,
        @Param("excluirIds")  excluirIds: Array<Int>?,
        @Param("sortBy")      sortBy: String,
        @Param("limit")       limit: Int,
        @Param("offset")      offset: Int,
    ): List<LibroPaginadoView>

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
        @Param("limite")  limite: Int?,
    ): List<CalificacionDetalleView>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Libro l 
        SET l.activo = :activo 
        WHERE l.duenio.id = :usuarioId
    """)
    fun actualizarActivoPorDuenio(
        @Param("usuarioId") usuarioId: Int,
        @Param("activo") activo: Boolean,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            UPDATE libros SET disponibilidad = 'DISPONIBLE'
            WHERE disponibilidad = 'RESERVADO'
              AND id NOT IN (
                  SELECT DISTINCT libro_id FROM reservas WHERE fecha_hasta >= :now
              )
        """,
        nativeQuery = true,
    )
    fun liberarReservasVencidas(@Param("now") now: java.time.LocalDateTime): Int
}