package ar.edu.unsam.phm.bootstrap

import ar.edu.unsam.phm.*
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AppBootstrap(
    val libroRepository: LibroRepository,
    private val usuarioRepository: UsuarioRepository,
): InitializingBean {
    // 1. Usuarios con sus credenciales
    val emilia = Usuario(
        Credencial("emilia", "123"),
        "Emilia Romero",
        "Lectora Ávida",
        "emilia@example.com",
        "11223344",
        "CABA",
        UsuarioLectorPublicador(),
        2345,
        fechaRegistro = LocalDate.of(2021, 6, 1)
    )

    val juan = Usuario(
        Credencial("juan", "123"),
        "Juan Pérez",
        "Lector intrépido",
        "juan@example.com",
        "11556677",
        "Morón",
        UsuarioLector(),
        500,
        fechaRegistro = LocalDate.of(2020, 9, 15)
    )

    val ana = Usuario(
        Credencial("ana", "123"),
        "Ana García",
        "Publicadora",
        "ana@example.com",
        "11889900",
        "Quilmes",
        UsuarioPublicador(),
        1200,
        fechaRegistro = LocalDate.of(2022, 2, 10)
    )

    val carlos = Usuario(
        Credencial("carlos", "123"),
        "Carlos Ruiz",
        "Fan de los clásicos",
        "carlos@example.com",
        "11334455",
        "Lanús",
        UsuarioLector(),
        150,
        fechaRegistro = LocalDate.of(2019, 12, 30)
    )

    //USUARIOS

    val user1 = Usuario(
        credencial = Credencial("juan@mail.com", "pass123"),
        nombre = "Juan Pérez",
        desc = "Lector intrépido",
        email = "juan@mail.com",
        celular = "11223344",
        ciudad = "San Martín",
        tipoUsuario = UsuarioLector(),
        bibliokarmas = 100,
        fechaRegistro = LocalDate.of(2021, 3, 15)
    )

    val user2 = Usuario(
        credencial = Credencial("mariag", "pass456"),
        nombre = "María Gómez",
        desc = "Devoradora de libros",
        email = "maria@mail.com",
        celular = "11556677",
        ciudad = "Villa Ballester",
        tipoUsuario = UsuarioPublicador(),
        bibliokarmas = 500,
        fechaRegistro = LocalDate.of(2020, 6, 10)
    )

    val user3 = Usuario(
        credencial = Credencial("carlosl", "pass789"),
        nombre = "Carlos López",
        desc = "Fan de la ciencia ficción",
        email = "carlos@mail.com",
        celular = "11889900",
        ciudad = "Caseros",
        tipoUsuario = UsuarioLectorPublicador(),
        bibliokarmas = 1200,
        fechaRegistro = LocalDate.of(2019, 9, 5)
    )

    val user4 = Usuario(
        credencial = Credencial("anad", "pass000"),
        nombre = "Ana Díaz",
        desc = "Amante del drama",
        email = "ana@mail.com",
        celular = "11443322",
        ciudad = "San Andrés",
        tipoUsuario = UsuarioLector(),
        bibliokarmas = 50,
        fechaRegistro = LocalDate.of(2022, 1, 20)
    )
    fun crearReservas(){
        listOf(emilia, juan, ana, carlos).forEach { usuarioRepository.create(it) }

        // 2. CATEGORÍA: "PRESTADOS A MÍ" (Emilia reserva libros de otros)
        repeat(5) { i ->
            // Ana es la dueña, Emilia reserva
            val libro = LibroComun(ana, 100 + i, "Harry Potter $i", "Coscu", "Desc", Genero.CIENCIA_FICCION, 300, "97801$i", Idioma.ESPANOL, "Minotauro", LocalDate.now(), EstadoLibro.EXCELENTE, "https://http2.mlstatic.com/D_NQ_NP_805995-MLA99469869724_112025-O.webp")
            libroRepository.create(libro)

            val res = Reserva(i, libro, emilia, LocalDate.now().plusDays(i.toLong()), LocalDate.now().plusDays(i.toLong() + 3))
            libro.agregarReserva(res) // [cite: 32, 33]
            emilia.bibliokarmas += libro.calcularBiblioKarmaTotal(emilia, res.fechaDesde, res.fechaHasta) //
        }

        // 3. CATEGORÍA: "PRESTADOS POR MÍ" (Otros reservan libros de Emilia) [cite: 42, 63]
        repeat(5) { i ->
            // Emilia es la dueña, Juan reserva
            val libro = LibroConDedicatoria(emilia, 200 + i, "El señor de los anillos $i", "Maria Claudia", "Desc", Genero.LITERATURA_CLASICA, 150, "97802$i", Idioma.FRANCES, "Sur", LocalDate.now(), EstadoLibro.MUY_BUENO, "https://http2.mlstatic.com/D_NQ_NP_639267-MLA81008530369_112024-O.webp")
            libroRepository.create(libro)

            val res = Reserva(50 + i, libro, juan, LocalDate.now().plusDays(i.toLong()), LocalDate.now().plusDays(i.toLong() + 5))
            libro.agregarReserva(res)
        }

        // 4. Libros Coleccionables adicionales (sin reservas actuales) [cite: 15, 86]
        repeat(8) { i ->
            val libro = LibroColeccionable(carlos, 300 + i, "Aura $i", "Luis", "Desc", Genero.DISENO, 800, "97803$i", Idioma.INGLES, "Taschen", LocalDate.now(), EstadoLibro.EXCELENTE, "https://http2.mlstatic.com/D_NQ_NP_2X_898813-MLA100509611482_122025-F.jpg")
            libroRepository.create(libro)
        }

        // 5. Casos de borde: Próximo a vencer para Emilia (vence mañana)
        val libroEspecial = LibroColeccionable(ana, 999, "Libro Raro", "Thomas", "Desc", Genero.CIENCIA_FICCION, 400, "978999", Idioma.ESPANOL, "Editor", LocalDate.now(), EstadoLibro.EXCELENTE, "https://dcdn-us.mitiendanube.com/stores/006/438/987/products/9789504984603_c6818713-5a23-42b4-9abc-3776eff69435-e4047a0d3582f16b5a17603956575426-480-0.webp")
        libroRepository.create(libroEspecial)
        val resVencimiento = Reserva(99, libroEspecial, emilia, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1))
        libroEspecial.agregarReserva(resVencimiento)
    }

    fun crearUsuarios(){
        usuarioRepository.apply {
            create(user1)
            create(user2)
            create(user3)
            create(user4)
        }
    }

    //LIBROS
    val libro1 = LibroComun(
        juan,
        0,
        "1984",
        "George Orwell",
        """En el año 1984 Londres es una ciudad lúgubre en la que la Policía del Pensamiento controla de forma asfixiante la vida de los ciudadanos. Winston Smith es un peón de este engranaje que comienza a darse cuenta de la farsa en la que vive y decide rebelarse.""".trimIndent(),
        Genero.CIENCIA_FICCION,
        328,
        "978-0451524935",
        Idioma.ESPANOL,
        "Debolsillo",
        LocalDate.of(1949, 6, 8),
        EstadoLibro.EXCELENTE,
        "https://contentv2.tap-commerce.com/cover/large/9789875669284_1.jpg?id_com=1156&back=fad2e1"
    )

    val libro2 = LibroComun(
        juan,
        0,
        "Clean Code",
        "Robert C. Martin",
        """Incluso el mal código puede funcionar. Pero si el código no es limpio, puede acabar con una empresa de desarrollo. Este libro es una lectura obligada para cualquier desarrollador, ingeniero de software o director de proyectos que quiera escribir mejor código.""".trimIndent(),
        Genero.DISENO,
        464,
        "978-0132350884",
        Idioma.INGLES,
        "Prentice Hall",
        LocalDate.of(2008, 8, 1),
        EstadoLibro.MUY_BUENO,
        "https://m.media-amazon.com/images/I/51E2055ZGUL._AC_UF1000,1000_QL80_.jpg"
    )

    val libro3 = LibroConDedicatoria(
        user2,
        0,
        "Orgullo y Prejuicio",
        "Jane Austen",
        """La historia sigue la vida de Elizabeth Bennet y sus cuatro hermanas, quienes enfrentan presiones sociales para casarse. La llegada del apuesto y rico Sr. Darcy y su amigo Bingley altera sus vidas, desatando una trama de amor, malentendidos y orgullo.""".trimIndent(),
        Genero.ROMANCE,
        432,
        "978-8420671848",
        Idioma.ESPANOL,
        "Alianza",
        LocalDate.of(1813, 1, 28),
        EstadoLibro.BUENO,
        "https://www.alianzaeditorial.es/imagenes/blog/9788411483018-orgullo-y-prejuicio.jpg"
    )

    val libro4 = LibroConDedicatoria(
        juan,
        0,
        "Hábitos Atómicos",
        "James Clear",
        """A menudo pensamos que para cambiar de vida tenemos que pensar en hacer cambios grandes. Nada más lejos de la realidad. Según James Clear, el cambio real proviene del resultado de cientos de pequeñas decisiones cotidianas: los hábitos atómicos.""".trimIndent(),
        Genero.AUTOAYUDA,
        320,
        "978-1847941831",
        Idioma.INGLES,
        "Penguin",
        LocalDate.of(2018, 10, 16),
        EstadoLibro.EXCELENTE,
        "https://images.booksense.com/images/122/694/9786075694122.jpg"
    )

    val libro5 = LibroColeccionable(
        user2,
        0,
        "El Señor de los Anillos: La Comunidad del Anillo",
        "J.R.R. Tolkien",
        """En la adormecida y pintoresca Comarca, un joven hobbit recibe un encargo: custodiar el Anillo Único y emprender un peligroso viaje para destruirlo en la Grieta del Destino. Una edición ilustrada espectacular e imperdible del clásico de la fantasía.""".trimIndent(),
        Genero.CIENCIA_FICCION,
        1200,
        "978-8445071754",
        Idioma.ESPANOL,
        "Minotauro",
        LocalDate.of(1954, 7, 29),
        EstadoLibro.EXCELENTE,
        "https://pdlibrosarg.cdnstatics2.com/usuaris/libros/thumbs/62c2f9e8-0ea1-403c-aa21-31c101eeaf40/d_360_620/384505_portada_el-senor-de-los-anillos-n-0103-la-comunidad-del-anillo-edicion-revisada_j-r-r-tolkien_202310231103.webp"
    )

    val libro6 = LibroColeccionable(
        user4,
        0,
        "Hamlet (Primera Edición Facsímil)",
        "William Shakespeare",
        """Una de las obras más influyentes de la literatura mundial. El príncipe Hamlet de Dinamarca busca vengar la muerte de su padre, asesinado por su propio hermano, Claudio, quien ha usurpado el trono y se ha casado con la viuda.""".trimIndent(),
        Genero.DRAMA,
        200,
        "978-0743477123",
        Idioma.INGLES,
        "Simon & Schuster",
        LocalDate.of(1603, 1, 1),
        EstadoLibro.REGULAR,
        "https://m.media-amazon.com/images/I/51egMsRda2L.jpg"
    )

    fun crearLibros(){
        libroRepository.apply {
            create(libro1)
            create(libro2)
            create(libro3)
            create(libro4)
            create(libro5)
            create(libro6)
        }
    }

    override fun afterPropertiesSet() {
        crearReservas()
        crearUsuarios()
        crearLibros()
    }
}