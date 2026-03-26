package ar.edu.unsam.phm.bootstrap

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AppBootstrap(
    val libroRepository: LibroRepository,
    val usuarioRepository: UsuarioRepository,
    val reservaRepository: ReservaRepository,
) : InitializingBean {

    // ── Usuarios ──────────────────────────────────────────────────────────────

    val juan = Usuario(
        credencial = Credencial("juan@mail.com", "123"),
        nombre = "Juan Pérez",
        desc = "Lector intrépido",
        celular = "11556677",
        ciudad = "San Martín",
        tipoUsuario = UsuarioLector(),
        bibliokarmas = 500,
        imagenUrl = "https://randomuser.me/api/portraits/men/1.jpg",
        fechaRegistro = LocalDate.of(2020, 9, 15)
    )
    val emilia = Usuario(
        credencial = Credencial("emilia@example.com", "123"),
        nombre = "Emilia Romero",
        desc = "Lectora Ávida",
        celular = "11223344",
        ciudad = "CABA",
        tipoUsuario = UsuarioLectorPublicador(),
        bibliokarmas = 2345,
        imagenUrl = "https://randomuser.me/api/portraits/women/2.jpg",
        fechaRegistro = LocalDate.of(2021, 6, 1)
    )
    val ana = Usuario(
        credencial = Credencial("ana@example.com", "123"),
        nombre = "Ana García",
        desc = "Publicadora",
        celular = "11889900",
        ciudad = "Quilmes",
        tipoUsuario = UsuarioPublicador(),
        bibliokarmas = 1200,
        imagenUrl = "https://randomuser.me/api/portraits/women/3.jpg",
        fechaRegistro = LocalDate.of(2022, 2, 10)
    )
    val carlos = Usuario(
        credencial = Credencial("carlos@example.com", "123"),
        nombre = "Carlos Ruiz",
        desc = "Fan de los clásicos",
        celular = "11334455",
        ciudad = "Lanús",
        tipoUsuario = UsuarioLector(),
        bibliokarmas = 150,
        imagenUrl = "https://randomuser.me/api/portraits/men/4.jpg",
        fechaRegistro = LocalDate.of(2019, 12, 30)
    )

    override fun afterPropertiesSet() {
        usuarioRepository.lista.clear()
        libroRepository.lista.clear()
        reservaRepository.lista.clear()

        listOf(juan, emilia, ana, carlos).forEach { usuarioRepository.create(it) }

        val librosComunes       = crearLibrosComunes()
        val librosConDedicatoria = crearLibrosConDedicatoria()
        val librosColeccionables = crearLibrosColeccionables()

        crearReservas(librosComunes, librosConDedicatoria, librosColeccionables)
        agregarCalificaciones(librosComunes, librosConDedicatoria, librosColeccionables)
    }

    // ── Libros Comunes (dueño: juan y carlos) ─────────────────────────────────

    private fun crearLibrosComunes(): List<Libro> {
        val datos = listOf(
            LibroData(duenio = juan, titulo = "1984", autor = "George Orwell",
                descripcion = "Una distopía sobre el totalitarismo y la vigilancia del Estado.",
                genero = Genero.CIENCIA_FICCION, cantidadPaginas = 328, isbn = "9780451524935",
                idioma = Idioma.ESPANOL, editorial = "Debolsillo",
                fechaPublicacion = LocalDate.of(1949, 6, 8), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://contentv2.tap-commerce.com/cover/large/9789875669284_1.jpg",
                fechaAgregado = LocalDate.of(2025, 1, 5)),
            LibroData(duenio = juan, titulo = "Clean Code", autor = "Robert C. Martin",
                descripcion = "Guía práctica para escribir código limpio y mantenible.",
                genero = Genero.DISENO, cantidadPaginas = 464, isbn = "9780132350884",
                idioma = Idioma.INGLES, editorial = "Prentice Hall",
                fechaPublicacion = LocalDate.of(2008, 8, 1), estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "https://m.media-amazon.com/images/I/51E2055ZGUL._AC_UF1000,1000_QL80_.jpg",
                fechaAgregado = LocalDate.of(2025, 1, 6)),
            LibroData(duenio = juan, titulo = "El Principito", autor = "Antoine de Saint-Exupéry",
                descripcion = "Un clásico de la literatura universal sobre la infancia y la amistad.",
                genero = Genero.DRAMA, cantidadPaginas = 96, isbn = "9780156012195",
                idioma = Idioma.ESPANOL, editorial = "Salamandra",
                fechaPublicacion = LocalDate.of(1943, 4, 6), estado = EstadoLibro.BUENO,
                imagenUrl = "https://tienda.planetadelibros.com.ar/cdn/shop/products/portada_el-principito_antoine-de-saint-exupery_201507152131.jpg",
                fechaAgregado = LocalDate.of(2025, 1, 7)),
            LibroData(duenio = juan, titulo = "Sapiens", autor = "Yuval Noah Harari",
                descripcion = "Un recorrido por la historia de la humanidad desde los orígenes.",
                genero = Genero.AUTOAYUDA, cantidadPaginas = 443, isbn = "9780062316097",
                idioma = Idioma.ESPANOL, editorial = "Debate",
                fechaPublicacion = LocalDate.of(2011, 1, 1), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSyAJxSA62_Z8k-pqxGp8x3Zp1bWoAtBjkZow&s",
                fechaAgregado = LocalDate.of(2025, 1, 8)),
            LibroData(duenio = carlos, titulo = "Don Quijote de la Mancha", autor = "Miguel de Cervantes",
                descripcion = "La obra cumbre de la literatura española sobre el caballero de La Mancha.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 1023, isbn = "9788467027983",
                idioma = Idioma.ESPANOL, editorial = "Espasa",
                fechaPublicacion = LocalDate.of(1605, 1, 16), estado = EstadoLibro.BUENO,
                imagenUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQcMRZdGl9oMFn7_O4oyErwUXK9yrN3OcSe0w&s",
                fechaAgregado = LocalDate.of(2024, 9, 10)),
            LibroData(duenio = carlos, titulo = "Crimen y Castigo", autor = "Fiódor Dostoyevski",
                descripcion = "Un estudiante comete un crimen y lidia con las consecuencias psicológicas.",
                genero = Genero.DRAMA, cantidadPaginas = 671, isbn = "9788420674469",
                idioma = Idioma.ESPANOL, editorial = "Alianza",
                fechaPublicacion = LocalDate.of(1866, 1, 1), estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "https://www.edicontinente.com.ar/image/titulos/9788417477639.jpg",
                fechaAgregado = LocalDate.of(2024, 9, 11)),
            LibroData(duenio = carlos, titulo = "Dune", autor = "Frank Herbert",
                descripcion = "La épica historia de Paul Atreides en el planeta desértico Arrakis.",
                genero = Genero.CIENCIA_FICCION, cantidadPaginas = 688, isbn = "9780441013593",
                idioma = Idioma.ESPANOL, editorial = "Minotauro",
                fechaPublicacion = LocalDate.of(1965, 8, 1), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTrP4Ohql-5Cf2oMJNHvnj5V-ZVm3Q8rpNj0OJXuGa2dOgPMIVMchKJCRSDEJnbUsJ7mHh1C3xE3wfDDncifQkAiSixNji3-L2yN8laRAU",
                fechaAgregado = LocalDate.of(2024, 9, 12)),
            LibroData(duenio = carlos, titulo = "El alquimista", autor = "Paulo Coelho",
                descripcion = "Un pastor andaluz viaja en busca de su tesoro personal.",
                genero = Genero.AUTOAYUDA, cantidadPaginas = 208, isbn = "9780062315007",
                idioma = Idioma.ESPANOL, editorial = "Planeta",
                fechaPublicacion = LocalDate.of(1988, 1, 1), estado = EstadoLibro.BUENO,
                imagenUrl = "https://m.media-amazon.com/images/I/71aFt4+OTOL.jpg",
                fechaAgregado = LocalDate.of(2024, 9, 13)),
        )
        return datos.map { libroRepository.create(LibroComun(it)) }
    }

    // ── Libros Con Dedicatoria (dueño: ana y emilia) ──────────────────────────

    private fun crearLibrosConDedicatoria(): List<Libro> {
        val datos = listOf(
            LibroData(duenio = ana, titulo = "Harry Potter y la Piedra Filosofal", autor = "J.K. Rowling",
                descripcion = "Un joven descubre que es un mago y asiste a la escuela Hogwarts.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 309, isbn = "9788478884452",
                idioma = Idioma.ESPANOL, editorial = "Salamandra",
                fechaPublicacion = LocalDate.of(1997, 6, 26), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://images.cdn3.buscalibre.com/fit-in/360x360/d7/e7/d7e777ce24f10eabdaae28d65ab70bd3.jpg",
                fechaAgregado = LocalDate.of(2024, 3, 1)),
            LibroData(duenio = ana, titulo = "El Código Da Vinci", autor = "Dan Brown",
                descripcion = "Un criptólogo descifra una serie de enigmas escondidos en obras de Da Vinci.",
                genero = Genero.DRAMA, cantidadPaginas = 489, isbn = "9788408174189",
                idioma = Idioma.ESPANOL, editorial = "Planeta",
                fechaPublicacion = LocalDate.of(2003, 3, 18), estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcSB8x01gOOsdG1D2IS0r3AquN2pGMtMnWLpFKPkF8pZZQ953Pbo1lHkAqxX1R_SWzHqmWeOC5Mcg7fbuJt_iLP76jzwWNtpbzPW3xMNlRZN",
                fechaAgregado = LocalDate.of(2024, 3, 2)),
            LibroData(duenio = ana, titulo = "Crónica de una muerte anunciada", autor = "Gabriel García Márquez",
                descripcion = "El relato del asesinato de Santiago Nasar en un pueblo del Caribe.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 120, isbn = "9788497592437",
                idioma = Idioma.ESPANOL, editorial = "Diana",
                fechaPublicacion = LocalDate.of(1981, 1, 1), estado = EstadoLibro.BUENO,
                imagenUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSVTCjrjj-XTtQPa0m4C8zNQtyvHz1HeB4zsQ&s",
                fechaAgregado = LocalDate.of(2024, 3, 3)),
            LibroData(duenio = ana, titulo = "Rayuela", autor = "Julio Cortázar",
                descripcion = "Una novela experimental que puede leerse en múltiples órdenes.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 600, isbn = "9788420471822",
                idioma = Idioma.ESPANOL, editorial = "Alfaguara",
                fechaPublicacion = LocalDate.of(1963, 1, 1), estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "https://descargarlibrosenpdf.wordpress.com/wp-content/uploads/2017/05/rayuela-cortazar.jpg",
                fechaAgregado = LocalDate.of(2024, 3, 4)),
            LibroData(duenio = emilia, titulo = "Ficciones", autor = "Jorge Luis Borges",
                descripcion = "Una colección de cuentos que mezclan lo fantástico con lo filosófico.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 224, isbn = "9788420633138",
                idioma = Idioma.ESPANOL, editorial = "Alianza",
                fechaPublicacion = LocalDate.of(1944, 1, 1), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://images.cdn1.buscalibre.com/fit-in/360x360/e4/a9/e4a975b4dce219d0b130b4d8915b8b88.jpg",
                fechaAgregado = LocalDate.of(2024, 6, 1)),
            LibroData(duenio = emilia, titulo = "El nombre de la rosa", autor = "Umberto Eco",
                descripcion = "Un monje investiga una serie de muertes misteriosas en una abadía medieval.",
                genero = Genero.DRAMA, cantidadPaginas = 500, isbn = "9788408081869",
                idioma = Idioma.ESPANOL, editorial = "Lumen",
                fechaPublicacion = LocalDate.of(1980, 1, 1), estado = EstadoLibro.BUENO,
                imagenUrl = "https://libroschorcha.wordpress.com/wp-content/uploads/2018/02/el-nombre-de-la-rosa-umberto-eco.jpg",
                fechaAgregado = LocalDate.of(2024, 6, 2)),
            LibroData(duenio = emilia, titulo = "Fundación", autor = "Isaac Asimov",
                descripcion = "Un matemático prevé la caída del Imperio Galáctico y planea preservar el conocimiento.",
                genero = Genero.CIENCIA_FICCION, cantidadPaginas = 244, isbn = "9788435018029",
                idioma = Idioma.ESPANOL, editorial = "Minotauro",
                fechaPublicacion = LocalDate.of(1951, 1, 1), estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9t9VngDwUP_IlIKXTYUAJkRC11dlsBX7ncw&s",
                fechaAgregado = LocalDate.of(2024, 6, 3)),
            LibroData(duenio = emilia, titulo = "Orgullo y Prejuicio", autor = "Jane Austen",
                descripcion = "La historia de amor entre Elizabeth Bennet y el señor Darcy en la Inglaterra del siglo XIX.",
                genero = Genero.ROMANCE, cantidadPaginas = 432, isbn = "9788491050421",
                idioma = Idioma.ESPANOL, editorial = "Austral",
                fechaPublicacion = LocalDate.of(1813, 1, 28), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://m.media-amazon.com/images/I/71Q1tPupKjL.jpg",
                fechaAgregado = LocalDate.of(2024, 6, 4)),
        )
        return datos.map { libroRepository.create(LibroConDedicatoria(it)) }
    }

    // ── Libros Coleccionables (dueño: ana y juan) ─────────────────────────────

    private fun crearLibrosColeccionables(): List<Libro> {
        val datos = listOf(
            LibroData(duenio = ana, titulo = "El Señor de los Anillos", autor = "J.R.R. Tolkien",
                descripcion = "La épica lucha entre el bien y el mal en la Tierra Media.",
                genero = Genero.CIENCIA_FICCION, cantidadPaginas = 1200, isbn = "9780000003005",
                idioma = Idioma.ESPANOL, editorial = "Minotauro",
                fechaPublicacion = LocalDate.of(1954, 7, 29), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://d22fxaf9t8d39k.cloudfront.net/2e23a0a073388dbaef0aa42e5c907cd5d9c5e52545af57bdfbf7f983b41eefcf114336.jpg",
                fechaAgregado = LocalDate.of(2025, 2, 20)),
            LibroData(duenio = ana, titulo = "Guerra y Paz", autor = "León Tolstói",
                descripcion = "La monumental novela sobre la invasión napoleónica de Rusia.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 1440, isbn = "9788420651835",
                idioma = Idioma.ESPANOL, editorial = "Alianza",
                fechaPublicacion = LocalDate.of(1869, 1, 1), estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "https://acdn-us.mitiendanube.com/stores/542/287/products/guerra_y_paz_lev_tolstoi1-4249aaf56c93a9f4e716770091781341-640-0.webp",
                fechaAgregado = LocalDate.of(2025, 2, 21)),
            LibroData(duenio = ana, titulo = "Los Miserables", autor = "Victor Hugo",
                descripcion = "La historia de Jean Valjean y su redención en la Francia del siglo XIX.",
                genero = Genero.DRAMA, cantidadPaginas = 1900, isbn = "9788420651842",
                idioma = Idioma.FRANCES, editorial = "Alianza",
                fechaPublicacion = LocalDate.of(1862, 1, 1), estado = EstadoLibro.BUENO,
                imagenUrl = "https://http2.mlstatic.com/D_NQ_NP_612382-MLA42127442306_062020-O.webp",
                fechaAgregado = LocalDate.of(2025, 2, 22)),
            LibroData(duenio = ana, titulo = "En busca del tiempo perdido", autor = "Marcel Proust",
                descripcion = "Una exploración de la memoria, el tiempo y la identidad en la alta sociedad francesa.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 4215, isbn = "9788420652009",
                idioma = Idioma.FRANCES, editorial = "Alianza",
                fechaPublicacion = LocalDate.of(1913, 11, 14), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://sbslibreria.vtexassets.com/arquivos/ids/5092712-1200-auto?v=638863180619070000&width=1200&height=auto&aspect=true",
                fechaAgregado = LocalDate.of(2025, 2, 23)),
            LibroData(duenio = juan, titulo = "El Quijote Ilustrado", autor = "Miguel de Cervantes",
                descripcion = "Edición de colección con ilustraciones originales del siglo XVII.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 1200, isbn = "9788467035001",
                idioma = Idioma.ESPANOL, editorial = "Espasa Edición Especial",
                fechaPublicacion = LocalDate.of(1605, 1, 16), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://images.cdn3.buscalibre.com/fit-in/360x360/3a/fc/3afcfcb813bc98ee2f549f21cb4a16b2.jpg",
                fechaAgregado = LocalDate.of(2025, 3, 1)),
            LibroData(duenio = juan, titulo = "Cien años de soledad", autor = "Gabriel García Márquez",
                descripcion = "La saga de la familia Buendía en el mítico pueblo de Macondo.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 471, isbn = "9780307474728",
                idioma = Idioma.ESPANOL, editorial = "RAE Edición Conmemorativa",
                fechaPublicacion = LocalDate.of(1967, 5, 30), estado = EstadoLibro.EXCELENTE,
                imagenUrl = "https://www.rae.es/sites/default/files/styles/obra_portada_ficha/public/portada_cien_anos_de_soledad_0.jpg?itok=DDV1xNqg",
                fechaAgregado = LocalDate.of(2025, 3, 2)),
            LibroData(duenio = juan, titulo = "Ulises", autor = "James Joyce",
                descripcion = "Un día en la vida de Leopold Bloom en Dublín, obra clave del modernismo.",
                genero = Genero.LITERATURA_CLASICA, cantidadPaginas = 933, isbn = "9788420651866",
                idioma = Idioma.INGLES, editorial = "Alianza Edición Numerada",
                fechaPublicacion = LocalDate.of(1922, 2, 2), estado = EstadoLibro.MUY_BUENO,
                imagenUrl = "https://http2.mlstatic.com/D_NQ_NP_728018-MLU70496468924_072023-O.webp",
                fechaAgregado = LocalDate.of(2025, 3, 3)),
            LibroData(duenio = juan, titulo = "Moby Dick", autor = "Herman Melville",
                descripcion = "La obsesiva cacería de la ballena blanca por el capitán Ahab.",
                genero = Genero.DRAMA, cantidadPaginas = 720, isbn = "9788420651873",
                idioma = Idioma.INGLES, editorial = "Alianza Edición Especial",
                fechaPublicacion = LocalDate.of(1851, 10, 18), estado = EstadoLibro.BUENO,
                imagenUrl = "https://m.media-amazon.com/images/I/81OthjkJBuL.jpg",
                fechaAgregado = LocalDate.of(2025, 3, 4)),
        )
        return datos.map { libroRepository.create(LibroColeccionable(it)) }
    }

    // ── Reservas ──────────────────────────────────────────────────────────────

    private fun crearReservas(
        comunes: List<Libro>,
        conDedicatoria: List<Libro>,
        coleccionables: List<Libro>
    ) {
        var reservaId = 500

        fun reservar(libro: Libro, solicitante: Usuario, desde: LocalDate, hasta: LocalDate) {
            val reserva = Reserva(libro, solicitante, desde, hasta).apply { id = reservaId++ }
            solicitante.agregarReserva(reserva)
            if (libro.duenio.id != solicitante.id) libro.duenio.agregarReserva(reserva)
            reservaRepository.create(reserva)
        }

        // Juan reserva libros de ana y emilia
        reservar(conDedicatoria[0], juan, LocalDate.of(2026, 3, 1),  LocalDate.of(2026, 3, 10))
        reservar(conDedicatoria[1], juan, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 16))
        reservar(conDedicatoria[4], juan, LocalDate.of(2026, 3, 17), LocalDate.of(2026, 4, 1))
        reservar(coleccionables[0], juan, LocalDate.of(2026, 4, 5),  LocalDate.of(2026, 4, 20))

        // Emilia reserva libros de juan y carlos
        reservar(comunes[0], emilia, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 30))
        reservar(comunes[3], emilia, LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 20))
        reservar(coleccionables[4], emilia, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15))

        // Ana reserva libros de juan y carlos
        reservar(comunes[1], ana, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 30))
        reservar(comunes[6], ana, LocalDate.of(2026, 3, 18), LocalDate.of(2026, 4, 20))
        reservar(coleccionables[5], ana, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 20))

        // Carlos reserva libros de ana, emilia y juan
        reservar(conDedicatoria[2], carlos, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 28))
        reservar(conDedicatoria[5], carlos, LocalDate.of(2026, 3, 18), LocalDate.of(2026, 4, 5))
        reservar(comunes[2], carlos, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 20))
        reservar(coleccionables[6], carlos, LocalDate.of(2026, 3, 17), LocalDate.of(2026, 4, 15))
    }

    // ── Calificaciones ────────────────────────────────────────────────────────

    private fun agregarCalificaciones(
        comunes: List<Libro>,
        conDedicatoria: List<Libro>,
        coleccionables: List<Libro>
    ) {
        // 1984 — muchas reseñas
        comunes[0].apply {
            agregarCalificacion(Calificacion(emilia.id, 5, "Una obra maestra absoluta. Orwell adelantó décadas lo que vendría."))
            agregarCalificacion(Calificacion(ana.id,    4, "Impactante y perturbadora. Lectura obligatoria."))
            agregarCalificacion(Calificacion(carlos.id, 5, "De las mejores que leí. El concepto del doblepensar es genial."))
        }

        // Clean Code — pocas reseñas
        comunes[1].apply {
            agregarCalificacion(Calificacion(emilia.id, 4, "Muy útil para cualquier desarrollador. Cambió cómo escribo código."))
        }

        // Sapiens — algunas reseñas
        comunes[3].apply {
            agregarCalificacion(Calificacion(emilia.id, 5, "Te cambia la perspectiva del mundo. Increíble."))
            agregarCalificacion(Calificacion(carlos.id, 4, "Muy bien escrito, aunque algunas partes son discutibles."))
        }

        // Harry Potter — muchas reseñas
        conDedicatoria[0].apply {
            agregarCalificacion(Calificacion(juan.id,   5, "Un clásico eterno. La magia de Hogwarts nunca envejece."))
            agregarCalificacion(Calificacion(emilia.id, 5, "La leí de chica y la sigo amando. Pura nostalgia."))
            agregarCalificacion(Calificacion(carlos.id, 4, "Entretenidísima. Entiende por qué arrasó en todo el mundo."))
        }

        // Ficciones de Borges — pocas reseñas
        conDedicatoria[4].apply {
            agregarCalificacion(Calificacion(juan.id, 5, "Borges es insuperable. Cada cuento es un universo."))
        }

        // Rayuela — algunas reseñas
        conDedicatoria[3].apply {
            agregarCalificacion(Calificacion(juan.id,   4, "La forma de leerla en saltos es una experiencia única."))
            agregarCalificacion(Calificacion(carlos.id, 3, "Interesante pero exige mucho del lector. No es para todos."))
        }

        // El Señor de los Anillos — muchas reseñas
        coleccionables[0].apply {
            agregarCalificacion(Calificacion(juan.id,   5, "La fantasía épica por excelencia. Tolkien es un genio."))
            agregarCalificacion(Calificacion(emilia.id, 5, "Esta edición de colección es una joya. El libro también."))
            agregarCalificacion(Calificacion(carlos.id, 4, "Largo pero cada página vale la pena."))
        }

        // Cien años de soledad — algunas reseñas
        coleccionables[5].apply {
            agregarCalificacion(Calificacion(emilia.id, 5, "El realismo mágico en su máxima expresión."))
            agregarCalificacion(Calificacion(ana.id,    4, "Una saga familiar fascinante. García Márquez es único."))
        }
    }
}