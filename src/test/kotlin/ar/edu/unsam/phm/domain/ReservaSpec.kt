package ar.edu.unsam.phm.domain

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class ReservaSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Compruebo Reserva") {
        val duenio = Usuario(Credencial("ana", "pass"), "Ana", "").apply { id = 1 }
        val solicitante = Usuario(Credencial("juan", "pass"), "Juan", "").apply { id = 2 }

        val libro = LibroComun(
            LibroData(
                duenio = duenio, titulo = "1984", autor = "Orwell", autorImg = "",
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
    }
})