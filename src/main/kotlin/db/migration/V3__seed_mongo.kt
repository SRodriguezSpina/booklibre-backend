package db.migration

import com.mongodb.ConnectionString
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.InsertManyOptions
import org.bson.Document
import org.bson.types.ObjectId
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.math.roundToInt

/**
 * Seed único de la base documental (Mongo / Atlas). Baseline para base virgen.
 * Reúne, en orden:
 *   1. libros curados (24) + libros masivos (15.000)
 *   2. calificaciones de los libros curados (recalcula el promedio)
 *   3. sincronización de reservas (leídas desde Postgres) en cada libro
 *   4. índices, incluido el índice de texto en español
 *
 * Lee usuarios y reservas desde Postgres vía context.connection, por lo que las
 * migraciones V1 (schema) y V2 (seed Postgres) deben correr antes.
 *
 * Protección de datos de usuario: un marcador en la colección `_seed_meta` (en Atlas)
 * registra que el seed inicial ya se completó. Si Postgres se recrea (su
 * flyway_schema_history se pierde) y Flyway re-ejecuta esta migración, el marcador hace
 * que se saltee sin tocar `libros` → nunca borra clicks/reservas/libros creados por la app.
 * El `drop()` sólo ocurre cuando el marcador NO existe (seed parcial previo, sin datos vivos).
 */
class V3__seed_mongo : BaseJavaMigration() {

    private companion object {
        const val SEED_MARKER_ID = "mongo-seed-v3"
    }

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

    private data class CalificacionSeed(
        val libroId: Int,
        val usuarioEmail: String,
        val valor: Int,
        val comentario: String,
    )

    // ── libros curados (ids 1-24) ─────────────────────────────────────────────
    private val librosCurados = listOf(
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

    // ── calificaciones de los libros curados ──────────────────────────────────
    private val calificaciones = listOf(
        CalificacionSeed(1,  "emilia@example.com", 5, "Una obra maestra absoluta. Orwell adelanto decadas lo que vendria."),
        CalificacionSeed(1,  "ana@example.com",    4, "Impactante y perturbadora. Lectura obligatoria."),
        CalificacionSeed(1,  "carlos@example.com", 5, "De las mejores que lei. El concepto del doblepensar es genial."),
        CalificacionSeed(2,  "emilia@example.com", 4, "Muy util para cualquier desarrollador. Cambio como escribo codigo."),
        CalificacionSeed(4,  "emilia@example.com", 5, "Te cambia la perspectiva del mundo. Increible."),
        CalificacionSeed(4,  "carlos@example.com", 4, "Muy bien escrito, aunque algunas partes son discutibles."),
        CalificacionSeed(9,  "juan@mail.com",      5, "Un clasico eterno. La magia de Hogwarts nunca envejece."),
        CalificacionSeed(9,  "emilia@example.com", 5, "La lei de chica y la sigo amando. Pura nostalgia."),
        CalificacionSeed(9,  "carlos@example.com", 4, "Entretenidisima. Entiende por que arraso en todo el mundo."),
        CalificacionSeed(13, "juan@mail.com",      5, "Borges es insuperable. Cada cuento es un universo."),
        CalificacionSeed(12, "juan@mail.com",      4, "La forma de leerla en saltos es una experiencia unica."),
        CalificacionSeed(12, "carlos@example.com", 3, "Interesante pero exige mucho del lector. No es para todos."),
        CalificacionSeed(17, "juan@mail.com",      5, "La fantasia epica por excelencia. Tolkien es un genio."),
        CalificacionSeed(17, "emilia@example.com", 5, "Esta edicion de coleccion es una joya. El libro tambien."),
        CalificacionSeed(17, "carlos@example.com", 4, "Largo pero cada pagina vale la pena."),
        CalificacionSeed(22, "emilia@example.com", 5, "El realismo magico en su maxima expresion."),
        CalificacionSeed(22, "ana@example.com",    4, "Una saga familiar fascinante. Garcia Marquez es unico."),
    )

    // ── parámetros del dataset masivo (ids 25 en adelante) ────────────────────
    private val cantidadLibrosMasivos = 15_000
    private val idInicioMasivos = 25

    private val tiposPorIndice = arrayOf(
        "Comun", "Comun", "Comun", "Comun",
        "ConDedicatoria", "ConDedicatoria",
        "Coleccionable",
    )
    private val prefijos = arrayOf(
        "El misterio de", "La sombra de", "Entre las ruinas de", "Memorias de",
        "El secreto de", "La historia de", "Mas alla de", "El viaje a",
        "La caida de", "El origen de", "Cartas desde", "El ultimo",
    )
    private val sufijos = arrayOf(
        "la oscuridad", "los suenos", "la humanidad", "el tiempo",
        "los mares", "la luz", "las estrellas", "el silencio",
        "la memoria", "los espejos", "el destino", "la verdad",
    )
    private val autores = arrayOf(
        "Sofia Beltran", "Mateo Acosta", "Lucia Iturralde", "Tomas Quintero",
        "Camila Aguilera", "Diego Sandoval", "Valentina Mora", "Bruno Tagliabue",
        "Isabella Cardenas", "Javier Solis", "Renata Espinosa", "Nicolas Ferreyra",
    )
    private val generos = arrayOf(
        "DRAMA", "ROMANCE", "CIENCIA_FICCION", "LITERATURA_CLASICA",
        "AUTOAYUDA", "DISENO",
    )
    private val idiomas = arrayOf("ESPANOL", "INGLES", "FRANCES")
    private val editoriales = arrayOf("Alfaguara", "Sudamericana", "Anagrama", "Tusquets", "Minotauro", "Espasa")
    private val estados = arrayOf("EXCELENTE", "MUY_BUENO", "BUENO")

    override fun migrate(context: Context) {
        val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://booklibre_app:booklibre_app_secret@localhost:27017,localhost:27018/booklibreMongo?authSource=booklibreMongo&wtimeoutMS=5000&socketTimeoutMS=5000&connectTimeoutMS=5000"
        val dbName = ConnectionString(mongoUri).database ?: "booklibreMongo"

        MongoClients.create(mongoUri).use { client ->
            val db = client.getDatabase(dbName)
            val meta = db.getCollection("_seed_meta")

            // ¿Atlas ya fue sembrado alguna vez? El marcador vive en Atlas (no en el
            // flyway_schema_history de Postgres), así que recrear Postgres NO vuelve a
            // sembrar ni borra datos de usuarios. Si el marcador existe: no tocar nada.
            if (meta.countDocuments(Document("_id", SEED_MARKER_ID)) > 0L) return

            // Marcador ausente ⇒ el seed inicial nunca terminó ⇒ lo que haya en `libros`
            // es un seed parcial (no datos de usuarios). Recién acá es seguro dropear.
            val libros = db.getCollection("libros")
            libros.drop()

            // Datos de Postgres (V1 + V2 ya corrieron).
            val usuariosPorEmail = loadUsuarios(context.connection)
            val usuarioIdPorEmail = loadUsuarioIds(context.connection)
            val duenios = loadDuenios(context.connection)
            require(duenios.isNotEmpty()) { "No hay usuarios no-admin para asignar libros" }
            val reservasPorLibro = loadReservasPorLibro(context.connection)

            seedLibrosCurados(libros, usuariosPorEmail)
            seedLibrosMasivos(libros, duenios)
            seedCalificaciones(libros, usuarioIdPorEmail)
            syncReservas(libros, reservasPorLibro)
            crearIndices(libros)

            // Marca el seed como completo. A partir de acá ningún re-run vuelve a dropear.
            meta.insertOne(Document("_id", SEED_MARKER_ID).append("seededAt", Date()))
        }
    }

    // ── 1a. libros curados ─────────────────────────────────────────────────────
    private fun seedLibrosCurados(libros: MongoCollection<Document>, usuariosPorEmail: Map<String, Document>) {
        for (libro in librosCurados) {
            val duenio = usuariosPorEmail[libro.duenioEmail]
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

            libros.insertOne(doc)
        }
    }

    // ── 1b. libros masivos ─────────────────────────────────────────────────────
    private fun seedLibrosMasivos(libros: MongoCollection<Document>, duenios: List<Document>) {
        val docs = ArrayList<Document>(2_000)

        for (i in 0 until cantidadLibrosMasivos) {
            val libroId = idInicioMasivos + i
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
                .append("libroId", libroId)
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
                .append("calificaciones", ArrayList<Document>())
                .append("clicks", ArrayList<Document>())
                .append("reservas", ArrayList<Document>())

            docs.add(doc)

            if (docs.size >= 1_000) {
                libros.insertMany(docs, InsertManyOptions().ordered(false))
                docs.clear()
            }
        }

        if (docs.isNotEmpty()) {
            libros.insertMany(docs, InsertManyOptions().ordered(false))
        }
    }

    // ── 2. calificaciones ──────────────────────────────────────────────────────
    private fun seedCalificaciones(libros: MongoCollection<Document>, usuarioIdPorEmail: Map<String, Int>) {
        val califsByLibro = calificaciones.groupBy { it.libroId }

        for ((libroId, califs) in califsByLibro) {
            val califDocs = califs.map { cal ->
                val usuarioId = usuarioIdPorEmail[cal.usuarioEmail]
                    ?: error("Usuario ${cal.usuarioEmail} no existe - V2 debe correr antes que V3")

                val fecha = LocalDateTime.of(2025, 1, 1, 0, 0).plusDays(cal.libroId * 10L + usuarioId)

                Document()
                    .append("usuarioId", usuarioId)
                    .append("valor", cal.valor)
                    .append("comentario", cal.comentario)
                    .append("fecha", Date.from(fecha.atZone(ZoneId.of("UTC")).toInstant()))
            }

            val avg = califs.map { it.valor }.average()
            val roundedAvg = (avg * 10.0).roundToInt() / 10.0

            libros.updateOne(
                Document("libroId", libroId),
                Document("\$set", Document("calificaciones", califDocs).append("calificacion", roundedAvg)),
            )
        }
    }

    // ── 3. sincronización de reservas (desde Postgres) ─────────────────────────
    private fun syncReservas(libros: MongoCollection<Document>, reservasPorLibro: Map<Int, List<Pair<LocalDateTime, LocalDateTime>>>) {
        for ((libroId, rangos) in reservasPorLibro) {
            val reservaDocs = rangos.map { (desde, hasta) ->
                Document()
                    .append("fechaDesde", toDateTime(desde))
                    .append("fechaHasta", toDateTime(hasta))
            }

            libros.updateOne(
                Document("libroId", libroId),
                Document("\$set", Document("reservas", reservaDocs)),
            )
        }
    }

    // ── 4. índices ─────────────────────────────────────────────────────────────
    private fun crearIndices(libros: MongoCollection<Document>) {
        val opts = IndexOptions().background(true)
        libros.createIndex(Indexes.ascending("id"), opts)
        libros.createIndex(Indexes.ascending("libroId"), opts)
        libros.createIndex(Indexes.ascending("activo"), opts)
        libros.createIndex(Indexes.ascending("duenio.id"), opts)
        libros.createIndex(Indexes.ascending("duenio.id", "activo"), opts)
        libros.createIndex(Indexes.ascending("genero"), opts)
        libros.createIndex(Indexes.ascending("isbn"), opts)
        libros.createIndex(Indexes.ascending("titulo"), opts)
        libros.createIndex(Indexes.ascending("autor"), opts)
        libros.createIndex(Indexes.descending("calificacion"), opts)
        libros.createIndex(Indexes.ascending("activo", "titulo"), opts)

        // índice de texto en español para la búsqueda principal
        val textOpts = IndexOptions().name("libros_text_index").defaultLanguage("spanish")
        val textKeys = Document()
            .append("titulo", "text")
            .append("autor", "text")
            .append("descripcion", "text")
        libros.createIndex(textKeys, textOpts)
    }

    // ── helpers ────────────────────────────────────────────────────────────────
    private fun classNameParaTipo(tipo: String): String = when (tipo) {
        "Comun"          -> "ar.edu.unsam.phm.domain.LibroComun"
        "ConDedicatoria" -> "ar.edu.unsam.phm.domain.LibroConDedicatoria"
        "Coleccionable" -> "ar.edu.unsam.phm.domain.LibroColeccionable"
        else             -> error("Tipo de libro desconocido: $tipo")
    }

    private fun loadUsuarios(connection: Connection): Map<String, Document> {
        val map = mutableMapOf<String, Document>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, email, nombre FROM usuarios").use { rs ->
                while (rs.next()) {
                    map[rs.getString("email")] = Document()
                        .append("_id", rs.getInt("id"))
                        .append("nombre", rs.getString("nombre"))
                }
            }
        }
        return map
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

    private fun loadDuenios(connection: Connection): List<Document> {
        val duenios = mutableListOf<Document>()
        val sql = "SELECT id, nombre FROM usuarios WHERE tipo_usuario <> 'ADMIN' ORDER BY id"
        connection.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                while (rs.next()) {
                    duenios.add(
                        Document()
                            .append("_id", rs.getInt("id"))
                            .append("nombre", rs.getString("nombre")),
                    )
                }
            }
        }
        return duenios
    }

    private fun loadReservasPorLibro(connection: Connection): Map<Int, List<Pair<LocalDateTime, LocalDateTime>>> {
        val map = mutableMapOf<Int, MutableList<Pair<LocalDateTime, LocalDateTime>>>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT libro_id, fecha_desde, fecha_hasta FROM reservas ORDER BY libro_id, fecha_desde").use { rs ->
                while (rs.next()) {
                    val libroId = rs.getInt("libro_id")
                    val desde = rs.getTimestamp("fecha_desde")!!.toLocalDateTime()
                    val hasta = rs.getTimestamp("fecha_hasta")!!.toLocalDateTime()
                    map.getOrPut(libroId) { mutableListOf() }.add(desde to hasta)
                }
            }
        }
        return map
    }

    private fun toDate(localDate: LocalDate): Date =
        Date.from(localDate.atStartOfDay(ZoneId.of("UTC")).toInstant())

    private fun toDateTime(value: LocalDateTime): Date =
        Date.from(value.atZone(ZoneId.systemDefault()).toInstant())
}
