package ar.edu.unsam.phm.domain

import ar.edu.unsam.phm.exceptions.BusinessException
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class LibroSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Compruebo Libro") {

        val duenio = Usuario(
            credencial = Credencial("ana", "pass"),
            nombre = "Ana García",
            desc = "Publicadora",
            bibliokarmas = 500,
        )

        val usuarioPobre   = Usuario(Credencial("p", "p"), "Pobre",   "sin karma", bibliokarmas = 0)
        val usuarioRico    = Usuario(Credencial("r", "r"), "Rico",    "con karma", bibliokarmas = 2000)

        val desde = LocalDate.of(2026, 3, 1)
        val hasta = LocalDate.of(2026, 3, 10) // 10 días

        // ── LibroComun ────────────────────────────────────────────────────────
        describe("LibroComun") {

            val libro = LibroComun(
                duenio = duenio, id = 1,
                titulo = "1984", autor = "George Orwell",
                descripcion = "Distopía", genero = Genero.LITERATURA_CLASICA,
                paginas = 300, isbn = "ISBN-001", idioma = Idioma.ESPANOL,
                editorial = "Debolsillo", fechaPublicacion = LocalDate.of(1949, 6, 8),
                estado = EstadoLibro.EXCELENTE, imagenUrl = "",
            )

            it("PLUS BIBLIOKARMA CON USUARIO CON MENOS DE 1000 KARMA USA MULTIPLICADOR 5") {
                // 300 páginas * 5 = 1500
                libro.plusBiblioKarma(usuarioPobre) shouldBe 1500
            }

            it("PLUS BIBLIOKARMA CON USUARIO CON MAS DE 1000 KARMA USA MULTIPLICADOR 2") {
                // 300 páginas * 2 = 600
                libro.plusBiblioKarma(usuarioRico) shouldBe 600
            }

            it("CALCULA BIBLIOKARMA TOTAL CORRECTAMENTE") {
                // (5 * 10 días) + plusBiblioKarma(usuarioPobre=1500) = 50 + 1500 = 1550
                libro.calcularBiblioKarmaTotal(usuarioPobre, desde, hasta) shouldBe 1550
            }
        }

        // ── LibroConDedicatoria ───────────────────────────────────────────────
        describe("LibroConDedicatoria") {

            val libro = LibroConDedicatoria(
                duenio = duenio, id = 2,
                titulo = "El Principito", autor = "Saint-Exupéry",
                descripcion = "Clásico", genero = Genero.LITERATURA_CLASICA,
                paginas = 100, isbn = "ISBN-002", idioma = Idioma.ESPANOL,
                editorial = "Salamandra", fechaPublicacion = LocalDate.of(1943, 4, 6),
                estado = EstadoLibro.MUY_BUENO, imagenUrl = "",
            )

            it("PLUS BIBLIOKARMA SIN RESERVAS DEL DUENIO ES 200") {
                // 200 + (10 * 0 reservas) = 200
                libro.plusBiblioKarma(usuarioPobre) shouldBe 200
            }

            it("PLUS BIBLIOKARMA AUMENTA SEGUN RESERVAS DEL DUENIO") {
                val reservaFake = Reserva(libro, duenio, desde, hasta)
                duenio.agregarReserva(reservaFake)
                // 200 + (10 * 2) = 220
                libro.plusBiblioKarma(usuarioPobre) shouldBe 210
            }

            it("CALCULA BIBLIOKARMA TOTAL CON DUENIO SIN RESERVAS") {
                // (5 * 10) + 200 = 250
                libro.calcularBiblioKarmaTotal(usuarioPobre, desde, hasta) shouldBe 250
            }
        }

        // ── LibroColeccionable ────────────────────────────────────────────────
        describe("LibroColeccionable") {

            val libro = LibroColeccionable(
                duenio = duenio, id = 3,
                titulo = "Dune", autor = "Frank Herbert",
                descripcion = "Sci-fi épico", genero = Genero.CIENCIA_FICCION,
                paginas = 500, isbn = "ISBN-003", idioma = Idioma.ESPANOL,
                editorial = "Minotauro", fechaPublicacion = LocalDate.of(1965, 8, 1),
                estado = EstadoLibro.EXCELENTE, imagenUrl = "",
            )

            it("PLUS BIBLIOKARMA ES CEIL(KARMA/5) + PAGINAS") {
                // ceil(2000/5) + 500 = 400 + 500 = 900
                libro.plusBiblioKarma(usuarioRico) shouldBe 900
            }

            it("PLUS BIBLIOKARMA CON KARMA 0 ES SOLO LAS PAGINAS") {
                // ceil(0/5) + 500 = 0 + 500 = 500
                libro.plusBiblioKarma(usuarioPobre) shouldBe 500
            }

            it("CALCULA BIBLIOKARMA TOTAL CORRECTAMENTE") {
                // (5 * 10) + 900 = 950
                libro.calcularBiblioKarmaTotal(usuarioRico, desde, hasta) shouldBe 950
            }
        }

        // ── Calificaciones ────────────────────────────────────────────────────
        describe("Calificaciones") {

            val libro = LibroComun(
                duenio = duenio, id = 4,
                titulo = "Sapiens", autor = "Yuval Harari",
                descripcion = "Historia", genero = Genero.LITERATURA_CLASICA,
                paginas = 400, isbn = "ISBN-004", idioma = Idioma.ESPANOL,
                editorial = "Debate", fechaPublicacion = LocalDate.of(2011, 1, 1),
                estado = EstadoLibro.EXCELENTE, imagenUrl = "",
            )

            it("CALIFICACION INICIAL ES 0.0 CUANDO NO HAY CALIFICACIONES") {
                libro.getCalificacion() shouldBe 0.0
            }

            it("AGREGA UNA CALIFICACION Y CALCULA EL PROMEDIO CORRECTAMENTE") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 4))
                libro.getCalificacion() shouldBe 4.0
            }

            it("PROMEDIO CON MULTIPLES CALIFICACIONES ES CORRECTO") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 4))
                libro.agregarCalificacion(Calificacion(usuarioId = 2, valor = 2))
                libro.getCalificacion() shouldBe 3.0
            }

            it("NO SE PUEDE CALIFICAR DOS VECES CON EL MISMO USUARIO") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 5))
                assertThrows<BusinessException> {
                    libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 3))
                }
            }

            it("LA CALIFICACION NO CAMBIA TRAS UN INTENTO DUPLICADO") {
                libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 5))
                assertThrows<BusinessException> {
                    libro.agregarCalificacion(Calificacion(usuarioId = 1, valor = 1))
                }
                libro.getCalificacion() shouldBe 5.0
            }
        }
    }
})