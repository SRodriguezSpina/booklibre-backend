package db.migration

import com.mongodb.client.MongoClients
import com.mongodb.client.model.InsertManyOptions
import org.bson.Document
import org.bson.types.ObjectId
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class V7__seed_massive_libros_mongo : BaseJavaMigration() {

    private val cantidadLibros = 15_000
    private val idInicio = 25

    private val tiposPorIndice = arrayOf(
        "Comun", "Comun", "Comun", "Comun",
        "ConDedicatoria", "ConDedicatoria",
        "Coleccionable",
    )

    private val prefijos = arrayOf(
        "El misterio de", "La sombra de", "Entre las ruinas de", "Memorias de",
        "El secreto de", "La historia de", "Más allá de", "El viaje a",
        "La caída de", "El origen de", "Cartas desde", "El último",
    )

    private val sufijos = arrayOf(
        "la oscuridad", "los sueños", "la humanidad", "el tiempo",
        "los mares", "la luz", "las estrellas", "el silencio",
        "la memoria", "los espejos", "el destino", "la verdad",
    )

    private val autores = arrayOf(
        "Sofía Beltrán", "Mateo Acosta", "Lucía Iturralde", "Tomás Quintero",
        "Camila Aguilera", "Diego Sandoval", "Valentina Mora", "Bruno Tagliabue",
        "Isabella Cárdenas", "Javier Solís", "Renata Espinosa", "Nicolás Ferreyra",
    )

    private val generos = arrayOf(
        "DRAMA", "ROMANCE", "CIENCIA_FICCION", "LITERATURA_CLASICA",
        "AUTOAYUDA", "DISENO",
    )

    private val idiomas = arrayOf("ESPANOL", "INGLES", "FRANCES")
    private val editoriales = arrayOf("Alfaguara", "Sudamericana", "Anagrama", "Tusquets", "Minotauro", "Espasa")
    private val estados = arrayOf("EXCELENTE", "MUY_BUENO", "BUENO")

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/booklibreMongo"
        val dbName = mongoUri.substringAfterLast("/").substringBefore("?")

        val duenios = loadDuenios(context.connection)
        require(duenios.isNotEmpty()) { "No hay usuarios no-admin para asignar libros" }

        MongoClients.create(mongoUri).use { client ->
            val database = client.getDatabase(dbName)
            val collection = database.getCollection("libros")

            val docs = ArrayList<Document>(2_000)

            for (i in 0 until cantidadLibros) {
                val libroId = idInicio + i
                val tipo = tiposPorIndice[i % tiposPorIndice.size]
                val titulo = "Libro #$libroId: ${prefijos[i % prefijos.size]} ${sufijos[(i / 3) % sufijos.size]}"
                val autor = autores[(i * 7) % autores.size]
                val genero = generos[(i * 5) % generos.size]
                val idioma = idiomas[(i * 3) % idiomas.size]
                val editorial = editoriales[i % editoriales.size]
                val estado = estados[(i * 2) % estados.size]
                val cantidadPaginas = 100 + ((i * 13) % 900)
                val isbn = String.format("978%010d", libroId)
                val duenio = duenios[i % duenios.size]
                val fechaPub = LocalDate.of(1900 + ((i * 7) % 125), 1 + (i % 12), 1 + (i % 28))
                val fechaAgr = LocalDate.of(2024, 1 + (i % 12), 1 + (i % 28))

                val doc = Document()
                    .append("_id", ObjectId())
                    .append("_class", classNameParaTipo(tipo))
                    .append("id", libroId)
                    .append("duenio", duenio)
                    .append("titulo", titulo)
                    .append("autor", autor)
                    .append("descripcion", "$titulo. Una obra de $autor.")
                    .append("genero", genero)
                    .append("cantidadPaginas", cantidadPaginas)
                    .append("isbn", isbn)
                    .append("idioma", idioma)
                    .append("editorial", editorial)
                    .append("fechaPublicacion", toDate(fechaPub))
                    .append("fechaAgregado", toDate(fechaAgr))
                    .append("estado", estado)
                    .append("imagenUrl", null)
                    .append("calificacion", 0.0)
                    .append("activo", true)

                docs.add(doc)

                if (docs.size >= 1_000) {
                    collection.insertMany(docs, InsertManyOptions().ordered(false))
                    docs.clear()
                }
            }

            if (docs.isNotEmpty()) {
                collection.insertMany(docs, InsertManyOptions().ordered(false))
            }
        }
    }

    private fun classNameParaTipo(tipo: String): String = when (tipo) {
        "Comun"          -> "ar.edu.unsam.phm.domain.LibroComun"
        "ConDedicatoria" -> "ar.edu.unsam.phm.domain.LibroConDedicatoria"
        "Coleccionable" -> "ar.edu.unsam.phm.domain.LibroColeccionable"
        else             -> error("Tipo desconocido: $tipo")
    }

    private fun loadDuenios(connection: Connection): List<Document> {
        val duenios = mutableListOf<Document>()
        val sql = """
            SELECT id, email, password, nombre, descripcion, celular, ciudad, tipo_usuario,
                   bibliokarmas, imagen_url, fecha_registro, leidos, reservados
            FROM usuarios
            WHERE tipo_usuario <> 'ADMIN'
            ORDER BY id
        """.trimIndent()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    duenios.add(
                        Document()
                            .append("id", rs.getInt("id"))
                            .append("email", rs.getString("email"))
                            .append("password", rs.getString("password"))
                            .append("nombre", rs.getString("nombre"))
                            .append("desc", rs.getString("descripcion"))
                            .append("celular", rs.getString("celular"))
                            .append("ciudad", rs.getString("ciudad"))
                            .append("tipoUsuario", rs.getString("tipo_usuario"))
                            .append("bibliokarmas", rs.getInt("bibliokarmas"))
                            .append("imagenUrl", rs.getString("imagen_url"))
                            .append("fechaRegistro", toDate(rs.getDate("fecha_registro").toLocalDate()))
                            .append("leidos", rs.getInt("leidos"))
                            .append("reservados", rs.getInt("reservados"))
                    )
                }
            }
        }
        return duenios
    }

    private fun toDate(localDate: LocalDate): Date =
        Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant())
}
