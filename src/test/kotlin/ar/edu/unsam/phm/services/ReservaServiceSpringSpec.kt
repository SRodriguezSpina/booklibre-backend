package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.EstadoLibro
import ar.edu.unsam.phm.domain.Genero
import ar.edu.unsam.phm.domain.Idioma
import ar.edu.unsam.phm.domain.LibroComun
import ar.edu.unsam.phm.domain.LibroData
import ar.edu.unsam.phm.domain.Reserva
import ar.edu.unsam.phm.domain.TipoReserva
import ar.edu.unsam.phm.domain.Usuario
import ar.edu.unsam.phm.dtos.CrearReservaDTO
import ar.edu.unsam.phm.exceptions.BusinessException
import ar.edu.unsam.phm.exceptions.NotFoundException
import ar.edu.unsam.phm.repository.LibroRepository
import ar.edu.unsam.phm.repository.ReservaJpaRepository
import ar.edu.unsam.phm.repository.UsuarioJpaRepository
import ar.edu.unsam.phm.repository.mongo.CalificacionMongoRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class ReservaServiceSpringSpec : DescribeSpec({

    val libroRepository = mockk<LibroRepository>()
    val usuarioJpa = mockk<UsuarioJpaRepository>()
    val reservaJpa = mockk<ReservaJpaRepository>()
    val calificacionesMongo = mockk<CalificacionMongoRepository>()
    val service = ReservaService(libroRepository, usuarioJpa, reservaJpa, calificacionesMongo)

    val duenio = Usuario(
        email = "ana@example.com",
        password = "123",
        nombre = "Ana García",
        desc = "Publicadora",
        celular = "+5412345678",
        ciudad = "CABA",
        bibliokarmas = 1200,
    ).apply { id = 3 }

    fun libroBase() = LibroComun(LibroData(
        duenio = duenio,
        titulo = "1984",
        autor = "George Orwell",
        descripcion = "Distopía",
        genero = Genero.LITERATURA_CLASICA,
        cantidadPaginas = 300,
        isbn = "9780000000001",
        idioma = Idioma.ESPANOL,
        editorial = "Debolsillo",
        fechaPublicacion = LocalDate.of(1949, 6, 8),
        estado = EstadoLibro.EXCELENTE,
        imagenUrl = "",
        fechaAgregado = LocalDate.now(),
    )).apply { id = 10 }

    fun lector(id: Int = 1) = Usuario(
        email = "juan@example.com",
        password = "123",
        nombre = "Juan Pérez",
        desc = "Lector",
        celular = "+54987654321",
        ciudad = "San Martín",
        bibliokarmas = 500,
    ).apply { this.id = id }

    describe("ReservaService con repositorios mockeados") {

        it("crearReserva lanza BusinessException si el libro ya está reservado en ese período") {
            val libro = libroBase()
            val user = lector()
            val desde = LocalDateTime.of(2026, 5, 1, 0, 0)
            val hasta = LocalDateTime.of(2026, 5, 10, 23, 59)
            val dto = CrearReservaDTO(10, 1, desde, hasta)
            every { libroRepository.getById(10) } returns libro
            every { usuarioJpa.findById(1) } returns Optional.of(user)
            every { reservaJpa.estaReservadoEnPeriodo(10, desde, hasta) } returns true

            shouldThrow<BusinessException> { service.crearReserva(dto) }

            verify(exactly = 0) { reservaJpa.save(any()) }
        }

        it("crearReserva lanza NotFoundException si el libro no existe") {
            val desde = LocalDateTime.of(2026, 5, 1, 0, 0)
            val hasta = LocalDateTime.of(2026, 5, 10, 23, 59)
            val dto = CrearReservaDTO(999, 1, desde, hasta)
            every { libroRepository.getById(999) } throws NotFoundException("Libro.kt 999 no encontrado")

            shouldThrow<NotFoundException> { service.crearReserva(dto) }
        }

        it("crearReserva lanza NotFoundException si el usuario no existe") {
            val libro = libroBase()
            val desde = LocalDateTime.of(2026, 5, 1, 0, 0)
            val hasta = LocalDateTime.of(2026, 5, 10, 23, 59)
            val dto = CrearReservaDTO(10, 999, desde, hasta)
            every { libroRepository.getById(10) } returns libro
            every { usuarioJpa.findById(999) } returns Optional.empty()

            shouldThrow<NotFoundException> { service.crearReserva(dto) }
        }

        it("crearReserva persiste usuario y reserva cuando todo es válido") {
            val libro = libroBase()
            val user = lector()
            val desde = LocalDateTime.of(2026, 5, 1, 0, 0)
            val hasta = LocalDateTime.of(2026, 5, 10, 23, 59)
            val dto = CrearReservaDTO(10, 1, desde, hasta)
            every { libroRepository.getById(10) } returns libro
            every { usuarioJpa.findById(1) } returns Optional.of(user)
            every { reservaJpa.estaReservadoEnPeriodo(10, desde, hasta) } returns false
            every { usuarioJpa.save(any()) } answers { firstArg() }
            every { reservaJpa.save(any()) } answers { firstArg<Reserva>().apply { id = 500 } }

            val reserva = service.crearReserva(dto)

            reserva.libro.id shouldBe 10
            reserva.usuario.id shouldBe 1
            verify { usuarioJpa.save(user) }
            verify { reservaJpa.save(any()) }
        }

        it("obtenerReservas lanza NotFoundException si el usuario no existe") {
            every { usuarioJpa.findById(99) } returns Optional.empty()
            shouldThrow<NotFoundException> { service.obtenerReservas(99, TipoReserva.HECHAS) }
        }
    }
})
