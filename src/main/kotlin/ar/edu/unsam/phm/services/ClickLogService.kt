package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.mongo.ClickLogDocument
import ar.edu.unsam.phm.repository.mongo.ClickLogMongoRepository
import ar.edu.unsam.phm.repository.mongo.LibroMasClickeadoResult
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ClickLogService(
    private val clickLogMongo: ClickLogMongoRepository,
    private val libroMongo: LibroMongoRepository,
) {

    /**
     * Registra que un usuario entró al detalle de un libro.
     * Se llama desde LibroController.getLibro() cuando hay usuario autenticado.
     */
    fun registrarClick(libroId: Int, libroTitulo: String, nombreUsuario: String) {
        clickLogMongo.save(
            ClickLogDocument(
                fechaHora     = LocalDateTime.now(),
                nombreUsuario = nombreUsuario,
                libroId       = libroId,
                libroTitulo   = libroTitulo,
            )
        )
    }

    /**
     * Integrante 1 — Query: libro más clickeado.
     * Devuelve null si todavía no hay ningún click registrado.
     */
    fun getLibroMasClickeado(): LibroMasClickeadoResult? =
        clickLogMongo.findLibroMasClickeado()

    /**
     * Bonus: todos los clicks sobre los libros de un publicador.
     * Se usa para mostrar la sección de clicks en el perfil del publicador.
     */
    fun getClicksDeLibrosDeUsuario(usuarioId: Int): List<ClickLogDocument> {
        val libroIds = libroMongo.findByDuenioId(usuarioId).map { it.id }
        if (libroIds.isEmpty()) return emptyList()
        return clickLogMongo.findByLibroIdInOrderByFechaHoraDesc(libroIds)
    }
}
