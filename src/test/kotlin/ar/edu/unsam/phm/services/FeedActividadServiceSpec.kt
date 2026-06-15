package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.*
import ar.edu.unsam.phm.dtos.LibroAgregadoDTO
import ar.edu.unsam.phm.dtos.ReservaConfirmadaDTO
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.mongo.LibroMongoRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

class FeedActividadServiceSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerTest

    describe("Dado un FeedActividadService") {
        val libroMongoRepository = mockk<LibroMongoRepository>()
        val reservaJpaRepository = mockk<ReservaJpaRepository>()
        val feedActividadService = FeedActividadService(libroMongoRepository, reservaJpaRepository)

        fun libroComun(id: Int, titulo: String, fechaAgregado: LocalDateTime): LibroComun {
            val data = LibroData(
                duenio = DuenioRef(id = 1, nombre = "Dueño Test"),
                titulo = titulo,
                autor = "Autor Test",
                descripcion = "Descripcion",
                genero = Genero.DRAMA,
                cantidadPaginas = 100,
                isbn = "1234567890123",
                idioma = Idioma.ESPANOL,
                editorial = "Editorial",
                fechaPublicacion = fechaAgregado.toLocalDate(),
                estado = EstadoLibro.BUENO,
                fechaAgregado = fechaAgregado,
            )
            return LibroComun(data).apply { this.id = id }
        }

        fun usuarioTest(id: Int, nombre: String): Usuario =
            Usuario(
                email = "$nombre@test.com",
                password = "pass",
                nombre = nombre,
                ciudad = "CABA",
            ).apply { this.id = id }

        fun reservaTest(id: Int, libroId: Int, usuario: Usuario, fechaDesde: LocalDateTime): Reserva =
            Reserva(
                libroId = libroId,
                usuario = usuario,
                fechaDesde = fechaDesde,
                fechaHasta = fechaDesde.plusDays(7),
            ).apply { this.id = id }

        it("retorna máximo 5 eventos ordenados por fecha descendente") {
            val ahora = LocalDateTime.now()
            val libro1 = libroComun(1, "Libro Reciente", ahora.minusHours(1))
            val libro2 = libroComun(2, "Libro Antiguo", ahora.minusDays(10))
            val usuario = usuarioTest(10, "Usuario Test")
            val reserva1 = reservaTest(100, 1, usuario, ahora.minusHours(3))

            every { libroMongoRepository.findTopNByFechaAgregadoDesc(5) } returns listOf(libro1, libro2)
            every { reservaJpaRepository.findTopNRecentesConUsuario(any<Pageable>()) } returns listOf(reserva1)
            every { libroMongoRepository.findAllByIds(any()) } returns listOf(libro1)

            val resultado = feedActividadService.obtenerFeed()

            resultado shouldHaveSize 3
            // El libro más reciente debe ser primero (ahora - 1h)
            resultado[0].shouldBeInstanceOf<LibroAgregadoDTO>()
            (resultado[0] as LibroAgregadoDTO).titulo shouldBe "Libro Reciente"
        }

        it("cuando no hay libros ni reservas devuelve lista vacía") {
            every { libroMongoRepository.findTopNByFechaAgregadoDesc(5) } returns emptyList()
            every { reservaJpaRepository.findTopNRecentesConUsuario(any<Pageable>()) } returns emptyList()
            every { libroMongoRepository.findAllByIds(any()) } returns emptyList()

            val resultado = feedActividadService.obtenerFeed()

            resultado shouldHaveSize 0
        }

        it("los eventos de tipo LibroAgregado tienen tipoEvento LIBRO_AGREGADO") {
            val ahora = LocalDateTime.now()
            val libro = libroComun(1, "Test", ahora)

            every { libroMongoRepository.findTopNByFechaAgregadoDesc(5) } returns listOf(libro)
            every { reservaJpaRepository.findTopNRecentesConUsuario(any<Pageable>()) } returns emptyList()
            every { libroMongoRepository.findAllByIds(any()) } returns emptyList()

            val resultado = feedActividadService.obtenerFeed()

            resultado shouldHaveSize 1
            (resultado[0] as LibroAgregadoDTO).tipoEvento shouldBe "LIBRO_AGREGADO"
        }

        it("los eventos de tipo ReservaConfirmada tienen tipoEvento RESERVA_CONFIRMADA") {
            val ahora = LocalDateTime.now()
            val usuario = usuarioTest(10, "Usuario Test")
            val reserva = reservaTest(100, 5, usuario, ahora)

            every { libroMongoRepository.findTopNByFechaAgregadoDesc(5) } returns emptyList()
            every { reservaJpaRepository.findTopNRecentesConUsuario(any<Pageable>()) } returns listOf(reserva)
            every { libroMongoRepository.findAllByIds(any()) } returns emptyList()

            val resultado = feedActividadService.obtenerFeed()

            resultado shouldHaveSize 1
            (resultado[0] as ReservaConfirmadaDTO).tipoEvento shouldBe "RESERVA_CONFIRMADA"
        }

        it("cuando hay más de 5 eventos, el resultado está limitado a 5") {
            val ahora = LocalDateTime.now()
            val libros = (1..5).map { libroComun(it, "Libro $it", ahora.minusHours(it.toLong())) }
            val usuario = usuarioTest(10, "Usuario Test")
            val reservas = (1..5).map { reservaTest(it * 100, it, usuario, ahora.minusMinutes(it.toLong())) }

            every { libroMongoRepository.findTopNByFechaAgregadoDesc(5) } returns libros
            every { reservaJpaRepository.findTopNRecentesConUsuario(any<Pageable>()) } returns reservas
            every { libroMongoRepository.findAllByIds(any()) } returns libros

            val resultado = feedActividadService.obtenerFeed()

            resultado shouldHaveSize 5
        }
    }
})
