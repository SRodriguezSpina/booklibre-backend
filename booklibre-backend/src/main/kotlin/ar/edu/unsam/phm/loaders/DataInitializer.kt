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
        // 1. Usuarios con sus credenciales [cite: 37, 87]
        val emilia = Usuario( Credencial("emilia", "123"), "Emilia Romero", "Lectora Ávida", "emilia@example.com", "11223344", "CABA", UsuarioLectorPublicador(), 2345)
        val juan = Usuario(Credencial("juan", "123"), "Juan Pérez", "Lector intrépido", "juan@example.com", "11556677", "Morón", UsuarioLector(), 500)
        val ana = Usuario( Credencial("ana", "123"), "Ana García", "Publicadora", "ana@example.com", "11889900", "Quilmes", UsuarioPublicador(), 1200)
        val carlos = Usuario( Credencial("carlos", "123"), "Carlos Ruiz", "Fan de los clásicos", "carlos@example.com", "11334455", "Lanús", UsuarioLector(), 150)

        listOf(emilia, juan, ana, carlos).forEach { usuarioRepo.create(it) } // [cite: 81]

        // 2. CATEGORÍA: "PRESTADOS A MÍ" (Emilia reserva libros de otros)
        repeat(5) { i ->
            // Ana es la dueña, Emilia reserva
            val libro = LibroComun(ana, 100 + i, "Crónicas $i", "Autor A", "Desc", Genero.CIENCIA_FICCION, 300, "97801$i", Idioma.ESPANOL, "Minotauro", LocalDate.now(), EstadoLibro.EXCELENTE, "")
            libroRepo.create(libro)

            val res = Reserva(i, libro, emilia, LocalDate.now().plusDays(i.toLong()), LocalDate.now().plusDays(i.toLong() + 3))
            libro.agregarReserva(res) // [cite: 32, 33]
            emilia.bibliokarmas += libro.calcularBiblioKarmaTotal(emilia, res.fechaDesde, res.fechaHasta) // [cite: 25, 27]
        }

        // 3. CATEGORÍA: "PRESTADOS POR MÍ" (Otros reservan libros de Emilia) [cite: 42, 63]
        repeat(5) { i ->
            // Emilia es la dueña, Juan reserva
            val libro = LibroConDedicatoria(emilia, 200 + i, "Poesía $i", "Autor B", "Desc", Genero.LITERATURA_CLASICA, 150, "97802$i", Idioma.FRANCES, "Sur", LocalDate.now(), EstadoLibro.MUY_BUENO, "")
            libroRepo.create(libro)

            val res = Reserva(50 + i, libro, juan, LocalDate.now().plusDays(i.toLong()), LocalDate.now().plusDays(i.toLong() + 5))
            libro.agregarReserva(res)
        }

        // 4. Libros Coleccionables adicionales (sin reservas actuales) [cite: 15, 86]
        repeat(8) { i ->
            val libro = LibroColeccionable(carlos, 300 + i, "Enciclopedia $i", "Autor C", "Desc", Genero.DISENO, 800, "97803$i", Idioma.INGLES, "Taschen", LocalDate.now(), EstadoLibro.EXCELENTE, "")
            libroRepo.create(libro)
        }

        // 5. Casos de borde: Próximo a vencer para Emilia (vence mañana)
        val libroEspecial = LibroColeccionable(ana, 999, "Libro Raro", "Autor X", "Desc", Genero.CIENCIA_FICCION, 400, "978999", Idioma.ESPANOL, "Editor", LocalDate.now(), EstadoLibro.EXCELENTE, "")
        libroRepo.create(libroEspecial)
        val resVencimiento = Reserva(99, libroEspecial, emilia, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1))
        libroEspecial.agregarReserva(resVencimiento)
    }
}