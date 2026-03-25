package ar.edu.unsam.phm.bootstrap

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import java.time.LocalDate

data class DatosLibro(
    val titulo: String,
    val autor: String,
    val autorImg: String,
    val portadaImg: String
)

@Service
class AppBootstrap(
    val libroRepository: LibroRepository,
    val usuarioRepository: UsuarioRepository,
    val reservaRepository: ReservaRepository,
): InitializingBean {

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

        configurarEscenarioJuan()
        crearLibrosAdicionales()
    }

    fun configurarEscenarioJuan() {

        // ── LIBROS DE ANA prestados A JUAN (prestados-a-mi) ──────────────────
        val librosDeAna = listOf(
            DatosLibro("Harry Potter y la Piedra Filosofal", "J.K. Rowling", "https://upload.wikimedia.org/wikipedia/commons/5/5d/J._K._Rowling_2010.jpg", "https://images.cdn3.buscalibre.com/fit-in/360x360/d7/e7/d7e777ce24f10eabdaae28d65ab70bd3.jpg"),
            DatosLibro("El Código Da Vinci", "Dan Brown", "https://upload.wikimedia.org/wikipedia/commons/8/8b/Dan_Brown_book_signing.jpg", "https://encrypted-tbn0.gstatic.com/shopping?q=tbn:ANd9GcSB8x01gOOsdG1D2IS0r3AquN2pGMtMnWLpFKPkF8pZZQ953Pbo1lHkAqxX1R_SWzHqmWeOC5Mcg7fbuJt_iLP76jzwWNtpbzPW3xMNlRZN"),
            DatosLibro("Crónica de una muerte anunciada", "Gabriel García Márquez", "https://upload.wikimedia.org/wikipedia/commons/0/0f/Gabriel_Garcia_Marquez.jpg", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSVTCjrjj-XTtQPa0m4C8zNQtyvHz1HeB4zsQ&s"),
            DatosLibro("Rayuela", "Julio Cortázar", "https://upload.wikimedia.org/wikipedia/commons/1/13/Julio_Cort%C3%A1zar_%281967%29.jpg", "https://descargarlibrosenpdf.wordpress.com/wp-content/uploads/2017/05/rayuela-cortazar.jpg"),
            DatosLibro("Ficciones", "Jorge Luis Borges", "https://upload.wikimedia.org/wikipedia/commons/c/c1/Jorge_Luis_Borges_1951%2C_by_Grete_Stern.jpg", "https://images.cdn1.buscalibre.com/fit-in/360x360/e4/a9/e4a975b4dce219d0b130b4d8915b8b88.jpg"),
            DatosLibro("El nombre de la rosa", "Umberto Eco", "https://upload.wikimedia.org/wikipedia/commons/c/c2/Umberto_Eco_1984.jpg", "https://libroschorcha.wordpress.com/wp-content/uploads/2018/02/el-nombre-de-la-rosa-umberto-eco.jpg"),
        )

        librosDeAna.forEachIndexed { i, datos ->
            val data = LibroData(
                duenio = ana,
                titulo = datos.titulo,
                autor = datos.autor,
                autorImg = datos.autorImg,
                descripcion = "Descripción",
                genero = Genero.LITERATURA_CLASICA,
                cantidadPaginas = 300,
                isbn = "ISBN-$i",
                idioma = Idioma.ESPANOL,
                editorial = "Editorial",
                fechaPublicacion = LocalDate.now(),
                estado = EstadoLibro.EXCELENTE,
                imagenUrl = datos.portadaImg,
                fechaAgregado = LocalDate.of(2024, 3, 1).plusDays(i.toLong()),
            )
            val libro = LibroComun(data)
            libroRepository.create(libro)

            val (fDesde, fHasta) = when (i) {
                0 -> LocalDate.of(2026, 3, 1)  to LocalDate.of(2026, 3, 10)
                1 -> LocalDate.of(2026, 3, 10) to LocalDate.of(2026, 3, 16)
                2 -> LocalDate.of(2026, 3, 15) to LocalDate.of(2026, 3, 25)
                3 -> LocalDate.of(2026, 2, 1)  to LocalDate.of(2026, 2, 15)
                4 -> LocalDate.of(2026, 3, 17) to LocalDate.of(2026, 4, 1)
                else -> LocalDate.of(2026, 3, 18) to LocalDate.of(2026, 4, 10)
            }
            val reserva = Reserva(libro, juan, fDesde, fHasta).apply { id = 500 + i }
            juan.agregarReserva(reserva)
            ana.agregarReserva(reserva)
            reservaRepository.create(reserva)
        }

        // ── LIBROS DE CARLOS prestados A JUAN (prestados-a-mi) ───────────────
        val librosDeCarlos = listOf(
            DatosLibro("Don Quijote de la Mancha", "Miguel de Cervantes", "https://upload.wikimedia.org/wikipedia/commons/0/09/Cervantes_J%C3%A1uregui.jpg", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQcMRZdGl9oMFn7_O4oyErwUXK9yrN3OcSe0w&s"),
            DatosLibro("Crimen y Castigo", "Fiódor Dostoyevski", "https://upload.wikimedia.org/wikipedia/commons/7/78/Vasily_Perov_-_%D0%9F%D0%BE%D1%80%D1%82%D1%80%D0%B5%D1%82_%D0%A4.%D0%9C.%D0%94%D0%BE%D1%81%D1%82%D0%BE%D0%B5%D0%B2%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_-_Google_Art_Project.jpg", "https://www.edicontinente.com.ar/image/titulos/9788417477639.jpg"),
        )

        librosDeCarlos.forEachIndexed { i, datos ->
            val data = LibroData(
                duenio = carlos,
                titulo = datos.titulo,
                autor = datos.autor,
                autorImg = datos.autorImg,
                descripcion = "Descripción",
                genero = Genero.LITERATURA_CLASICA,
                cantidadPaginas = 400,
                isbn = "ISBN-C$i",
                idioma = Idioma.ESPANOL,
                editorial = "Editorial",
                fechaPublicacion = LocalDate.now(),
                estado = EstadoLibro.MUY_BUENO,
                imagenUrl = datos.portadaImg,
                fechaAgregado = LocalDate.of(2024, 9, 10).plusDays(i.toLong()),
            )
            val libro = LibroComun(data)
            libroRepository.create(libro)

            val (fDesde, fHasta) = when (i) {
                0 -> LocalDate.of(2026, 2, 10) to LocalDate.of(2026, 2, 28)
                else -> LocalDate.of(2026, 3, 18) to LocalDate.of(2026, 4, 5)
            }
            val reserva = Reserva(libro, juan, fDesde, fHasta).apply { id = 520 + i }
            juan.agregarReserva(reserva)
            carlos.agregarReserva(reserva)
            reservaRepository.create(reserva)
        }

        // ── LIBROS DE JUAN prestados a EMILIA / CARLOS / ANA (mis-prestamos) ─
        val misLibros = listOf(
            DatosLibro("1984", "George Orwell", "https://upload.wikimedia.org/wikipedia/commons/7/7e/George_Orwell_press_photo.jpg", "https://contentv2.tap-commerce.com/cover/large/9789875669284_1.jpg"),
            DatosLibro("Clean Code", "Robert C. Martin", "https://upload.wikimedia.org/wikipedia/commons/e/ee/Robert_Cecil_Martin.png", "https://m.media-amazon.com/images/I/51E2055ZGUL._AC_UF1000,1000_QL80_.jpg"),
            DatosLibro("El Principito", "Antoine de Saint-Exupéry", "https://upload.wikimedia.org/wikipedia/commons/0/07/Antoine_de_Saint-Exup%C3%A9ry.jpg", "https://tienda.planetadelibros.com.ar/cdn/shop/products/portada_el-principito_antoine-de-saint-exupery_201507152131.jpg?v=1684356025"),
            DatosLibro("Sapiens", "Yuval Noah Harari", "https://upload.wikimedia.org/wikipedia/commons/0/07/Yuval_Noah_Harari_at_the_WEF_Annual_Meeting_2020.jpg", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSyAJxSA62_Z8k-pqxGp8x3Zp1bWoAtBjkZow&s"),
            DatosLibro("El alquimista", "Paulo Coelho", "https://upload.wikimedia.org/wikipedia/commons/6/6d/Paulo_Coelho_en_2007.jpg", "https://m.media-amazon.com/images/I/71aFt4+OTOL.jpg"),
            DatosLibro("Dune", "Frank Herbert", "https://upload.wikimedia.org/wikipedia/commons/5/5e/Frank_Herbert_-_1984.jpg", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTrP4Ohql-5Cf2oMJNHvnj5V-ZVm3Q8rpNj0OJXuGa2dOgPMIVMchKJCRSDEJnbUsJ7mHh1C3xE3wfDDncifQkAiSixNji3-L2yN8laRAU&s=10"),
            DatosLibro("Fundación", "Isaac Asimov", "https://upload.wikimedia.org/wikipedia/commons/3/34/Isaac.Asimov01.jpg", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9t9VngDwUP_IlIKXTYUAJkRC11dlsBX7ncw&s"),
        )

        val prestatarios = listOf(emilia, carlos, ana, emilia, carlos, emilia, ana)

        misLibros.forEachIndexed { i, datos ->
            val data = LibroData(
                duenio = juan,
                titulo = datos.titulo,
                autor = datos.autor,
                autorImg = datos.autorImg,
                descripcion = "Descripción",
                genero = Genero.CIENCIA_FICCION,
                cantidadPaginas = 350,
                isbn = "ISBN-J$i",
                idioma = Idioma.ESPANOL,
                editorial = "Editorial",
                fechaPublicacion = LocalDate.now(),
                estado = EstadoLibro.EXCELENTE,
                imagenUrl = datos.portadaImg,
                fechaAgregado = LocalDate.of(2025, 1, 5).plusDays(i.toLong()),
            )
            val libro = LibroComun(data)
            libroRepository.create(libro)

            val prestatario = prestatarios[i]
            val (fDesde, fHasta) = when (i) {
                0 -> LocalDate.of(2026, 3, 10) to LocalDate.of(2026, 3, 30)
                1 -> LocalDate.of(2026, 3, 10) to LocalDate.of(2026, 3, 30)
                2 -> LocalDate.of(2026, 2, 1)  to LocalDate.of(2026, 2, 20)
                3 -> LocalDate.of(2026, 3, 15) to LocalDate.of(2026, 3, 20)
                4 -> LocalDate.of(2026, 1, 5)  to LocalDate.of(2026, 1, 20)
                5 -> LocalDate.of(2026, 3, 17) to LocalDate.of(2026, 4, 15)
                else -> LocalDate.of(2026, 3, 18) to LocalDate.of(2026, 4, 20)
            }
            val reserva = Reserva(libro, prestatario, fDesde, fHasta).apply { id = 600 + i }
            juan.agregarReserva(reserva)
            prestatario.agregarReserva(reserva)
            reservaRepository.create(reserva)
        }
    }

    fun crearLibrosAdicionales() {
        val data = LibroData(
            duenio = ana,
            titulo = "El Señor de los Anillos",
            autor = "J.R.R. Tolkien",
            autorImg = "https://upload.wikimedia.org/wikipedia/commons/d/d4/J._R._R._Tolkien%2C_1940s.jpg",
            descripcion = "Fantasía",
            genero = Genero.CIENCIA_FICCION,
            cantidadPaginas = 1200,
            isbn = "ISBN-3005",
            idioma = Idioma.ESPANOL,
            editorial = "Minotauro",
            fechaPublicacion = LocalDate.now(),
            estado = EstadoLibro.EXCELENTE,
            imagenUrl = "https://m.media-amazon.com/images/I/91R196S8S8L.jpg",
            fechaAgregado = LocalDate.of(2025, 2, 20),
        )
        val libro5 = LibroColeccionable(data)
        libroRepository.create(libro5)
    }
}