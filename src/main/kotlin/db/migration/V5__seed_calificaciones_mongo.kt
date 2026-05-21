package db.migration

import com.mongodb.client.MongoClients
import org.bson.Document
import org.bson.types.ObjectId
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class V5__seed_calificaciones_mongo : BaseJavaMigration() {

    private data class CalificacionSeed(
        val libroId: Int,
        val usuarioEmail: String,
        val valor: Int,
        val comentario: String,
    )

    private val calificaciones = listOf(
        CalificacionSeed(1,  "emilia@example.com", 5, "Una obra maestra absoluta. Orwell adelantó décadas lo que vendría."),
        CalificacionSeed(1,  "ana@example.com",    4, "Impactante y perturbadora. Lectura obligatoria."),
        CalificacionSeed(1,  "carlos@example.com", 5, "De las mejores que leí. El concepto del doblepensar es genial."),
        CalificacionSeed(2,  "emilia@example.com", 4, "Muy útil para cualquier desarrollador. Cambió cómo escribo código."),
        CalificacionSeed(4,  "emilia@example.com", 5, "Te cambia la perspectiva del mundo. Increíble."),
        CalificacionSeed(4,  "carlos@example.com", 4, "Muy bien escrito, aunque algunas partes son discutibles."),
        CalificacionSeed(9,  "juan@mail.com",      5, "Un clásico eterno. La magia de Hogwarts nunca envejece."),
        CalificacionSeed(9,  "emilia@example.com", 5, "La leí de chica y la sigo amando. Pura nostalgia."),
        CalificacionSeed(9,  "carlos@example.com", 4, "Entretenidísima. Entiende por qué arrasó en todo el mundo."),
        CalificacionSeed(13, "juan@mail.com",      5, "Borges es insuperable. Cada cuento es un universo."),
        CalificacionSeed(12, "juan@mail.com",      4, "La forma de leerla en saltos es una experiencia única."),
        CalificacionSeed(12, "carlos@example.com", 3, "Interesante pero exige mucho del lector. No es para todos."),
        CalificacionSeed(17, "juan@mail.com",      5, "La fantasía épica por excelencia. Tolkien es un genio."),
        CalificacionSeed(17, "emilia@example.com", 5, "Esta edición de colección es una joya. El libro también."),
        CalificacionSeed(17, "carlos@example.com", 4, "Largo pero cada página vale la pena."),
        CalificacionSeed(22, "emilia@example.com", 5, "El realismo mágico en su máxima expresión."),
        CalificacionSeed(22, "ana@example.com",    4, "Una saga familiar fascinante. García Márquez es único."),
    )

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/booklibreMongo"
        val dbName = mongoUri.substringAfterLast("/").substringBefore("?")

        val usuarioIdPorEmail = loadUsuarioIds(context.connection)

        MongoClients.create(mongoUri).use { client ->
            val database = client.getDatabase(dbName)
            val collection = database.getCollection("calificaciones")

            for (cal in calificaciones) {
                val usuarioId = usuarioIdPorEmail[cal.usuarioEmail]
                    ?: error("Usuario ${cal.usuarioEmail} no existe - V2 debe correr antes que V5")

                val fecha = LocalDateTime.of(2025, 1, 1, 0, 0).plusDays((cal.libroId * 10L + usuarioId).toLong())

                val doc = Document()
                    .append("_id", ObjectId())
                    .append("libroId", cal.libroId)
                    .append("usuarioId", usuarioId)
                    .append("valor", cal.valor)
                    .append("comentario", cal.comentario)
                    .append("fecha", Date.from(fecha.atZone(ZoneId.of("UTC")).toInstant()))

                collection.insertOne(doc)
            }
        }
    }

    private fun loadUsuarioIds(connection: Connection): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, email FROM usuarios").use { rs ->
                while (rs.next()) {
                    map[rs.getString("email")] = rs.getInt("id")
                }
            }
        }
        return map
    }
}
