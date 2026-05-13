package ar.edu.unsam.phm.services

import ar.edu.unsam.phm.domain.Disponibilidad
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
import ar.edu.unsam.phm.repository.ReservaRepository
import ar.edu.unsam.phm.repository.UsuarioRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ReservaServiceSpringSpec : DescribeSpec({

    val libroRepository = mockk<LibroRepository>()
    val usuarioRepository = mockk<UsuarioRepository>()
    val reservaRepository = mockk<ReservaRepository>()
    val service = ReservaService(libroRepository, usuarioRepository, reservaRepository)

    val duenio = Usuario(
        email = "ana@example.com",
        password = "123",
        nombre = "Ana García",
        desc = "Publicadora",
        celular = "+5412345678",
        ciudad = "CABA",
        bibliokarmas = 1200,
    ).apply { id = 3 }

    fun libroDisponible() = LibroComun(LibroData(
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
        disponibilidad = Disponibilidad.DISPONIBLE,
    )).apply { id = 10 }

    fun libroReservado() = LibroComun(LibroData(
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
        disponibilidad = Disponibilidad.RESERVADO,
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

        it("crearReserva lanza BusinessException si el libro no está disponible") {
            val libro = libroReservado()
            val user = lector()
            val dto = CrearReservaDTO(10, 1, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10))
            every { libroRepository.getById(10) } returns libro
            every { usuarioRepository.getById(1) } returns user

            shouldThrow<BusinessException> { service.crearReserva(dto) }

            verify(exactly = 0) { libroRepository.save(any()) }
            verify(exactly = 0) { reservaRepository.save(any()) }
        }

        it("crearReserva lanza NotFoundException si el libro no existe") {
            val dto = CrearReservaDTO(999, 1, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10))
            every { libroRepository.getById(999) } throws NotFoundException("Libro.kt 999 no encontrado")

            shouldThrow<NotFoundException> { service.crearReserva(dto) }
        }

        it("crearReserva lanza NotFoundException si el usuario no existe") {
            val libro = libroDisponible()
            val dto = CrearReservaDTO(10, 999, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10))
            every { libroRepository.getById(10) } returns libro
            every { usuarioRepository.getById(999) } throws NotFoundException("Usuario 999 no encontrado")

            shouldThrow<NotFoundException> { service.crearReserva(dto) }
        }

        it("crearReserva persiste libro, usuario y reserva cuando todo es válido") {
            val libro = libroDisponible()
            val user = lector()
            val dto = CrearReservaDTO(10, 1, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10))
            every { libroRepository.getById(10) } returns libro
            every { usuarioRepository.getById(1) } returns user
            every { libroRepository.save(any()) } answers { firstArg() }
            every { usuarioRepository.save(any()) } answers { firstArg() }
            every { reservaRepository.save(any()) } answers { firstArg<Reserva>().apply { id = 500 } }

            val reserva = service.crearReserva(dto)

            reserva.libro.id shouldBe 10
            reserva.usuario.id shouldBe 1
            libro.estaDisponible().shouldBeFalse()
            verify { libroRepository.save(libro) }
            verify { usuarioRepository.save(user) }
            verify { reservaRepository.save(any()) }
        }

        it("obtenerReservas lanza NotFoundException si el usuario no existe") {
            every { usuarioRepository.getById(99) } throws NotFoundException("Usuario 99 no encontrado")
            shouldThrow<NotFoundException> { service.obtenerReservas(99, TipoReserva.HECHAS) }
        }
    }
})
