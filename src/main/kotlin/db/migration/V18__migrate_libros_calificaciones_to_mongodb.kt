package db.migration

import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import org.bson.Document
import org.bson.types.ObjectId
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class V18__migrate_libros_calificaciones_to_mongodb : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/booklibre"
        val dbName = extractDbName(mongoUri)

        MongoClients.create(mongoUri).use { client ->
            val database = client.getDatabase(dbName)
            val librosCollection = database.getCollection("libros")
            val calificacionesCollection = database.getCollection("calificaciones")

            val usuarios = loadUsuarios(context.connection)
            val calificacionesPorLibro = loadCalificaciones(context.connection).groupBy { it.libroId }
            val libros = loadLibros(context.connection)

            for (libro in libros) {
                val duenio = usuarios[libro.duenioId] ?: continue
                val calificaciones = calificacionesPorLibro[libro.id].orEmpty()

                // Persist full calificaciones history in dedicated Mongo collection.
                calificaciones.forEach { calificacion ->
                    val fecha = fechaDeterministica(calificacion.id)
                    val setValues = Document()
                        .append("libroId", calificacion.libroId)
                        .append("usuarioId", calificacion.usuarioId)
                        .append("valor", calificacion.valor)
                        .append("comentario", calificacion.comentario)
                        .append("fecha", toDate(fecha))

                    calificacionesCollection.updateOne(
                        Filters.and(
                            Filters.eq("libroId", calificacion.libroId),
                            Filters.eq("usuarioId", calificacion.usuarioId),
                        ),
                        Document("\$set", setValues),
                        UpdateOptions().upsert(true),
                    )
                }

                val ultimasDos = calificaciones
                    .sortedByDescending { it.id }
                    .take(2)
                    .map {
                        Document()
                            .append("usuarioId", it.usuarioId)
                            .append("valor", it.valor)
                            .append("comentario", it.comentario)
                            .append("fecha", toDate(fechaDeterministica(it.id)))
                    }

                val libroDoc = Document()
                    .append("_class", classNameParaTipo(libro.tipo))
                    .append("id", libro.id)
                    .append("duenio", duenio)
                    .append("titulo", libro.titulo)
                    .append("autor", libro.autor)
                    .append("descripcion", libro.descripcion)
                    .append("genero", libro.genero)
                    .append("cantidadPaginas", libro.cantidadPaginas)
                    .append("isbn", libro.isbn)
                    .append("idioma", libro.idioma)
                    .append("editorial", libro.editorial)
                    .append("fechaPublicacion", toDate(libro.fechaPublicacion.atStartOfDay()))
                    .append("fechaAgregado", toDate(libro.fechaAgregado.atStartOfDay()))
                    .append("estado", libro.estado)
                    .append("imagenUrl", libro.imagenUrl)
                    .append("disponibilidad", libro.disponibilidad)
                    .append("calificaciones", ultimasDos)
                    .append("calificacion", libro.calificacion)
                    .append("activo", libro.activo)

                librosCollection.updateOne(
                    Filters.eq("id", libro.id),
                    Document(
                        "\$set",
                        libroDoc,
                    ).append(
                        "\$setOnInsert",
                        Document("_id", ObjectId()),
                    ),
                    UpdateOptions().upsert(true),
                )
            }
        }
    }

    private fun loadUsuarios(connection: Connection): Map<Int, Document> {
        val sql = """
            SELECT id, email, password, nombre, descripcion, celular, ciudad, tipo_usuario,
                   bibliokarmas, imagen_url, fecha_registro, leidos, reservados
            FROM usuarios
        """.trimIndent()

        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val result = mutableMapOf<Int, Document>()
                while (rs.next()) {
                    val id = rs.getInt("id")
                    val doc = Document()
                        .append("id", id)
                        .append("email", rs.getString("email"))
                        .append("password", rs.getString("password"))
                        .append("nombre", rs.getString("nombre"))
                        .append("desc", rs.getString("descripcion"))
                        .append("celular", rs.getString("celular"))
                        .append("ciudad", rs.getString("ciudad"))
                        .append("tipoUsuario", rs.getString("tipo_usuario"))
                        .append("bibliokarmas", rs.getInt("bibliokarmas"))
                        .append("imagenUrl", rs.getString("imagen_url"))
                        .append("fechaRegistro", toDate(rs.getDate("fecha_registro").toLocalDate().atStartOfDay()))
                        .append("leidos", rs.getInt("leidos"))
                        .append("reservados", rs.getInt("reservados"))
                    result[id] = doc
                }
                return result
            }
        }
    }

    private fun loadLibros(connection: Connection): List<LibroRow> {
        val sql = """
            SELECT id, tipo, titulo, autor, descripcion, genero, estado, disponibilidad,
                   cantidad_paginas, isbn, idioma, editorial, fecha_publicacion, fecha_agregado,
                   imagen_url, calificacion, duenio_id, activo
            FROM libros
        """.trimIndent()

        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val result = mutableListOf<LibroRow>()
                while (rs.next()) {
                    result += LibroRow(
                        id = rs.getInt("id"),
                        tipo = rs.getString("tipo"),
                        titulo = rs.getString("titulo"),
                        autor = rs.getString("autor"),
                        descripcion = rs.getString("descripcion"),
                        genero = rs.getString("genero"),
                        estado = rs.getString("estado"),
                        disponibilidad = rs.getString("disponibilidad"),
                        cantidadPaginas = rs.getInt("cantidad_paginas"),
                        isbn = rs.getString("isbn"),
                        idioma = rs.getString("idioma"),
                        editorial = rs.getString("editorial"),
                        fechaPublicacion = rs.getDate("fecha_publicacion").toLocalDate(),
                        fechaAgregado = rs.getDate("fecha_agregado").toLocalDate(),
                        imagenUrl = rs.getString("imagen_url"),
                        calificacion = rs.getDouble("calificacion"),
                        duenioId = rs.getInt("duenio_id"),
                        activo = rs.getBoolean("activo"),
                    )
                }
                return result
            }
        }
    }

    private fun loadCalificaciones(connection: Connection): List<CalificacionRow> {
        val sql = """
            SELECT id, libro_id, usuario_id, valor, comentario
            FROM calificaciones
            ORDER BY libro_id, id DESC
        """.trimIndent()

        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                val result = mutableListOf<CalificacionRow>()
                while (rs.next()) {
                    result += CalificacionRow(
                        id = rs.getInt("id"),
                        libroId = rs.getInt("libro_id"),
                        usuarioId = rs.getInt("usuario_id"),
                        valor = rs.getInt("valor"),
                        comentario = rs.getString("comentario"),
                    )
                }
                return result
            }
        }
    }

    private fun classNameParaTipo(tipo: String): String = when (tipo) {
        "Comun" -> "ar.edu.unsam.phm.domain.LibroComun"
        "ConDedicatoria" -> "ar.edu.unsam.phm.domain.LibroConDedicatoria"
        "Coleccionable" -> "ar.edu.unsam.phm.domain.LibroColeccionable"
        else -> "ar.edu.unsam.phm.domain.LibroComun"
    }

    private fun fechaDeterministica(calificacionId: Int): LocalDateTime =
        LocalDateTime.of(2000, 1, 1, 0, 0).plusSeconds(calificacionId.toLong())

    private fun toDate(value: LocalDateTime): Date =
        Date.from(value.atZone(ZoneId.systemDefault()).toInstant())

    private fun extractDbName(mongoUri: String): String {
        val withoutQuery = mongoUri.substringBefore('?')
        return withoutQuery.substringAfterLast('/').ifBlank { "booklibre" }
    }

    private data class LibroRow(
        val id: Int,
        val tipo: String,
        val titulo: String,
        val autor: String,
        val descripcion: String,
        val genero: String,
        val estado: String,
        val disponibilidad: String,
        val cantidadPaginas: Int,
        val isbn: String,
        val idioma: String,
        val editorial: String,
        val fechaPublicacion: java.time.LocalDate,
        val fechaAgregado: java.time.LocalDate,
        val imagenUrl: String?,
        val calificacion: Double,
        val duenioId: Int,
        val activo: Boolean,
    )

    private data class CalificacionRow(
        val id: Int,
        val libroId: Int,
        val usuarioId: Int,
        val valor: Int,
        val comentario: String,
    )
}
