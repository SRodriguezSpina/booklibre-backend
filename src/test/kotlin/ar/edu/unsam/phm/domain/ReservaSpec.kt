package ar.edu.unsam.phm.domain

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class ReservaSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Compruebo Reserva") {
        // Asignamos IDs distintos para que la lógica de Reserva sepa que son personas diferentes
        val duenio = Usuario(Credencial("ana", "pass"), "Ana", "").apply { id = 1 }
        val solicitante = Usuario(Credencial("juan", "pass"), "Juan", "").apply { id = 2 }

        val libro = LibroComun(
            LibroData(
                duenio = duenio, titulo = "1984", autor = "Orwell",
                descripcion = "Descripción de prueba", genero = Genero.DRAMA, cantidadPaginas = 300,
                isbn = "9780000000006", idioma = Idioma.ESPANOL, editorial = "Editorial de prueba",
                fechaPublicacion = LocalDate.now(), estado = EstadoLibro.BUENO,
                imagenUrl = "https://example.com/cover.jpg",
                disponibilidad = Disponibilidad.DISPONIBLE
            )
        ).apply { id = 10 }

        val reserva = Reserva(
            libro = libro,
            usuario = solicitante,
            fechaDesde = LocalDate.of(2026, 3, 10),
            fechaHasta = LocalDate.of(2026, 3, 20)
        ).apply { id = 100 }

        it("sePisaCon DEVUELVE TRUE SI LAS FECHAS SE SUPERPONEN") {
            reserva.sePisaCon(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 15)) shouldBe true
            reserva.sePisaCon(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 25)) shouldBe true
            reserva.sePisaCon(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 18)) shouldBe true
        }

        it("sePisaCon DEVUELVE FALSE SI LAS FECHAS NO SE SUPERPONEN") {
            reserva.sePisaCon(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 9)) shouldBe false
            reserva.sePisaCon(LocalDate.of(2026, 3, 21), LocalDate.of(2026, 3, 30)) shouldBe false
        }

        it("confirmar ASIGNA EL KARMA AL USUARIO SOLICITANTE") {
            reserva.confirmar()

            solicitante.bibliokarmas shouldBe 1555
        }
        it("sePisaCon DEVUELVE TRUE CUANDO LAS FECHAS SE TOCAN EXACTAMENTE EN EL BORDE INFERIOR") {
            // reserva: 10 → 20; nueva termina el día 10 → se pisa
            reserva.sePisaCon(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 10)
            ) shouldBe true
        }

        it("sePisaCon DEVUELVE TRUE CUANDO LAS FECHAS SE TOCAN EN EL BORDE SUPERIOR") {
            // reserva: 10 → 20; nueva empieza el día 20 → se pisa
            reserva.sePisaCon(
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 3, 30)
            ) shouldBe true
        }

        it("sePisaCon DEVUELVE FALSE CON UN DIA DE DIFERENCIA ANTES") {
            reserva.sePisaCon(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 9)
            ) shouldBe false
        }

        it("confirmar FUNCIONA CUANDO EL DUENIO ES EL MISMO USUARIO") {
            val reservaPropia = Reserva(
                libro = libro, usuario = duenio,
                fechaDesde = LocalDate.of(2026, 3, 10),
                fechaHasta = LocalDate.of(2026, 3, 20)
            ).apply { id = 200 }

            reservaPropia.confirmar()

            duenio.bibliokarmas shouldBe 1555
        }
    }
})