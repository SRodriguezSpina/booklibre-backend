package ar.edu.unsam.phm.domain

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows
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
                descripcion = "", genero = Genero.DRAMA, cantidadPaginas = 300,
                isbn = "123", idioma = Idioma.ESPANOL, editorial = "",
                fechaPublicacion = LocalDate.now(), estado = EstadoLibro.BUENO, imagenUrl = ""
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

        it("confirmar AGREGA LA RESERVA A LAS LISTAS Y ASIGNA KARMA") {
            reserva.confirmar()

            solicitante.todasLasReservas() shouldContain reserva
            duenio.todasLasReservas() shouldContain reserva
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

        it("confirmar NO AGREGA LA RESERVA AL DUENIO SI ES EL MISMO USUARIO") {
            val reservaPropia = Reserva(
                libro = libro, usuario = duenio,
                fechaDesde = LocalDate.of(2026, 3, 10),
                fechaHasta = LocalDate.of(2026, 3, 20)
            ).apply { id = 200 }

            reservaPropia.confirmar()

            // Aparece una sola vez (como solicitante = dueño)
            duenio.todasLasReservas().count { it.id == 200 } shouldBe 1
        }

        it("agregarReserva LANZA BusinessException SI SE INTENTA AGREGAR LA MISMA RESERVA DOS VECES") {
            solicitante.agregarReserva(reserva)
            assertThrows<ar.edu.unsam.phm.exceptions.BusinessException> {
                solicitante.agregarReserva(reserva)
            }
        }
    }
})