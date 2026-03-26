package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Calificacion
import ar.edu.unsam.phm.domain.Credencial
import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ReservaServiceSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Dado un ReservaService") {
        val libroRepository = mockk<LibroRepository>()
        val reservaRepository = mockk<ReservaRepository>()
        val usuarioRepository = mockk<UsuarioRepository>()
        val reservaService = ReservaService(libroRepository, usuarioRepository, reservaRepository)

        val duenio = Usuario(
            credencial = Credencial("ana", "pass"),
            nombre = "Ana García",
            desc = "Publicadora",
            bibliokarmas = 1200,
        ).apply { id = 3 }

        val solicitante = Usuario(
            credencial = Credencial("juan", "1234"),
            nombre = "Juan Pérez",
            desc = "Lector",
            bibliokarmas = 500,
        ).apply { id = 1 }

        val libro = LibroComun(
            LibroData(
                duenio = duenio,
                titulo = "1984", autor = "George Orwell",
                descripcion = "Distopía", genero = Genero.LITERATURA_CLASICA,
                cantidadPaginas = 300, isbn = "9780000000001", idioma = Idioma.ESPANOL,
                editorial = "Debolsillo", fechaPublicacion = LocalDate.of(1949, 6, 8),
                estado = EstadoLibro.EXCELENTE, imagenUrl = "",
            )
        ).apply { id = 10 }

        val reserva = Reserva(
            libro = libro,
            usuario = solicitante,
            fechaDesde = LocalDate.of(2026, 3, 1),
            fechaHasta = LocalDate.of(2026, 3, 10),
        ).apply { id = 500 }

        val usuarioSinReservas = Usuario(
            credencial = Credencial("vacio", "123"),
            nombre = "Sin Reservas",
            desc = ""
        ).apply { id = 99 }

        every { usuarioRepository.findById(1) } returns solicitante
        every { usuarioRepository.findById(3) } returns duenio
        every { usuarioRepository.findById(99) } returns usuarioSinReservas

        it("OBTENER RESERVAS DEVUELVE LOS DTOs DEL USUARIO SOLICITANTE") {
            every { reservaRepository.getReservasDeUsuario(1) } returns listOf(reserva)

            val resultado = reservaService.obtenerReservas(1)

            resultado shouldHaveSize 1
            resultado[0].titulo shouldBe "1984"
            resultado[0].autor shouldBe "George Orwell"
            resultado[0].alquiladoPorId shouldBe 1
            resultado[0].propietarioId shouldBe 3
            resultado[0].fechaDesde shouldBe "2026-03-01"
            resultado[0].fechaHasta shouldBe "2026-03-10"

            verify { reservaRepository.getReservasDeUsuario(1) }
        }

        it("OBTENER RESERVAS DEVUELVE LOS DTOs CUANDO EL USUARIO ES DUENIO") {
            every { reservaRepository.getReservasDeUsuario(3) } returns listOf(reserva)

            val resultado = reservaService.obtenerReservas(3)

            resultado shouldHaveSize 1
            resultado[0].propietarioId shouldBe 3

            verify { reservaRepository.getReservasDeUsuario(3) }
        }

        it("OBTENER RESERVAS DEVUELVE LISTA VACIA SI NO HAY RESERVAS") {
            every { reservaRepository.getReservasDeUsuario(99) } returns emptyList()

            val resultado = reservaService.obtenerReservas(99)

            resultado.shouldBeEmpty()
            verify { reservaRepository.getReservasDeUsuario(99) }
        }

        it("OBTENER RESERVAS MAPEA EL BIBLIOKARMA CORRECTAMENTE") {
            every { reservaRepository.getReservasDeUsuario(1) } returns listOf(reserva)

            val resultado = reservaService.obtenerReservas(1)

            // (5 * 10 días) + plusBiblioKarma(500 karma → multiplicador 5 → 300*5=1500) = 1550
            resultado[0].bibliokarma shouldBe 1550
        }

        it("OBTENER RESERVAS MAPEA EL RATING CORRECTAMENTE CUANDO NO HAY CALIFICACIONES") {
            every { reservaRepository.getReservasDeUsuario(1) } returns listOf(reserva)

            val resultado = reservaService.obtenerReservas(1)

            resultado[0].rating shouldBe 0.0
        }

        it("OBTENER RESERVAS MAPEA EL RATING CUANDO HAY CALIFICACIONES") {
            libro.agregarCalificacion(Calificacion(usuarioId = 2, valor = 4))
            every { reservaRepository.getReservasDeUsuario(1) } returns listOf(reserva)

            val resultado = reservaService.obtenerReservas(1)

            resultado[0].rating shouldBe 4.0
        }

        it("OBTENER RESERVAS CON MULTIPLES RESERVAS DEVUELVE TODOS LOS DTOs") {
            val libro2 = LibroComun(
                LibroData(
                    duenio = duenio,
                    titulo = "Dune", autor = "Frank Herbert",
                    descripcion = "Sci-fi", genero = Genero.CIENCIA_FICCION,
                    cantidadPaginas = 500, isbn = "9780000000002", idioma = Idioma.ESPANOL,
                    editorial = "Minotauro", fechaPublicacion = LocalDate.of(1965, 8, 1),
                    estado = EstadoLibro.MUY_BUENO, imagenUrl = "",
                )
            ).apply { id = 11 }

            val reserva2 = Reserva(
                libro = libro2,
                usuario = solicitante,
                fechaDesde = LocalDate.of(2026, 4, 1),
                fechaHasta = LocalDate.of(2026, 4, 15),
            ).apply { id = 501 }

            every { reservaRepository.getReservasDeUsuario(1) } returns listOf(reserva, reserva2)

            val resultado = reservaService.obtenerReservas(1)

            resultado shouldHaveSize 2
            resultado[0].titulo shouldBe "1984"
            resultado[1].titulo shouldBe "Dune"
        }
    }
})