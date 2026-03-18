package ar.edu.unsam.phm.bootstrap

import ar.edu.unsam.phm.*
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
): InitializingBean {

    val juan = Usuario(Credencial("juan", "123"), "Juan Pérez", "Lector intrépido", "juan@mail.com", "11556677", "San Martín", UsuarioLector(), 500, fechaRegistro = LocalDate.of(2020, 9, 15)).apply { id = 1 }
    val emilia = Usuario(Credencial("emilia", "123"), "Emilia Romero", "Lectora Ávida", "emilia@example.com", "11223344", "CABA", UsuarioLectorPublicador(), 2345, fechaRegistro = LocalDate.of(2021, 6, 1)).apply { id = 2 }
    val ana = Usuario(Credencial("ana", "123"), "Ana García", "Publicadora", "ana@example.com", "11889900", "Quilmes", UsuarioPublicador(), 1200, fechaRegistro = LocalDate.of(2022, 2, 10)).apply { id = 3 }
    val carlos = Usuario(Credencial("carlos", "123"), "Carlos Ruiz", "Fan de los clásicos", "carlos@example.com", "11334455", "Lanús", UsuarioLector(), 150, fechaRegistro = LocalDate.of(2019, 12, 30)).apply { id = 4 }

    override fun afterPropertiesSet() {
        usuarioRepository.lista.clear()
        libroRepository.lista.clear()

        listOf(juan, emilia, ana, carlos).forEach { usuarioRepository.create(it) }

        configurarEscenarioJuan()
        crearLibrosAdicionales()
    }

    fun configurarEscenarioJuan() {

        // ── LIBROS DE ANA prestados A JUAN (prestados-a-mi) ──────────────────
        val librosDeAna = listOf(
            Triple("Harry Potter y la Piedra Filosofal", "J.K. Rowling",       "https://contentv2.tap-commerce.com/cover/large/9789875669284_1.jpg"),
            Triple("El Código Da Vinci",                 "Dan Brown",           "https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcSB8x01gOOsdG1D2IS0r3AquN2pGMtMnWLpFKPkF8pZZQ953Pbo1lHkAqxX1R_SWzHqmWeOC5Mcg7fbuJt_iLP76jzwWNtpbzPW3xMNlRZN"),
            Triple("Crónica de una muerte anunciada",    "Gabriel García Márquez", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSVTCjrjj-XTtQPa0m4C8zNQtyvHz1HeB4zsQ&s"),
            Triple("Rayuela",                            "Julio Cortázar",      "https://descargarlibrosenpdf.wordpress.com/wp-content/uploads/2017/05/rayuela-cortazar.jpg"),
            Triple("Ficciones",                          "Jorge Luis Borges",   "https://images.cdn1.buscalibre.com/fit-in/360x360/e4/a9/e4a975b4dce219d0b130b4d8915b8b88.jpg"),
            Triple("El nombre de la rosa",               "Umberto Eco",         "https://libroschorcha.wordpress.com/wp-content/uploads/2018/02/el-nombre-de-la-rosa-umberto-eco.jpg"),
        )

        librosDeAna.forEachIndexed { i, data ->
            val libro = LibroComun(ana, 1000 + i, data.first, data.second, "Descripción", Genero.LITERATURA_CLASICA, 300, "ISBN-$i", Idioma.ESPANOL, "Editorial", LocalDate.now(), EstadoLibro.EXCELENTE, data.third)
            libroRepository.create(libro)

            val (fDesde, fHasta) = when (i) {
                0 -> LocalDate.of(2026, 3, 1)  to LocalDate.of(2026, 3, 10) // DEVUELTO
                1 -> LocalDate.of(2026, 3, 10) to LocalDate.of(2026, 3, 16) // PRÓXIMO A VENCER
                2 -> LocalDate.of(2026, 3, 15) to LocalDate.of(2026, 3, 25) // ACTIVO
                3 -> LocalDate.of(2026, 2, 1)  to LocalDate.of(2026, 2, 15) // DEVUELTO (viejo)
                4 -> LocalDate.of(2026, 3, 17) to LocalDate.of(2026, 4, 1)  // ACTIVO reciente
                else -> LocalDate.of(2026, 3, 18) to LocalDate.of(2026, 4, 10) // ACTIVO
            }
            val reserva = Reserva(libro, juan, fDesde, fHasta).apply { id = 500 + i }
            juan.agregarReserva(reserva)
            ana.agregarReserva(reserva)
            reservaRepository.create(reserva)
        }

        // ── LIBROS DE CARLOS prestados A JUAN (prestados-a-mi) ───────────────
        val librosDeCarlos = listOf(
            Triple("Don Quijote de la Mancha", "Miguel de Cervantes", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQcMRZdGl9oMFn7_O4oyErwUXK9yrN3OcSe0w&s"),
            Triple("Crimen y Castigo",         "Fiódor Dostoyevski",  "https://www.edicontinente.com.ar/image/titulos/9788417477639.jpg"),
        )

        librosDeCarlos.forEachIndexed { i, data ->
            val libro = LibroComun(carlos, 1100 + i, data.first, data.second, "Descripción", Genero.LITERATURA_CLASICA, 400, "ISBN-C$i", Idioma.ESPANOL, "Editorial", LocalDate.now(), EstadoLibro.MUY_BUENO, data.third)
            libroRepository.create(libro)

            val (fDesde, fHasta) = when (i) {
                0 -> LocalDate.of(2026, 2, 10) to LocalDate.of(2026, 2, 28) // DEVUELTO
                else -> LocalDate.of(2026, 3, 18) to LocalDate.of(2026, 4, 5) // ACTIVO
            }
            val reserva = Reserva(libro, juan, fDesde, fHasta).apply { id = 520 + i }
            juan.agregarReserva(reserva)
            carlos.agregarReserva(reserva)
            reservaRepository.create(reserva)
        }

        // ── LIBROS DE JUAN prestados a EMILIA / CARLOS / ANA (mis-prestamos) ─
        val misLibros = listOf(
            Triple("1984",                   "George Orwell",      "https://contentv2.tap-commerce.com/cover/large/9789875669284_1.jpg"),
            Triple("Clean Code",             "Robert C. Martin",   "https://m.media-amazon.com/images/I/51E2055ZGUL._AC_UF1000,1000_QL80_.jpg"),
            Triple("El Principito",          "Antoine de Saint-Exupéry", "https://tienda.planetadelibros.com.ar/cdn/shop/products/portada_el-principito_antoine-de-saint-exupery_201507152131.jpg?v=1684356025"),
            Triple("Sapiens",                "Yuval Noah Harari",  "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSyAJxSA62_Z8k-pqxGp8x3Zp1bWoAtBjkZow&s"),
            Triple("El alquimista",          "Paulo Coelho",       "https://m.media-amazon.com/images/I/71aFt4+OTOL.jpg"),
            Triple("Dune",                   "Frank Herbert",      "https://pics.filmaffinity.com/Dune_Parte_Dos-802143593-large.jpg"),
            Triple("Fundación",              "Isaac Asimov",       "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9t9VngDwUP_IlIKXTYUAJkRC11dlsBX7ncw&s"),
        )

        val prestatarios = listOf(emilia, carlos, ana, emilia, carlos, emilia, ana)

        misLibros.forEachIndexed { i, data ->
            val libro = LibroComun(juan, 2000 + i, data.first, data.second, "Descripción", Genero.CIENCIA_FICCION, 350, "ISBN-J$i", Idioma.ESPANOL, "Editorial", LocalDate.now(), EstadoLibro.EXCELENTE, data.third)
            libroRepository.create(libro)

            val prestatario = prestatarios[i]
            val (fDesde, fHasta) = when (i) {
                0 -> LocalDate.of(2026, 3, 10) to LocalDate.of(2026, 3, 30) // ACTIVO
                1 -> LocalDate.of(2026, 3, 10) to LocalDate.of(2026, 3, 30) // ACTIVO
                2 -> LocalDate.of(2026, 2, 1)  to LocalDate.of(2026, 2, 20) // DEVUELTO
                3 -> LocalDate.of(2026, 3, 15) to LocalDate.of(2026, 3, 20) // PRÓXIMO A VENCER
                4 -> LocalDate.of(2026, 1, 5)  to LocalDate.of(2026, 1, 20) // DEVUELTO (viejo)
                5 -> LocalDate.of(2026, 3, 17) to LocalDate.of(2026, 4, 15) // ACTIVO
                else -> LocalDate.of(2026, 3, 18) to LocalDate.of(2026, 4, 20) // ACTIVO
            }
            val reserva = Reserva(libro, prestatario, fDesde, fHasta).apply { id = 600 + i }
            juan.agregarReserva(reserva)
            prestatario.agregarReserva(reserva)
            reservaRepository.create(reserva)
        }
    }

    fun crearLibrosAdicionales() {
        val libro5 = LibroColeccionable(ana, 3005, "El Señor de los Anillos", "J.R.R. Tolkien", "Fantasía", Genero.CIENCIA_FICCION, 1200, "ISBN-3005", Idioma.ESPANOL, "Minotauro", LocalDate.now(), EstadoLibro.EXCELENTE, "https://m.media-amazon.com/images/I/91R196S8S8L.jpg")
        libroRepository.create(libro5)
    }
}