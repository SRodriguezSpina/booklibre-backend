package ar.edu.unsam.phm.domain

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import ar.edu.unsam.phm.exceptions.BusinessException
import java.time.LocalDate
import java.time.LocalDateTime

class ReservaSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Compruebo Reserva") {
        // Asignamos IDs distintos para que la lógica de Reserva sepa que son personas diferentes
        val duenio = Usuario("ana", "pass", "Ana", "").apply { id = 1 }
        val solicitante = Usuario("juan", "pass", "Juan", "").apply { id = 2 }
        val duenioRef = DuenioRef(id = duenio.id, nombre = duenio.nombre)

        val libro = LibroComun(
            LibroData(
                duenio = duenioRef, titulo = "1984", autor = "Orwell",
                descripcion = "Novela distópica", genero = Genero.DRAMA, cantidadPaginas = 300,
                isbn = "9780000000006", idioma = Idioma.ESPANOL, editorial = "Debolsillo",
                fechaPublicacion = LocalDate.now(), estado = EstadoLibro.BUENO, imagenUrl = "",
            )
        ).apply { id = 10 }

        val reserva = Reserva(
            libroId = libro.id,
            usuario = solicitante,
            fechaDesde = LocalDate.of(2026, 3, 10).atStartOfDay(),
            fechaHasta = LocalDate.of(2026, 3, 20).atTime(23, 59, 59)
        ).apply { id = 100 }

        it("sePisaCon DEVUELVE TRUE SI LAS FECHAS SE SUPERPONEN") {
            reserva.sePisaCon(LocalDate.of(2026, 3, 5).atStartOfDay(), LocalDate.of(2026, 3, 15).atTime(23, 59, 59)) shouldBe true
            reserva.sePisaCon(LocalDate.of(2026, 3, 15).atStartOfDay(), LocalDate.of(2026, 3, 25).atTime(23, 59, 59)) shouldBe true
            reserva.sePisaCon(LocalDate.of(2026, 3, 12).atStartOfDay(), LocalDate.of(2026, 3, 18).atTime(23, 59, 59)) shouldBe true
        }

        it("sePisaCon DEVUELVE FALSE SI LAS FECHAS NO SE SUPERPONEN") {
            reserva.sePisaCon(LocalDate.of(2026, 3, 1).atStartOfDay(), LocalDate.of(2026, 3, 9).atTime(23, 59, 59)) shouldBe false
            reserva.sePisaCon(LocalDate.of(2026, 3, 21).atStartOfDay(), LocalDate.of(2026, 3, 30).atTime(23, 59, 59)) shouldBe false
        }

        it("confirmar ASIGNA EL KARMA AL USUARIO SOLICITANTE") {
            reserva.confirmar(libro)

            solicitante.bibliokarmas shouldBe 1555
        }
        it("sePisaCon DEVUELVE TRUE CUANDO LAS FECHAS SE TOCAN EXACTAMENTE EN EL BORDE INFERIOR") {
            // reserva: 10 → 20; nueva termina el día 10 → se pisa
            reserva.sePisaCon(
                LocalDate.of(2026, 3, 1).atStartOfDay(),
                LocalDate.of(2026, 3, 10).atStartOfDay()
            ) shouldBe true
        }

        it("sePisaCon DEVUELVE TRUE CUANDO LAS FECHAS SE TOCAN EN EL BORDE SUPERIOR") {
            // reserva: 10 → 20; nueva empieza el día 20 → se pisa
            reserva.sePisaCon(
                LocalDate.of(2026, 3, 20).atStartOfDay(),
                LocalDate.of(2026, 3, 30).atTime(23, 59, 59)
            ) shouldBe true
        }

        it("sePisaCon DEVUELVE FALSE CON UN DIA DE DIFERENCIA ANTES") {
            reserva.sePisaCon(
                LocalDate.of(2026, 3, 1).atStartOfDay(),
                LocalDate.of(2026, 3, 9).atTime(23, 59, 59)
            ) shouldBe false
        }

        it("confirmar NO FUNCIONA CUANDO EL DUENIO ES EL MISMO USUARIO") {
            val reservaPropia = Reserva(
                libroId = libro.id, usuario = duenio,
                fechaDesde = LocalDate.of(2026, 3, 10).atStartOfDay(),
                fechaHasta = LocalDate.of(2026, 3, 20).atTime(23, 59, 59)
            ).apply { id = 200 }

            shouldThrow<BusinessException> {
                reservaPropia.confirmar(libro)
            }
        }
    }
})