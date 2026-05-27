package db.migration

import com.mongodb.client.MongoClients
import org.bson.Document
import org.bson.types.ObjectId
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class V3__seed_libros_mongo : BaseJavaMigration() {

    private data class LibroSeed(
        val id: Int,
        val tipo: String,
        val titulo: String,
        val autor: String,
        val descripcion: String,
        val genero: String,
        val estado: String,
        val cantidadPaginas: Int,
        val isbn: String,
        val idioma: String,
        val editorial: String,
        val fechaPublicacion: LocalDate,
        val fechaAgregado: LocalDate,
        val imagenUrl: String,
        val duenioEmail: String,
    )

    private val libros = listOf(
        LibroSeed(1,  "Comun",          "1984",                                "George Orwell",              "Una distopia sobre el totalitarismo y la vigilancia del Estado.", "CIENCIA_FICCION", "EXCELENTE", 328,  "9780451524935", "ESPANOL", "Debolsillo",             LocalDate.of(1949, 6, 8),  LocalDate.of(2025, 1, 5),  "https://contentv2.tap-commerce.com/cover/large/9789875669284_1.jpg",                                              "juan@mail.com"),
        LibroSeed(2,  "Comun",          "Clean Code",                          "Robert C. Martin",           "Guia practica para escribir codigo limpio y mantenible.",        "DISENO",          "MUY_BUENO", 464,  "9780132350884", "INGLES",  "Prentice Hall",          LocalDate.of(2008, 8, 1),  LocalDate.of(2025, 1, 6),  "https://m.media-amazon.com/images/I/51E2055ZGUL._AC_UF1000,1000_QL80_.jpg",                                       "juan@mail.com"),
        LibroSeed(3,  "Comun",          "El Principito",                       "Antoine de Saint-Exupery",   "Un clasico de la literatura universal sobre la infancia y la amistad.", "DRAMA",     "BUENO",     96,   "9780156012195", "ESPANOL", "Salamandra",             LocalDate.of(1943, 4, 6),  LocalDate.of(2025, 1, 7),  "https://tienda.planetadelibros.com.ar/cdn/shop/products/portada_el-principito_antoine-de-saint-exupery_201507152131.jpg", "juan@mail.com"),
        LibroSeed(4,  "Comun",          "Sapiens",                             "Yuval Noah Harari",          "Un recorrido por la historia de la humanidad desde los origenes.","AUTOAYUDA",       "EXCELENTE", 443,  "9780062316097", "ESPANOL", "Debate",                 LocalDate.of(2011, 1, 1),  LocalDate.of(2025, 1, 8),  "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSyAJxSA62_Z8k-pqxGp8x3Zp1bWoAtBjkZow&s",                  "juan@mail.com"),
        LibroSeed(5,  "Comun",          "Don Quijote de la Mancha",            "Miguel de Cervantes",        "La obra cumbre de la literatura espanola sobre el caballero de La Mancha.", "LITERATURA_CLASICA", "BUENO", 1023, "9788467027983", "ESPANOL", "Espasa",     LocalDate.of(1605, 1, 16), LocalDate.of(2024, 9, 10), "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQcMRZdGl9oMFn7_O4oyErwUXK9yrN3OcSe0w&s",                  "carlos@example.com"),
        LibroSeed(6,  "Comun",          "Crimen y Castigo",                    "Fiodor Dostoyevski",         "Un estudiante comete un crimen y lidia con las consecuencias psicologicas.", "DRAMA",     "MUY_BUENO", 671,  "9788420674469", "ESPANOL", "Alianza",          LocalDate.of(1866, 1, 1),  LocalDate.of(2024, 9, 11), "https://www.edicontinente.com.ar/image/titulos/9788417477639.jpg",                                              "carlos@example.com"),
        LibroSeed(7,  "Comun",          "Dune",                                "Frank Herbert",              "La epica historia de Paul Atreides en el planeta desertico Arrakis.","CIENCIA_FICCION","EXCELENTE", 688,  "9780441013593", "ESPANOL", "Minotauro",              LocalDate.of(1965, 8, 1),  LocalDate.of(2024, 9, 12), "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTrP4Ohql-5Cf2oMJNHvnj5V-ZVm3Q8rpNj0OJXuGa2dOgPMIVMchKJCRSDEJnbUsJ7mHh1C3xE3wfDDncifQkAiSixNji3-L2yN8laRAU", "carlos@example.com"),
        LibroSeed(8,  "Comun",          "El alquimista",                       "Paulo Coelho",               "Un pastor andaluz viaja en busca de su tesoro personal.",         "AUTOAYUDA",       "BUENO",     208,  "9780062315007", "ESPANOL", "Planeta",                LocalDate.of(1988, 1, 1),  LocalDate.of(2024, 9, 13), "https://m.media-amazon.com/images/I/71aFt4+OTOL.jpg",                                                            "carlos@example.com"),
        LibroSeed(9,  "ConDedicatoria", "Harry Potter y la Piedra Filosofal",  "J.K. Rowling",               "Un joven descubre que es un mago y asiste a la escuela Hogwarts.","LITERATURA_CLASICA","EXCELENTE",309,  "9788478884452", "ESPANOL", "Salamandra",             LocalDate.of(1997, 6, 26), LocalDate.of(2024, 3, 1),  "https://images.cdn3.buscalibre.com/fit-in/360x360/d7/e7/d7e777ce24f10eabdaae28d65ab70bd3.jpg",                  "ana@example.com"),
        LibroSeed(10, "ConDedicatoria", "El Codigo Da Vinci",                  "Dan Brown",                  "Un criptologo descifra una serie de enigmas escondidos en obras de Da Vinci.","DRAMA","MUY_BUENO",  489,  "9788408174189", "ESPANOL", "Planeta",                LocalDate.of(2003, 3, 18), LocalDate.of(2024, 3, 2),  "https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcSB8x01gOOsdG1D2IS0r3AquN2pGMtMnWLpFKPkF8pZZQ953Pbo1lHkAqxX1R_SWzHqmWeOC5Mcg7fbuJt_iLP76jzwWNtpbzPW3xMNlRZN", "ana@example.com"),
        LibroSeed(11, "ConDedicatoria", "Cronica de una muerte anunciada",     "Gabriel Garcia Marquez",     "El relato del asesinato de Santiago Nasar en un pueblo del Caribe.","LITERATURA_CLASICA","BUENO",  120,  "9788497592437", "ESPANOL", "Diana",                  LocalDate.of(1981, 1, 1),  LocalDate.of(2024, 3, 3),  "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSVTCjrjj-XTtQPa0m4C8zNQtyvHz1HeB4zsQ&s",                  "ana@example.com"),
        LibroSeed(12, "ConDedicatoria", "Rayuela",                             "Julio Cortazar",             "Una novela experimental que puede leerse en multiples ordenes.",  "LITERATURA_CLASICA","MUY_BUENO",600,  "9788420471822", "ESPANOL", "Alfaguara",              LocalDate.of(1963, 1, 1),  LocalDate.of(2024, 3, 4),  "https://descargarlibrosenpdf.wordpress.com/wp-content/uploads/2017/05/rayuela-cortazar.jpg",                    "ana@example.com"),
        LibroSeed(13, "ConDedicatoria", "Ficciones",                           "Jorge Luis Borges",          "Una coleccion de cuentos que mezclan lo fantastico con lo filosofico.","LITERATURA_CLASICA","EXCELENTE",224, "9788420633138", "ESPANOL", "Alianza",                LocalDate.of(1944, 1, 1),  LocalDate.of(2024, 6, 1),  "https://images.cdn1.buscalibre.com/fit-in/360x360/e4/a9/e4a975b4dce219d0b130b4d8915b8b88.jpg",                 "emilia@example.com"),
        LibroSeed(14, "ConDedicatoria", "El nombre de la rosa",                "Umberto Eco",                "Un monje investiga una serie de muertes misteriosas en una abadia medieval.","DRAMA","BUENO",      500,  "9788408081869", "ESPANOL", "Lumen",                  LocalDate.of(1980, 1, 1),  LocalDate.of(2024, 6, 2),  "https://libroschorcha.wordpress.com/wp-content/uploads/2018/02/el-nombre-de-la-rosa-umberto-eco.jpg",         "emilia@example.com"),
        LibroSeed(15, "ConDedicatoria", "Fundacion",                           "Isaac Asimov",               "Un matematico preve la caida del Imperio Galactico y planea preservar el conocimiento.","CIENCIA_FICCION","MUY_BUENO",244,"9788435018029","ESPANOL","Minotauro",          LocalDate.of(1951, 1, 1),  LocalDate.of(2024, 6, 3),  "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9t9VngDwUP_IlIKXTYUAJkRC11dlsBX7ncw&s",                "emilia@example.com"),
        LibroSeed(16, "ConDedicatoria", "Orgullo y Prejuicio",                 "Jane Austen",                "La historia de amor entre Elizabeth Bennet y el senor Darcy en la Inglaterra del siglo XIX.","ROMANCE","EXCELENTE",432,"9788491050421","ESPANOL","Austral",          LocalDate.of(1813, 1, 28), LocalDate.of(2024, 6, 4),  "https://m.media-amazon.com/images/I/71Q1tPupKjL.jpg",                                                          "emilia@example.com"),
        LibroSeed(17, "Coleccionable",  "El Senor de los Anillos",             "J.R.R. Tolkien",             "La epica lucha entre el bien y el mal en la Tierra Media.",       "CIENCIA_FICCION", "EXCELENTE", 1200, "9780000003005", "ESPANOL", "Minotauro",              LocalDate.of(1954, 7, 29), LocalDate.of(2025, 2, 20), "https://d22fxaf9t8d39k.cloudfront.net/2e23a0a073388dbaef0aa42e5c907cd5d9c5e52545af57bdfbf7f983b41eefcf114336.jpg", "ana@example.com"),
        LibroSeed(18, "Coleccionable",  "Guerra y Paz",                        "Leon Tolstoi",               "La monumental novela sobre la invasion napoleonica de Rusia.",    "LITERATURA_CLASICA","MUY_BUENO",1440, "9788420651835", "ESPANOL", "Alianza",              LocalDate.of(1869, 1, 1),  LocalDate.of(2025, 2, 21), "https://acdn-us.mitiendanube.com/stores/542/287/products/guerra_y_paz_lev_tolstoi1-4249aaf56c93a9f4e716770091781341-640-0.webp", "ana@example.com"),
        LibroSeed(19, "Coleccionable",  "Los Miserables",                      "Victor Hugo",                "La historia de Jean Valjean y su redencion en la Francia del siglo XIX.","DRAMA","BUENO",          1900, "9788420651842", "FRANCES", "Alianza",                LocalDate.of(1862, 1, 1),  LocalDate.of(2025, 2, 22), "https://http2.mlstatic.com/D_NQ_NP_612382-MLA42127442306_062020-O.webp",                                       "ana@example.com"),
        LibroSeed(20, "Coleccionable",  "En busca del tiempo perdido",         "Marcel Proust",              "Una exploracion de la memoria, el tiempo y la identidad en la alta sociedad francesa.","LITERATURA_CLASICA","EXCELENTE",4215,"9788420652009","FRANCES","Alianza",       LocalDate.of(1913, 11, 14),LocalDate.of(2025, 2, 23), "https://sbslibreria.vtexassets.com/arquivos/ids/5092712-1200-auto?v=638863180619070000&width=1200&height=auto&aspect=true", "ana@example.com"),
        LibroSeed(21, "Coleccionable",  "El Quijote Ilustrado",                "Miguel de Cervantes",        "Edicion de coleccion con ilustraciones originales del siglo XVII.","LITERATURA_CLASICA","EXCELENTE",1200,"9788467035001","ESPANOL","Espasa Edicion Especial",     LocalDate.of(1605, 1, 16), LocalDate.of(2025, 3, 1),  "https://images.cdn3.buscalibre.com/fit-in/360x360/3a/fc/3afcfcb813bc98ee2f549f21cb4a16b2.jpg",                "juan@mail.com"),
        LibroSeed(22, "Coleccionable",  "Cien anos de soledad",                "Gabriel Garcia Marquez",     "La saga de la familia Buendia en el mitico pueblo de Macondo.",   "LITERATURA_CLASICA","EXCELENTE",471, "9780307474728", "ESPANOL", "RAE Edicion Conmemorativa", LocalDate.of(1967, 5, 30), LocalDate.of(2025, 3, 2),  "https://www.rae.es/sites/default/files/styles/obra_portada_ficha/public/portada_cien_anos_de_soledad_0.jpg?itok=DDV1xNqg", "juan@mail.com"),
        LibroSeed(23, "Coleccionable",  "Ulises",                              "James Joyce",                "Un dia en la vida de Leopold Bloom en Dublin, obra clave del modernismo.","LITERATURA_CLASICA","MUY_BUENO",933,"9788420651866","INGLES","Alianza Edicion Numerada", LocalDate.of(1922, 2, 2),  LocalDate.of(2025, 3, 3),  "https://http2.mlstatic.com/D_NQ_NP_728018-MLU70496468924_072023-O.webp",                                      "juan@mail.com"),
        LibroSeed(24, "Coleccionable",  "Moby Dick",                           "Herman Melville",            "La obsesiva caceria de la ballena blanca por el capitan Ahab.",   "DRAMA",           "BUENO",     720,  "9788420651873", "INGLES",  "Alianza Edicion Especial", LocalDate.of(1851, 10, 18),LocalDate.of(2025, 3, 4),  "https://m.media-amazon.com/images/I/81OthjkJBuL.jpg",                                                          "juan@mail.com"),
    )

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://booklibre_app:booklibre_app_secret@localhost:27017,localhost:27018/booklibreMongo?authSource=booklibreMongo&wtimeoutMS=5000&socketTimeoutMS=5000&connectTimeoutMS=5000"
        val dbName = mongoUri.substringAfterLast("/").substringBefore("?")

        val usuarios = loadUsuarios(context.connection)

        MongoClients.create(mongoUri).use { client ->
            val database = client.getDatabase(dbName)
            val librosCollection = database.getCollection("libros")

            for (libro in libros) {
                val duenio = usuarios[libro.duenioEmail]
                    ?: error("Usuario ${libro.duenioEmail} no existe en Postgres - V2 debe correr antes que V3")

                val doc = Document()
                    .append("_id", ObjectId())
                    .append("_class", classNameParaTipo(libro.tipo))
                    .append("id", libro.id)
                    .append("libroId", libro.id)
                    .append("duenio", duenio)
                    .append("titulo", libro.titulo)
                    .append("autor", libro.autor)
                    .append("descripcion", libro.descripcion)
                    .append("genero", libro.genero)
                    .append("cantidadPaginas", libro.cantidadPaginas)
                    .append("isbn", libro.isbn)
                    .append("idioma", libro.idioma)
                    .append("editorial", libro.editorial)
                    .append("fechaPublicacion", toDate(libro.fechaPublicacion))
                    .append("fechaAgregado", toDate(libro.fechaAgregado))
                    .append("estado", libro.estado)
                    .append("imagenUrl", libro.imagenUrl)
                    .append("calificacion", 0.0)
                    .append("activo", true)
                    .append("calificaciones", ArrayList<Document>())
                    .append("clicks", ArrayList<Document>())
                    .append("reservas", ArrayList<Document>())

                librosCollection.insertOne(doc)
            }
        }
    }

    private fun classNameParaTipo(tipo: String): String = when (tipo) {
        "Comun"          -> "ar.edu.unsam.phm.domain.LibroComun"
        "ConDedicatoria" -> "ar.edu.unsam.phm.domain.LibroConDedicatoria"
        "Coleccionable" -> "ar.edu.unsam.phm.domain.LibroColeccionable"
        else             -> error("Tipo de libro desconocido: $tipo")
    }

    private fun loadUsuarios(connection: Connection): Map<String, Document> {
        val sql = """
            SELECT id, email, password, nombre, descripcion, celular, ciudad, tipo_usuario,
                   bibliokarmas, imagen_url, fecha_registro, leidos, reservados
            FROM usuarios
        """.trimIndent()
        val map = mutableMapOf<String, Document>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    val email = rs.getString("email")
                    val doc = Document()
                        .append("_id", rs.getInt("id"))
                        .append("nombre", rs.getString("nombre"))
                    map[email] = doc
                }
            }
        }
        return map
    }

    private fun toDate(localDate: LocalDate): Date =
        Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant())
}
