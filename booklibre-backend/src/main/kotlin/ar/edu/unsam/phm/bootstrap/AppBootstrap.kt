package ar.edu.unsam.phm.bootstrap

import ar.edu.unsam.phm.*
import ar.edu.unsam.phm.repository.LibroRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AppBootstrap(
    val libroRepository: LibroRepository,
): InitializingBean {
   //USUARIOS
    val user1 = Usuario(
        credencial = Credencial("juanp", "pass123"),
        nombre = "Juan Pérez",
        desc = "Lector intrépido",
        email = "juan@mail.com",
        celular = "11223344",
        ciudad = "San Martín",
        tipoUsuario = UsuarioLector(),
        bibliokarmas = 100 // Puntos iniciales de prueba
    )
    val user2 = Usuario(
        credencial = Credencial("mariag", "pass456"),
        nombre = "María Gómez",
        desc = "Devoradora de libros",
        email = "maria@mail.com",
        celular = "11556677",
        ciudad = "Villa Ballester",
        tipoUsuario = UsuarioPublicador(),
        bibliokarmas = 500
    )
    val user3 = Usuario(
        credencial = Credencial("carlosl", "pass789"),
        nombre = "Carlos López",
        desc = "Fan de la ciencia ficción",
        email = "carlos@mail.com",
        celular = "11889900",
        ciudad = "Caseros",
        tipoUsuario = UsuarioLectorPublicador(),
        bibliokarmas = 1200 // Este tiene más de 1000, ideal para probar la regla del Libro Común
    )
    val user4 = Usuario(
        credencial = Credencial("anad", "pass000"),
        nombre = "Ana Díaz",
        desc = "Amante del drama",
        email = "ana@mail.com",
        celular = "11443322",
        ciudad = "San Andrés",
        tipoUsuario = UsuarioLector(),
        bibliokarmas = 50
    )

    //LIBROS
    val libro1 = LibroComun(user1, 0, "1984", "George Orwell", "Distopía clásica", Genero.CIENCIA_FICCION, 328, "978-0451524935", Idioma.ESPANOL, "Debolsillo", LocalDate.of(1949, 6, 8), EstadoLibro.EXCELENTE)
    val libro2 = LibroComun(user3, 0, "Clean Code", "Robert C. Martin", "Buenas prácticas de programación", Genero.DISENO, 464, "978-0132350884", Idioma.INGLES, "Prentice Hall", LocalDate.of(2008, 8, 1), EstadoLibro.MUY_BUENO)

    val libro3 = LibroConDedicatoria(user2, 0, "Orgullo y Prejuicio", "Jane Austen", "Clásico del romance", Genero.ROMANCE, 432, "978-8420671848", Idioma.ESPANOL, "Alianza", LocalDate.of(1813, 1, 28), EstadoLibro.BUENO)
    val libro4 = LibroConDedicatoria(user3, 0, "Hábitos Atómicos", "James Clear", "Mejora personal", Genero.AUTOAYUDA, 320, "978-1847941831", Idioma.INGLES, "Penguin", LocalDate.of(2018, 10, 16), EstadoLibro.EXCELENTE)

    val libro5 = LibroColeccionable(user2, 0, "El Señor de los Anillos (Edición Ilustrada)", "J.R.R. Tolkien", "Edición tapa dura de colección", Genero.CIENCIA_FICCION, 1200, "978-8445071754", Idioma.ESPANOL, "Minotauro", LocalDate.of(1954, 7, 29), EstadoLibro.EXCELENTE)
    val libro6 = LibroColeccionable(user4, 0, "Hamlet (Primera Edición Facsímil)", "William Shakespeare", "Copia exacta del original", Genero.DRAMA, 200, "978-0743477123", Idioma.INGLES, "Simon & Schuster", LocalDate.of(1603, 1, 1), EstadoLibro.REGULAR)


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
        crearLibros()
    }
}