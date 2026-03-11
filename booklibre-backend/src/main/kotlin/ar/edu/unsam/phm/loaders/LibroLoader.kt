package ar.edu.unsam.phm.loaders

import ar.edu.unsam.phm.*
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class DataInitializer(
    val usuarioRepo: UsuarioRepository,
    val libroRepo: LibroRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // 1. Crear Usuarios [cite: 87]
        val emilia = Usuario(1, Credencial("emilia", "123"), "Emilia Romero", "Lectora ávida", "emilia@example.com", "11223344", "CABA", UsuarioLectorPublicador(), 2345)
        val juan = Usuario(2, Credencial("juan", "123"), "Juan Pérez", "Fan de la ciencia ficción", "juan@example.com", "11556677", "Morón", UsuarioLector(), 500)
        val ana = Usuario(3, Credencial("ana", "123"), "Ana García", "Coleccionista", "ana@example.com", "11889900", "Quilmes", UsuarioPublicador(), 1200)
        val carlos = Usuario(4, Credencial("carlos", "123"), "Carlos Ruiz", "Lector intrépido", "carlos@example.com", "11334455", "Lanús", UsuarioLector(), 100)

        listOf(emilia, juan, ana, carlos).forEach { usuarioRepo.create(it) }

        // 2. Crear Libros (Ejemplos de los 8 requeridos por tipo) [cite: 86]
        val libro1 = LibroComun(emilia, 1, "El Aleph", "Borges", "Cuentos clásicos", Genero.LITERATURA_CLASICA, 200, "97801", Idioma.ESPANOL, "Debolsillo", LocalDate.of(1945, 9, 30), EstadoLibro.EXCELENTE)
        val libro2 = LibroConDedicatoria(juan, 2, "Rayuela", "Cortázar", "Edición firmada", Genero.LITERATURA_CLASICA, 600, "97802", Idioma.ESPANOL, "Sudamericana", LocalDate.of(1963, 6, 28), EstadoLibro.MUY_BUENO)
        val libro3 = LibroColeccionable(ana, 3, "Dune", "Frank Herbert", "Primera edición", Genero.CIENCIA_FICCION, 500, "97803", Idioma.INGLES, "Chilton", LocalDate.of(1965, 8, 1), EstadoLibro.EXCELENTE)
        val libro4 = LibroComun(carlos, 4, "Atomic Habits", "James Clear", "Productividad", Genero.AUTOAYUDA, 320, "97804", Idioma.INGLES, "Viking", LocalDate.of(2018, 10, 16), EstadoLibro.BUENO)

        listOf(libro1, libro2, libro3, libro4).forEach { libroRepo.create(it) }

        // 3. Crear Reservas (Al menos 2 por usuario)
        // Juan reserva el libro de Emilia
        val res1 = Reserva(1, libro1, juan, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))
        libro1.agregarReserva(res1)
        juan.bibliokarmas += libro1.calcularBiblioKarmaTotal(juan, res1.fechaDesde, res1.fechaHasta)

        // Ana reserva el libro de Carlos
        val res2 = Reserva(2, libro4, ana, LocalDate.now().plusDays(2), LocalDate.now().plusDays(10))
        libro4.agregarReserva(res2)
        ana.bibliokarmas += libro4.calcularBiblioKarmaTotal(ana, res2.fechaDesde, res2.fechaHasta)

        // Emilia reserva el libro de Juan (Próximo a vencer)
        val res3 = Reserva(3, libro2, emilia, LocalDate.now().minusDays(3), LocalDate.now().plusDays(1))
        libro2.agregarReserva(res3)
    }
}
